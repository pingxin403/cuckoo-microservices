package com.pingxin403.cuckoo.order.consumer;

import com.pingxin403.cuckoo.common.event.PaymentSuccessEvent;
import com.pingxin403.cuckoo.common.kafka.AbstractEventConsumer;
import com.pingxin403.cuckoo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 支付成功事件消费者
 * 监听 payment-events 主题，处理 PaymentSuccessEvent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessEventConsumer extends AbstractEventConsumer<PaymentSuccessEvent> {

    private final OrderService orderService;

    /**
     * 监听 payment-events 主题
     */
    @KafkaListener(
        topics = "payment-events", 
        groupId = "order-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        log.info("收到 PaymentSuccessEvent: eventId={}, orderId={}, paymentId={}",
                event.getEventId(), event.getOrderId(), event.getPaymentId());
        consume(event);
    }

    /**
     * 处理支付成功事件
     * 更新订单状态为已支付
     */
    @Override
    protected void handleEvent(PaymentSuccessEvent event) {
        orderService.updateOrderToPaid(event.getOrderId(), event.getPaymentId());
    }
}
