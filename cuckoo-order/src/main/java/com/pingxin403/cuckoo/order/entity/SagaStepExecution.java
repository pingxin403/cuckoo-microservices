package com.pingxin403.cuckoo.order.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Saga 步骤执行记录实体
 * 用于记录 Saga 中每个步骤的执行状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "saga_step_execution")
public class SagaStepExecution {
    
    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Saga 实例 ID
     */
    @Column(name = "saga_id", length = 64, nullable = false)
    private String sagaId;
    
    /**
     * 步骤名称
     */
    @Column(name = "step_name", length = 100, nullable = false)
    private String stepName;
    
    /**
     * 步骤顺序
     */
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    /**
     * 步骤状态
     * PENDING: 待执行
     * RUNNING: 执行中
     * COMPLETED: 已完成
     * FAILED: 失败
     * COMPENSATED: 已补偿
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private StepStatus status;
    
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
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 步骤状态枚举
     */
    public enum StepStatus {
        PENDING,     // 待执行
        RUNNING,     // 执行中
        COMPLETED,   // 已完成
        FAILED,      // 失败
        COMPENSATED  // 已补偿
    }
}
