package com.pingxin403.cuckoo.inventory.consumer;

import com.pingxin403.cuckoo.common.event.OrderCancelledEvent;
import com.pingxin403.cuckoo.common.kafka.AbstractEventConsumer;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 订单取消事件消费者
 * 监听 order-events 主题，处理 OrderCancelledEvent
 * 当订单取消时，释放该订单对应的预占库存
 */
@Slf4j
@Component
public class OrderCancelledEventConsumer extends AbstractEventConsumer<OrderCancelledEvent> {

    @Autowired
    private InventoryService inventoryService;

    @KafkaListener(topics = "order-events",
            groupId = "inventory-order-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, skuId={}, quantity={}, eventId={}",
                event.getOrderId(), event.getSkuId(), event.getQuantity(), event.getEventId());
        consume(event);
    }

    @Override
    protected void handleEvent(OrderCancelledEvent event) {
        InventoryOperationRequest releaseRequest = new InventoryOperationRequest(
                event.getSkuId(),
                event.getQuantity(),
                String.valueOf(event.getOrderId())
        );
        inventoryService.releaseStock(releaseRequest);
        log.info("Released stock for cancelled order: orderId={}, skuId={}, quantity={}",
                event.getOrderId(), event.getSkuId(), event.getQuantity());
    }
}
