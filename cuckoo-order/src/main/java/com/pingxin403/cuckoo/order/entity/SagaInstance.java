package com.pingxin403.cuckoo.order.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Saga 实例实体
 * 用于记录 Saga 分布式事务的执行状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "saga_instance")
public class SagaInstance {
    
    /**
     * Saga 实例 ID（UUID）
     */
    @Id
    @Column(name = "saga_id", length = 64)
    private String sagaId;
    
    /**
     * Saga 类型（如 ORDER_CREATION）
     */
    @Column(name = "saga_type", length = 100, nullable = false)
    private String sagaType;
    
    /**
     * Saga 状态
     * RUNNING: 运行中
     * COMPLETED: 已完成
     * COMPENSATING: 补偿中
     * COMPENSATED: 已补偿
     * FAILED: 失败
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SagaStatus status;
    
    /**
     * 当前执行到的步骤索引
     */
    @Column(name = "current_step")
    private Integer currentStep;
    
    /**
     * Saga 上下文数据（JSON 格式）
     */
    @Column(name = "context", columnDefinition = "TEXT")
    private String context;
    
    /**
     * Saga 执行日志
     */
    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;
    
    /**
     * 开始时间
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 超时时间
     */
    @Column(name = "timeout_at", nullable = false)
    private LocalDateTime timeoutAt;
    
    /**
     * Saga 状态枚举
     */
    public enum SagaStatus {
        RUNNING,      // 运行中
        COMPLETED,    // 已完成
        COMPENSATING, // 补偿中
        COMPENSATED,  // 已补偿
        FAILED        // 失败
    }
}
