package com.pingxin403.cuckoo.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ProcessedEvent 数据访问层
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * 根据 eventId 判断事件是否已被处理
     *
     * @param eventId 事件唯一标识
     * @return true 表示已处理（重复事件）
     */
    boolean existsByEventId(String eventId);
}
