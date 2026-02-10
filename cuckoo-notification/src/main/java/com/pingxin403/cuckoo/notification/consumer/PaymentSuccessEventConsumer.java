package com.pingxin403.cuckoo.notification.consumer;

import com.pingxin403.cuckoo.common.event.PaymentSuccessEvent;
import com.pingxin403.cuckoo.common.kafka.AbstractEventConsumer;
import com.pingxin403.cuckoo.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 支付成功事件消费者
 * 监听 payment-events 主题，处理 PaymentSuccessEvent
 * 当支付成功时，创建"支付成功"通知
 */
@Slf4j
@Component
public class PaymentSuccessEventConsumer extends AbstractEventConsumer<PaymentSuccessEvent> {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "payment-events",
            groupId = "notification-payment-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());
        consume(event);
    }

    @Override
    protected void handleEvent(PaymentSuccessEvent event) {
        String content = String.format("您的订单 %d 支付成功，支付金额：%.2f 元",
                event.getOrderId(), event.getAmount());

        notificationService.createNotification(
                event.getUserId(),
                event.getOrderId(),
                "PAYMENT_SUCCESS",
                content
        );

        log.info("Created PAYMENT_SUCCESS notification for userId={}, orderId={}",
                event.getUserId(), event.getOrderId());
    }
}
