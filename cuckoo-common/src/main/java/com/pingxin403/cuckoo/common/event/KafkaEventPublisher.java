package com.pingxin403.cuckoo.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 事件发布器实现
 * 使用 KafkaTemplate 实现事件发布，支持同步和异步发布，包含失败重试逻辑。
 * 
 * 功能：
 * 1. 自动设置 eventId 和 timestamp（如果未设置）
 * 2. 支持异步发布（返回 CompletableFuture）
 * 3. 支持同步发布（阻塞等待结果）
 * 4. 支持批量发布
 * 5. 自动重试机制（最多 3 次，指数退避）
 * 6. 统一日志记录
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    /**
     * 默认 topic 映射：根据事件类型自动选择 topic
     */
    private static final String DEFAULT_TOPIC = "domain-events";

    @Override
    public <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(T event) {
        String topic = getTopicForEvent(event);
        return publish(topic, event);
    }

    @Override
    public <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(String topic, T event) {
        prepareEvent(event);
        return publish(topic, event.getEventId(), event);
    }

    @Override
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        retryFor = {Exception.class}
    )
    public <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(
            String topic, String key, T event) {
        
        prepareEvent(event);
        
        log.info("发布事件: topic={}, key={}, eventType={}, eventId={}", 
            topic, key, event.getEventType(), event.getEventId());
        
        CompletableFuture<SendResult<String, DomainEvent>> future = kafkaTemplate.send(topic, key, event);
        
        // 添加回调处理
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("事件发布成功: topic={}, partition={}, offset={}, eventType={}, eventId={}", 
                    topic, 
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    event.getEventType(), 
                    event.getEventId());
            } else {
                log.error("事件发布失败: topic={}, key={}, eventType={}, eventId={}, error={}", 
                    topic, key, event.getEventType(), event.getEventId(), ex.getMessage(), ex);
            }
        });
        
        return future;
    }

    @Override
    public <T extends DomainEvent> SendResult<String, DomainEvent> publishSync(T event) throws Exception {
        String topic = getTopicForEvent(event);
        return publishSync(topic, event);
    }

    @Override
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        retryFor = {Exception.class}
    )
    public <T extends DomainEvent> SendResult<String, DomainEvent> publishSync(String topic, T event) throws Exception {
        prepareEvent(event);
        
        log.info("同步发布事件: topic={}, key={}, eventType={}, eventId={}", 
            topic, event.getEventId(), event.getEventType(), event.getEventId());
        
        try {
            // 同步发送，等待结果（最多 10 秒）
            SendResult<String, DomainEvent> result = kafkaTemplate.send(topic, event.getEventId(), event)
                .get(10, TimeUnit.SECONDS);
            
            log.info("事件同步发布成功: topic={}, partition={}, offset={}, eventType={}, eventId={}", 
                topic,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                event.getEventType(), 
                event.getEventId());
            
            return result;
        } catch (Exception e) {
            log.error("事件同步发布失败: topic={}, eventType={}, eventId={}", 
                topic, event.getEventType(), event.getEventId(), e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<List<SendResult<String, DomainEvent>>> publishBatch(List<DomainEvent> events) {
        log.info("批量发布事件: count={}", events.size());
        
        List<CompletableFuture<SendResult<String, DomainEvent>>> futures = new ArrayList<>();
        
        for (DomainEvent event : events) {
            CompletableFuture<SendResult<String, DomainEvent>> future = publish(event);
            futures.add(future);
        }
        
        // 等待所有发布完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<SendResult<String, DomainEvent>> results = new ArrayList<>();
                for (CompletableFuture<SendResult<String, DomainEvent>> future : futures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        log.error("批量发布中的某个事件失败", e);
                    }
                }
                log.info("批量发布完成: total={}, success={}", events.size(), results.size());
                return results;
            });
    }

    /**
     * 准备事件：自动设置 eventId、timestamp 和 traceId
     */
    private <T extends DomainEvent> void prepareEvent(T event) {
        // 自动设置 eventId（如果为空）
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        
        // 自动设置 timestamp（如果为空）
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }
        
        // 自动设置 traceId（从 MDC 获取，如果存在）
        if (event.getTraceId() == null || event.getTraceId().isEmpty()) {
            try {
                String traceId = org.slf4j.MDC.get("traceId");
                if (traceId != null) {
                    event.setTraceId(traceId);
                }
            } catch (Exception e) {
                // 忽略 MDC 获取失败
            }
        }
    }

    /**
     * 根据事件类型自动选择 topic
     */
    private String getTopicForEvent(DomainEvent event) {
        String eventType = event.getEventType();
        if (eventType == null) {
            return DEFAULT_TOPIC;
        }
        
        // 根据事件类型映射到对应的 topic
        if (eventType.startsWith("ORDER_")) {
            return "order-events";
        } else if (eventType.startsWith("PAYMENT_")) {
            return "payment-events";
        } else if (eventType.startsWith("INVENTORY_")) {
            return "inventory-events";
        } else if (eventType.startsWith("NOTIFICATION_")) {
            return "notification-events";
        }
        
        return DEFAULT_TOPIC;
    }
}
