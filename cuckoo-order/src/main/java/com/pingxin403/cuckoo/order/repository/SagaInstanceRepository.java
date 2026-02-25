package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.SagaInstance;
import com.pingxin403.cuckoo.order.entity.SagaInstance.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 实例仓储接口
 */
@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, String> {
    
    /**
     * 根据状态查询 Saga 实例
     */
    List<SagaInstance> findByStatus(SagaStatus status);
    
    /**
     * 查询超时的 Saga 实例
     * 用于定时任务扫描并触发补偿
     */
    List<SagaInstance> findByStatusAndTimeoutAtBefore(SagaStatus status, LocalDateTime timeoutAt);
    
    /**
     * 根据 Saga 类型查询
     */
    List<SagaInstance> findBySagaType(String sagaType);
}
