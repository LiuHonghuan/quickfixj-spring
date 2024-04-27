package com.honghuan.initiator.fix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import quickfix.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.InputStream;

@Service
@Slf4j
public class FixClient {

    /**
     * The Initiator.
     */
    private Initiator initiator = null;
    /**
     * The Initiator started.
     */
    private boolean initiatorStarted = false;

    /**
     * The Client application.
     */
    @Resource
    private FixClientApplication clientApplication;

    /**
     * Start.
     */
    @PostConstruct
    public void start() {
        try (InputStream inputStream = new DefaultResourceLoader().getResource("classpath:client.cfg").getInputStream()) {
            SessionSettings settings = new SessionSettings(inputStream);
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
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
     * Logon.
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
