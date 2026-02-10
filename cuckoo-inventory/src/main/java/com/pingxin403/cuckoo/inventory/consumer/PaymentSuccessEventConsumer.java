package com.pingxin403.cuckoo.inventory.consumer;

import com.pingxin403.cuckoo.common.event.PaymentSuccessEvent;
import com.pingxin403.cuckoo.common.kafka.AbstractEventConsumer;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.entity.InventoryLog;
import com.pingxin403.cuckoo.inventory.repository.InventoryLogRepository;
import com.pingxin403.cuckoo.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付成功事件消费者
 * 监听 payment-events 主题，处理 PaymentSuccessEvent
 * 当支付成功时，扣减对应订单的预占库存
 */
@Slf4j
@Component
public class PaymentSuccessEventConsumer extends AbstractEventConsumer<PaymentSuccessEvent> {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @KafkaListener(topics = "payment-events",
            groupId = "inventory-payment-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: orderId={}, eventId={}",
                event.getOrderId(), event.getEventId());
        consume(event);
    }

    @Override
    protected void handleEvent(PaymentSuccessEvent event) {
        String orderId = String.valueOf(event.getOrderId());

        // 从库存日志中查找该订单的 RESERVE 记录，获取 skuId 和 quantity
        List<InventoryLog> reserveLogs = inventoryLogRepository.findByOrderId(orderId);
        List<InventoryLog> reserveEntries = reserveLogs.stream()
                .filter(logEntry -> "RESERVE".equals(logEntry.getOperation()))
                .toList();

        for (InventoryLog reserveLog : reserveEntries) {
            InventoryOperationRequest deductRequest = new InventoryOperationRequest(
                    reserveLog.getSkuId(),
                    reserveLog.getQuantity(),
                    orderId
            );
            inventoryService.deductStock(deductRequest);
            log.info("Deducted stock for orderId={}, skuId={}, quantity={}",
                    orderId, reserveLog.getSkuId(), reserveLog.getQuantity());
        }
    }
}
