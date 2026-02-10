package com.pingxin403.cuckoo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件基类
 * 所有微服务之间通过 Kafka 传递的异步消息都继承此基类。
 * 每个事件包含唯一的 eventId（用于幂等性检查）、eventType、timestamp、version。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent {

    /**
     * 事件唯一标识（UUID），用于幂等性检查
     */
    private String eventId;

    /**
     * 事件类型名称（如 ORDER_CREATED, PAYMENT_SUCCESS 等）
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private Instant timestamp;

    /**
     * Schema 版本号
     */
    private String version;

    /**
     * 初始化事件基础字段的便捷方法。
     * 子类构造时调用此方法自动生成 eventId 和 timestamp。
     *
     * @param eventType 事件类型
     * @param version   Schema 版本号
     */
    protected void init(String eventType, String version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.version = version;
    }
}
