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

@Service
@Slf4j
public class FixServer {

    private static ThreadedSocketAcceptor acceptor = null;


    @Resource
    private FixServerApplication serverApplication;

    /**
     * The Dynamic session mappings.
     */
    private final Map<InetSocketAddress, List<DynamicAcceptorSessionProvider.TemplateMapping>> dynamicSessionMappings = new HashMap<>();


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

    private void configureDynamicSessions(SessionSettings settings, Application application, MessageStoreFactory messageStoreFactory, LogFactory logFactory, MessageFactory messageFactory) throws ConfigError, FieldConvertError {
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
     * Gets mappings.
     *
     * @param address the address
     * @return the mappings
     */
    private List<DynamicAcceptorSessionProvider.TemplateMapping> getMappings(InetSocketAddress address) {
        return dynamicSessionMappings.computeIfAbsent(address, k -> new ArrayList<>());
    }

    /**
     * 允许接收的IP地址
     *
     * @param settings
     * @param sessionID
     * @return
     * @throws ConfigError
     * @throws FieldConvertError
     */
    private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        String acceptorHost = "0.0.0.0";
        if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
            acceptorHost = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
        }

        int acceptorPort = (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT);
        return new InetSocketAddress(acceptorHost, acceptorPort);
    }

    private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE) && settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
    }


    public List<SessionID> getSessionIDs() {
        if (null != acceptor) {
            return acceptor.getSessions();
        }
        return null;
    }
}
