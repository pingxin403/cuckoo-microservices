package com.pingxin403.cuckoo.common.event;

import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 事件发布器接口
 * 定义事件发布的标准接口，支持同步和异步发布。
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
public interface EventPublisher {

    /**
     * 异步发布事件到 Kafka
     * 
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     * @return CompletableFuture 包含发布结果
     */
    <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(T event);

    /**
     * 异步发布事件到指定 topic
     * 
     * @param topic Kafka topic
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     * @return CompletableFuture 包含发布结果
     */
    <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(String topic, T event);

    /**
     * 异步发布事件到指定 topic，使用自定义 key
     * 
     * @param topic Kafka topic
     * @param key 消息 key
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     * @return CompletableFuture 包含发布结果
     */
    <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(String topic, String key, T event);

    /**
     * 同步发布事件（阻塞直到发布完成）
     * 
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     * @return 发布结果
     * @throws Exception 发布失败时抛出异常
     */
    <T extends DomainEvent> SendResult<String, DomainEvent> publishSync(T event) throws Exception;

    /**
     * 同步发布事件到指定 topic
     * 
     * @param topic Kafka topic
     * @param event 领域事件
     * @param <T> 事件类型，必须继承 DomainEvent
     * @return 发布结果
     * @throws Exception 发布失败时抛出异常
     */
    <T extends DomainEvent> SendResult<String, DomainEvent> publishSync(String topic, T event) throws Exception;

    /**
     * 批量异步发布事件
     * 
     * @param events 领域事件列表
     * @return CompletableFuture 包含所有发布结果
     */
    CompletableFuture<List<SendResult<String, DomainEvent>>> publishBatch(List<DomainEvent> events);
}
