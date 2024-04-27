package com.honghuan.initiator.fix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class FixClientApplication extends MessageCracker implements Application {

    private static SessionID sessionID;

    @Override
    public void onCreate(SessionID sessionID) {
        log.info("onCreate");
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("onLogon 客户端登陆成功");
        this.sessionID = sessionID;
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.warn("onLogout 客户端断开连接");
        this.sessionID = null;
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        log.info("toAdmin msg 发送管理消息 {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.info("fromAdmin msg 接收管理消息 {}", message);
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        log.info("toApp msg 发送业务消息 {}", message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        log.info("fromApp msg 接收业务消息 {}", message);
        crack(message, sessionID);
    }

    @Handler
    public void onExecutionReportMessage(ExecutionReport message, SessionID sessionID) throws FieldNotFound {
        String orderId = message.getString(OrderID.FIELD);
        char ordStatus = message.getChar(OrdStatus.FIELD);

        log.warn("receive order execution report msg, orderId={}", orderId);
        switch (ordStatus) {
            case OrdStatus.NEW:
                log.warn("create new order:{}", orderId);
                break;
            case OrdStatus.PENDING_CANCEL:
                log.warn("cancel order:{},msg={}", orderId);
                break;
        }

    }

    public boolean createOrder() throws SessionNotFound {
        NewOrderSingle newOrderSingle = new NewOrderSingle(
                new ClOrdID("IT001"), new Side(Side.BUY),
                new TransactTime(LocalDateTime.now()), new OrdType(OrdType.MARKET));
        newOrderSingle.set(new OrderQty(0));
        newOrderSingle.set(new CashOrderQty(0));
        newOrderSingle.set(new Price(0));
        newOrderSingle.set(new Symbol("BTC"));
        newOrderSingle.set(new HandlInst('1'));
        newOrderSingle.set(new Currency("CNY"));
        newOrderSingle.set(new TimeInForce(TimeInForce.DAY));
        return Session.sendToTarget(newOrderSingle, sessionID);
    }

    public Boolean cancelOrder() throws SessionNotFound {
        OrderCancelRequest request = new OrderCancelRequest();
        request.set(new ClOrdID(UUID.randomUUID().toString()));
        request.set(new OrderID("123"));
        request.set(new OrderQty(2D));
        request.set(new OrigClOrdID("XXX"));
        request.set(new Side(Side.BUY));
        request.set(new Symbol("BTC"));
        request.set(new TransactTime(LocalDateTime.now()));
        request.set(new Text("Cancel Order!"));
        boolean b = Session.sendToTarget(request, sessionID);

        return b;
    }
}
