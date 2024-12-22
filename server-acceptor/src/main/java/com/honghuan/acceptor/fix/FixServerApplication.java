package com.honghuan.acceptor.fix;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Logon;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * FIX 服务端应用程序实现类
 * 负责处理所有 FIX 消息的接收和发送逻辑
 */
@Service
@Slf4j
public class FixServerApplication extends MessageCracker implements Application {

    /**
     * 处理登录消息
     * @param message Logon 消息对象
     * @param sessionID 会话 ID
     */
    @Handler
    public void onLogonMessage(Logon message, SessionID sessionID) {
        Session session = Session.lookupSession(sessionID);
        String targetCompID = sessionID.getTargetCompID();
        // 简单鉴权
        if (!Objects.equals(targetCompID, "client-honghuan")) {
            throw new RuntimeException("auth error");
        }
        // 3.允许登陆
        session.logon();
        session.sentLogon();
    }

    /**
     * 处理新订单消息
     * @param message 新订单消息对象
     * @param sessionID 会话 ID
     */
    @Handler
    public void onNewOrderMessage(NewOrderSingle message, SessionID sessionID) throws FieldNotFound {
        log.info("receive new order:{}", message.toString());
        String clOrdId = message.getClOrdID().getValue();
        String symbol = message.getSymbol().getValue();
        // 发送响应给客户端
        ExecutionReport report = new ExecutionReport();
        report.set(new ClOrdID(clOrdId));
        report.set(new OrderID(UUID.randomUUID().toString()));
        report.set(new ExecType(ExecType.NEW));
        report.set(new OrdStatus(OrdStatus.NEW));
        report.set(new TransactTime(LocalDateTime.now()));
        report.set(new ExecID("12345"));
        report.set(new Side(Side.BUY));
        report.set(new LeavesQty(1));
        report.set(new CumQty(1));
        report.set(new AvgPx(1.00D));
        report.set(new Symbol(symbol));
        report.set(new Text("11111"));

        try {
            boolean result = Session.sendToTarget(report, sessionID);
            log.warn("send new order res message:{}", result);
        } catch (SessionNotFound sessionNotFound) {
            log.error(sessionNotFound.getMessage(), sessionNotFound);
        }
    }

    /**
     * 处理取消订单消息
     * @param message 取消订单消息对象
     * @param sessionID 会话 ID
     */
    @Handler
    public void onCancelOrderMessage(OrderCancelRequest message, SessionID sessionID) {
        log.warn("receive new cancel order message:{}", message.toString());
    }

    // QuickFIX 生命周期回调方法
    @Override
    public void onCreate(SessionID sessionID) {
        log.info("会话创建");
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("会话登录成功");
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.info("会话登出");
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        log.info("发送管理类消息 {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.info("接收管理类消息 sessionId={}, message={}", sessionID, message);
        crack(message, sessionID);
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        log.info("发送应用消息 {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        log.info("接收应用消息 {}", message);
        crack(message, sessionID);
    }
}
