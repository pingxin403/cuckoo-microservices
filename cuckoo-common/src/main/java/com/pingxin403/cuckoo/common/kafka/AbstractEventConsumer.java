package com.pingxin403.cuckoo.common.kafka;

import com.pingxin403.cuckoo.common.event.DomainEvent;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Kafka 事件消费者抽象基类
 * 封装 Kafka 消费 + 幂等性检查 + 错误日志的模板方法模式。
 * 各服务的 Kafka 消费者继承此基类，只需实现 handleEvent(T event) 方法即可。
 *
 * @param <T> 具体的领域事件类型
 */
@Slf4j
public abstract class AbstractEventConsumer<T extends DomainEvent> {

    @Autowired
    private IdempotencyService idempotencyService;

    /**
     * 模板方法：消费事件
     * 1. 幂等性检查 - 跳过已处理的事件
     * 2. 调用子类实现的 handleEvent 处理业务逻辑
     * 3. 标记事件为已处理
     * 4. 异常时记录错误日志并重新抛出（允许 Kafka 自动重试）
     *
     * @param event 领域事件
     */
    protected void consume(T event) {
        String eventId = event.getEventId();
        String eventType = event.getEventType();

        if (idempotencyService.isDuplicate(eventId)) {
            log.info("跳过重复事件: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        try {
            log.info("开始处理事件: eventId={}, eventType={}", eventId, eventType);
            handleEvent(event);
            idempotencyService.markProcessed(eventId);
            log.info("事件处理完成: eventId={}, eventType={}", eventId, eventType);
        } catch (Exception e) {
            log.error("事件处理失败: eventId={}, eventType={}", eventId, eventType, e);
            throw e; // 允许 Kafka 重试
        }
    }

    /**
     * 子类实现具体的事件处理逻辑
     *
     * @param event 领域事件
     */
    protected abstract void handleEvent(T event);
}
