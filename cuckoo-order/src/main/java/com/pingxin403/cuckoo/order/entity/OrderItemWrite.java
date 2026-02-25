package com.pingxin403.cuckoo.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细写模型实体
 */
@Entity
@Table(name = "order_item_write", indexes = {
        @Index(name = "idx_order_id", columnList = "order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemWrite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
