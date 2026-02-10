package com.pingxin403.cuckoo.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 已处理事件 JPA 实体
 * 用于基于 eventId 的幂等性检查，防止 Kafka 事件重复消费。
 * 需要消费 Kafka 事件的服务（Order、Inventory、Notification）在各自数据库中创建 processed_events 表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 事件唯一标识（对应 DomainEvent.eventId）
     */
    @Column(unique = true, nullable = false, length = 50)
    private String eventId;

    /**
     * 事件处理时间
     */
    @Column(nullable = false)
    private Instant processedAt;

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }
}
