package com.pingxin403.cuckoo.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 通知实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户 ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 订单 ID（可选）
     */
    @Column
    private Long orderId;

    /**
     * 通知类型：PAYMENT_SUCCESS, ORDER_CANCELLED
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 通知内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Notification(Long userId, Long orderId, String type, String content) {
        this.userId = userId;
        this.orderId = orderId;
        this.type = type;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
