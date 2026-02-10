package com.pingxin403.cuckoo.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 库存操作流水日志实体
 * 记录每次库存操作（RESERVE、DEDUCT、RELEASE）的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_log", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_sku_id", columnList = "sku_id")
})
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SKU ID
     */
    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    /**
     * 订单 ID
     */
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    /**
     * 操作类型：RESERVE, DEDUCT, RELEASE
     */
    @Column(nullable = false, length = 20)
    private String operation;

    /**
     * 操作数量
     */
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
