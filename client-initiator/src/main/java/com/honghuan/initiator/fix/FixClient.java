package com.honghuan.initiator.fix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import quickfix.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.InputStream;

/**
 * FIX 客户端主类
 * 负责初始化和管理 FIX 客户端连接
 */
@Service
@Slf4j
public class FixClient {

    /**
     * FIX 消息发送器实例
     */
    private Initiator initiator = null;

    /**
     * 发送器启动状态标志
     */
    private boolean initiatorStarted = false;

    /**
     * FIX 客户端应用程序实例
     */
    @Resource
    private FixClientApplication clientApplication;

    /**
     * 启动 FIX 客户端
     * 在 Spring 容器初始化后自动执行
     */
    @PostConstruct
    public void start() {
        try (InputStream inputStream = new DefaultResourceLoader().getResource("classpath:client.cfg").getInputStream()) {
            SessionSettings settings = new SessionSettings(inputStream);
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            // 使用内存存储替代文件存储
            MessageStoreFactory storeFactory1 = new MemoryStoreFactory();
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();
            initiator = new SocketInitiator(clientApplication, storeFactory1, settings, logFactory, messageFactory);
            logon();
        } catch (Exception e) {
            log.error("start failed", e);
        }
    }

    /**
     * 执行 FIX 登录
     * 如果发送器未启动则启动发送器，否则对所有会话执行登录
     */
    public synchronized void logon() {
        if (!initiatorStarted) {
            try {
                initiator.start();
                initiatorStarted = true;
                log.warn("fix client started!");
            } catch (Exception e) {
                log.error("logon failed", e);
            }
        } else {
            for (SessionID sessionId : initiator.getSessions()) {
                Session.lookupSession(sessionId).logon();
            }
        }
    }
}
