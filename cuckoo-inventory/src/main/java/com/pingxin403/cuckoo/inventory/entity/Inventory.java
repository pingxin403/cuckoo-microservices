package com.pingxin403.cuckoo.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 库存实体
 * 库存不变量：total_stock = available_stock + reserved_stock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SKU ID（唯一标识商品库存）
     */
    @Column(name = "sku_id", nullable = false, unique = true)
    private Long skuId;

    /**
     * 总库存
     */
    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    /**
     * 可用库存
     */
    @Column(name = "available_stock", nullable = false)
    private Integer availableStock;

    /**
     * 预占库存
     */
    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock;

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
}
