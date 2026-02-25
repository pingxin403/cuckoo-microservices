package com.pingxin403.cuckoo.order.saga.steps;

import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.event.OrderCreatedEvent;
import com.pingxin403.cuckoo.common.message.LocalMessageService;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.saga.CompensationException;
import com.pingxin403.cuckoo.order.saga.SagaContext;
import com.pingxin403.cuckoo.order.saga.SagaStep;
import com.pingxin403.cuckoo.order.saga.SagaStepException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 发送通知步骤
 * 执行：发布订单创建事件
 * 补偿：无需补偿（通知不影响业务）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendNotificationStep implements SagaStep {
    
    private final EventPublisher eventPublisher;
    private final LocalMessageService localMessageService;
    
    @Override
    public void execute(SagaContext context) throws SagaStepException {
        try {
            CreateOrderRequest request = context.get("orderRequest");
            Long orderId = context.get("orderId");
            BigDecimal totalAmount = context.get("totalAmount");
            
            // 创建订单创建事件
            OrderCreatedEvent event = OrderCreatedEvent.create(
                    String.valueOf(orderId),  // Convert Long to String
                    request.getUserId(),
                    request.getSkuId(),
                    request.getQuantity(),
                    totalAmount
            );
            
            // 保存到本地消息表
            localMessageService.saveMessage(event);
            log.info("订单创建事件已保存到本地消息表: eventId={}, orderId={}", event.getEventId(), orderId);
            
            // 发布事件到 Kafka
            try {
                eventPublisher.publish("order-events", orderId.toString(), event);
                localMessageService.markAsSent(event.getEventId());
                log.info("订单创建事件已发布到 Kafka: eventId={}", event.getEventId());
            } catch (Exception e) {
                log.error("发布订单创建事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
                // 消息保持 PENDING 状态，等待定时任务重试
            }
            
        } catch (Exception e) {
            log.error("发送通知失败", e);
            throw new SagaStepException("发送通知失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void compensate(SagaContext context) throws CompensationException {
        // 通知步骤无需补偿
        log.info("通知步骤无需补偿");
    }
    
    @Override
    public String getName() {
        return "SendNotification";
    }
}
