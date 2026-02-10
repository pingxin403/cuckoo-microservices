package com.pingxin403.cuckoo.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 幂等性检查服务
 * 封装基于 eventId 的幂等性检查逻辑，避免 Kafka 事件重复消费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * 检查事件是否已被处理（重复事件）
     *
     * @param eventId 事件唯一标识
     * @return true 表示已处理，应跳过
     */
    public boolean isDuplicate(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    /**
     * 标记事件为已处理
     *
     * @param eventId 事件唯一标识
     */
    @Transactional
    public void markProcessed(String eventId) {
        if (!isDuplicate(eventId)) {
            processedEventRepository.save(new ProcessedEvent(eventId));
            log.debug("Event marked as processed: {}", eventId);
        }
    }
}
