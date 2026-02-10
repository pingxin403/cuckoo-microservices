package com.pingxin403.cuckoo.notification.consumer;

import com.pingxin403.cuckoo.common.event.OrderCancelledEvent;
import com.pingxin403.cuckoo.common.kafka.AbstractEventConsumer;
import com.pingxin403.cuckoo.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 订单取消事件消费者
 * 监听 order-events 主题，处理 OrderCancelledEvent
 * 当订单取消时，创建"订单取消"通知
 */
@Slf4j
@Component
public class OrderCancelledEventConsumer extends AbstractEventConsumer<OrderCancelledEvent> {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "order-events",
            groupId = "notification-order-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());
        consume(event);
    }

    @Override
    protected void handleEvent(OrderCancelledEvent event) {
        String content = String.format("您的订单 %d 已取消，取消原因：%s",
                event.getOrderId(), event.getReason());

        notificationService.createNotification(
                event.getUserId(),
                event.getOrderId(),
                "ORDER_CANCELLED",
                content
        );

        log.info("Created ORDER_CANCELLED notification for userId={}, orderId={}",
                event.getUserId(), event.getOrderId());
    }
}
