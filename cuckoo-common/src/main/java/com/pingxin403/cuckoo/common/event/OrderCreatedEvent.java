package com.pingxin403.cuckoo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单创建事件
 * 发布者：Order_Service
 * 消费者：Notification_Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends DomainEvent {

    private String orderId;  // Changed from Long to String for CQRS UUID support
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, Long userId, Long skuId, Integer quantity, BigDecimal totalAmount, Integer version) {
        init("ORDER_CREATED", version);
        this.orderId = orderId;
        this.userId = userId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    /**
     * 使用默认版本号 1 创建事件
     */
    public static OrderCreatedEvent create(String orderId, Long userId, Long skuId, Integer quantity, BigDecimal totalAmount) {
        return new OrderCreatedEvent(orderId, userId, skuId, quantity, totalAmount, 1);
    }
}
