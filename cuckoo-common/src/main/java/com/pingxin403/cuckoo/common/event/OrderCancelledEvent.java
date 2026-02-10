package com.pingxin403.cuckoo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 订单取消事件
 * 发布者：Order_Service
 * 消费者：Inventory_Service, Notification_Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends DomainEvent {

    private Long orderId;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private String reason;

    public OrderCancelledEvent(Long orderId, Long userId, Long skuId, Integer quantity, String reason, String version) {
        init("ORDER_CANCELLED", version);
        this.orderId = orderId;
        this.userId = userId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.reason = reason;
    }

    /**
     * 使用默认版本号 "1.0" 创建事件
     */
    public static OrderCancelledEvent create(Long orderId, Long userId, Long skuId, Integer quantity, String reason) {
        return new OrderCancelledEvent(orderId, userId, skuId, quantity, reason, "1.0");
    }
}
