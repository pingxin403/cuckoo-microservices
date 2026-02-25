package com.pingxin403.cuckoo.common.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 领域事件基类
 * 所有微服务之间通过 Kafka 传递的异步消息都继承此基类。
 * 每个事件包含唯一的 eventId（用于幂等性检查）、eventType、timestamp、version 和 payload。
 * 
 * Requirements: 1.8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
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
     * 事件发生时间（毫秒时间戳）
     */
    private Long timestamp;

    /**
     * Schema 版本号，用于支持事件演化和向后兼容
     */
    private Integer version;

    /**
     * 事件负载数据，用于存储额外的元数据或扩展字段
     */
    private Map<String, Object> payload;

    /**
     * 链路追踪 ID，用于关联分布式调用链
     */
    private String traceId;

    /**
     * 初始化事件基础字段的便捷方法。
     * 子类构造时调用此方法自动生成 eventId 和 timestamp。
     *
     * @param eventType 事件类型
     * @param version   Schema 版本号
     */
    protected void init(String eventType, Integer version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
        this.version = version;
        this.payload = new HashMap<>();
    }

    /**
     * 添加负载数据
     *
     * @param key   键
     * @param value 值
     */
    public void addPayload(String key, Object value) {
        if (this.payload == null) {
            this.payload = new HashMap<>();
        }
        this.payload.put(key, value);
    }

    /**
     * 获取负载数据
     *
     * @param key 键
     * @return 值
     */
    public Object getPayload(String key) {
        return this.payload != null ? this.payload.get(key) : null;
    }

    /**
     * 序列化为 JSON 字符串
     *
     * @return JSON 字符串
     * @throws JsonProcessingException 序列化异常
     */
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.writeValueAsString(this);
    }

    /**
     * 从 JSON 字符串反序列化
     *
     * @param json  JSON 字符串
     * @param clazz 事件类型
     * @param <T>   事件类型泛型
     * @return 事件对象
     * @throws JsonProcessingException 反序列化异常
     */
    public static <T extends DomainEvent> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(json, clazz);
    }
}
