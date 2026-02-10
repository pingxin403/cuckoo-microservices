package com.pingxin403.cuckoo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 支付失败事件
 * 发布者：Payment_Service
 * 消费者：Order_Service, Inventory_Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends DomainEvent {

    private Long orderId;
    private Long paymentId;
    private Long userId;
    private String reason;

    public PaymentFailedEvent(Long orderId, Long paymentId, Long userId, String reason, String version) {
        init("PAYMENT_FAILED", version);
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.reason = reason;
    }

    /**
     * 使用默认版本号 "1.0" 创建事件
     */
    public static PaymentFailedEvent create(Long orderId, Long paymentId, Long userId, String reason) {
        return new PaymentFailedEvent(orderId, paymentId, userId, reason, "1.0");
    }
}
