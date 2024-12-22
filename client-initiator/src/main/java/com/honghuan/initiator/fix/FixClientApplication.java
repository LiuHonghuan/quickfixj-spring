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

/**
 * FIX 客户端应用程序实现类
 * 负责处理所有 FIX 消息的接收和发送逻辑
 */
@Service
@Slf4j
public class FixClientApplication extends MessageCracker implements Application {

    /**
     * 当前活动的会话 ID
     */
    private static SessionID sessionID;

    // QuickFIX 生命周期回调方法
    @Override
    public void onCreate(SessionID sessionID) {
        log.info("会话创建");
    }

    @Override
    public void onLogon(SessionID sessionID) {
        log.info("客户端登录成功");
        this.sessionID = sessionID;
    }

    @Override
    public void onLogout(SessionID sessionID) {
        log.warn("客户端断开连接");
        this.sessionID = null;
    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        log.info("发送管理类消息 {}", message);
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        log.info("接收管理类消息 {}", message);
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

    /**
     * 处理执行报告消息
     * @param message 执行报告消息对象
     * @param sessionID 会话 ID
     */
    @Handler
    public void onExecutionReportMessage(ExecutionReport message, SessionID sessionID) throws FieldNotFound {
        String orderId = message.getString(OrderID.FIELD);
        char ordStatus = message.getChar(OrdStatus.FIELD);

        log.warn("收到订单执行报告, orderId={}", orderId);
        switch (ordStatus) {
            case OrdStatus.NEW:
                log.warn("新建订单:{}", orderId);
                break;
            case OrdStatus.PENDING_CANCEL:
                log.warn("取消订单:{}", orderId);
                break;
        }
    }

    /**
     * 创建新订单
     * @return 发送是否成功
     */
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

    /**
     * 取消订单
     * @return 发送是否成功
     */
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
        return Session.sendToTarget(request, sessionID);
    }
}
