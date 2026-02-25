package com.pingxin403.cuckoo.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单读模型实体（CQRS 读模型）
 * 反范式设计，优化查询性能
 */
@Entity
@Table(name = "order_read", indexes = {
        @Index(name = "idx_user_id_created", columnList = "user_id, created_at"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRead {

    @Id
    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "status_display", length = 50)
    private String statusDisplay;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "product_names", columnDefinition = "TEXT")
    private String productNames;

    @Column(name = "sku_ids", columnDefinition = "TEXT")
    private String skuIds;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 获取状态显示文本
     */
    public static String getStatusDisplay(String status) {
        switch (status) {
            case "PENDING_PAYMENT":
                return "待支付";
            case "PAID":
                return "已支付";
            case "CANCELLED":
                return "已取消";
            default:
                return status;
        }
    }
}
