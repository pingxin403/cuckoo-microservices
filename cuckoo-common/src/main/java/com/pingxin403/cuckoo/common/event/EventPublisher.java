package com.pingxin403.cuckoo.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 事件发布器工具类
 * 封装 Kafka 事件发布逻辑，自动设置 eventId 和 timestamp，统一日志记录。
 * 
 * 功能：
 * 1. 自动设置 eventId（如果为空，生成 UUID）
 * 2. 自动设置 timestamp（如果为空，设置当前时间）
 * 3. 统一记录包含 topic、key、eventType、eventId 的日志
 * 4. 提供两个重载方法：带 key 和不带 key（使用 eventId 作为默认 key）
 * 
 * 使用示例：
 * <pre>
 * // 使用自定义 key
 * OrderCancelledEvent event = OrderCancelledEvent.create(...);
 * eventPublisher.publish("order-events", order.getId().toString(), event);
 * 
 * // 使用 eventId 作为默认 key
 * eventPublisher.publish("order-events", event);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    /**
     * 发布事件到 Kafka（使用自定义 key）
     * 自动设置 eventId 和 timestamp（如果未设置）
     * 
     * @param topic Kafka topic
     * @param key 消息 key
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     */
    public <T extends DomainEvent> void publish(String topic, String key, T event) {
        try {
            // 自动设置 eventId（如果为空）
            if (event.getEventId() == null || event.getEventId().isEmpty()) {
                event.setEventId(UUID.randomUUID().toString());
            }
            
            // 自动设置 timestamp（如果为空）
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            
            // 发布事件
            kafkaTemplate.send(topic, key, event);
            
            // 记录日志
            log.info("发布事件成功: topic={}, key={}, eventType={}, eventId={}", 
                topic, key, event.getEventType(), event.getEventId());
        } catch (Exception e) {
            // 记录错误但不抛出异常，避免阻塞业务流程
            log.error("发布事件失败: topic={}, key={}, eventType={}, eventId={}", 
                topic, key, event.getEventType(), event.getEventId(), e);
        }
    }

    /**
     * 发布事件到 Kafka（使用 eventId 作为默认 key）
     * 自动设置 eventId 和 timestamp（如果未设置）
     * 
     * @param topic Kafka topic
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     */
    public <T extends DomainEvent> void publish(String topic, T event) {
        // 确保 eventId 已设置（用作默认 key）
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        
        // 使用 eventId 作为 key
        publish(topic, event.getEventId(), event);
    }
}
