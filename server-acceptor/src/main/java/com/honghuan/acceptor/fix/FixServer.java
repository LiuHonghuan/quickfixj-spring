package com.honghuan.acceptor.fix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import quickfix.*;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

import static quickfix.Acceptor.*;

/**
 * FIX 服务器主类
 * 负责初始化和管理 FIX 服务端连接
 */
@Service
@Slf4j
public class FixServer {

    /**
     * FIX 消息接收器实例
     */
    private static ThreadedSocketAcceptor acceptor = null;

    @Resource
    private FixServerApplication serverApplication;

    /**
     * 动态会话映射配置
     * key: 网络地址
     * value: 会话模板映射列表
     */
    private final Map<InetSocketAddress, List<DynamicAcceptorSessionProvider.TemplateMapping>> dynamicSessionMappings = new HashMap<>();

    /**
     * 启动 FIX 服务器
     * 在 Spring 容器初始化后自动执行
     */
    @PostConstruct
    public void start() {
        try (InputStream inputStream = new DefaultResourceLoader().getResource("classpath:server.cfg").getInputStream()) {
            SessionSettings settings = new SessionSettings(inputStream);
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            acceptor = new ThreadedSocketAcceptor(serverApplication, storeFactory, settings, logFactory, messageFactory);
            configureDynamicSessions(settings, serverApplication, storeFactory, logFactory, messageFactory);
            acceptor.start();
        } catch (Exception e) {
            log.error("start failed", e);
        }
    }

    /**
     * 配置动态会话
     * @param settings FIX 配置
     * @param application FIX 应用程序实例
     * @param messageStoreFactory 消息存储工厂
     * @param logFactory 日志工厂
     * @param messageFactory 消息工厂
     */
    private void configureDynamicSessions(SessionSettings settings, Application application,
            MessageStoreFactory messageStoreFactory, LogFactory logFactory,
            MessageFactory messageFactory) throws ConfigError, FieldConvertError {
        Iterator<SessionID> sectionIterator = settings.sectionIterator();
        while (sectionIterator.hasNext()) {
            SessionID sessionID = sectionIterator.next();
            if (isSessionTemplate(settings, sessionID)) {
                InetSocketAddress address = getAcceptorSocketAddress(settings, sessionID);
                getMappings(address).add(new DynamicAcceptorSessionProvider.TemplateMapping(sessionID, sessionID));
            }
        }

        for (Map.Entry<InetSocketAddress, List<DynamicAcceptorSessionProvider.TemplateMapping>> entry : dynamicSessionMappings.entrySet()) {
            acceptor.setSessionProvider(entry.getKey(), new DynamicAcceptorSessionProvider(
                    settings, entry.getValue(), application, messageStoreFactory, logFactory,
                    messageFactory));
        }
    }

    /**
     * 获取指定地址的会话映射列表
     * @param address 网络地址
     * @return 会话映射列表
     */
    private List<DynamicAcceptorSessionProvider.TemplateMapping> getMappings(InetSocketAddress address) {
        return dynamicSessionMappings.computeIfAbsent(address, k -> new ArrayList<>());
    }

    /**
     * 获取接收器的套接字地址
     * @param settings FIX 配置
     * @param sessionID 会话 ID
     * @return 套接字地址
     */
    private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        String acceptorHost = "0.0.0.0";
        if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
            acceptorHost = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
        }

        int acceptorPort = (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT);
        return new InetSocketAddress(acceptorHost, acceptorPort);
    }

    /**
     * 判断是否为会话模板
     * @param settings FIX 配置
     * @param sessionID 会话 ID
     * @return 是否为模板
     */
    private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE) && settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
    }

    /**
     * 获取所有活动的会话 ID
     * @return 会话 ID 列表
     */
    public List<SessionID> getSessionIDs() {
        if (null != acceptor) {
            return acceptor.getSessions();
        }
        return null;
    }
}
