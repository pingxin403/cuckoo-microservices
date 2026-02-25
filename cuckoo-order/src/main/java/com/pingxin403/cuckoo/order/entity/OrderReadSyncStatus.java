package com.pingxin403.cuckoo.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 读模型同步状态实体
 * 用于追踪读模型同步进度和失败重试
 */
@Entity
@Table(name = "order_read_sync_status", indexes = {
        @Index(name = "idx_sync_status", columnList = "sync_status"),
        @Index(name = "idx_order_id", columnList = "order_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_id", columnNames = "event_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadSyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "sync_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 同步状态枚举
     */
    public enum SyncStatus {
        PENDING,   // 待同步
        SUCCESS,   // 同步成功
        FAILED     // 同步失败
    }
}
