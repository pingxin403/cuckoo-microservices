package com.pingxin403.cuckoo.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付成功事件
 * 发布者：Payment_Service
 * 消费者：Order_Service, Inventory_Service, Notification_Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentSuccessEvent extends DomainEvent {

    private Long orderId;
    private Long paymentId;
    private Long userId;
    private BigDecimal amount;

    public PaymentSuccessEvent(Long orderId, Long paymentId, Long userId, BigDecimal amount, String version) {
        init("PAYMENT_SUCCESS", version);
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.amount = amount;
    }

    /**
     * 使用默认版本号 "1.0" 创建事件
     */
    public static PaymentSuccessEvent create(Long orderId, Long paymentId, Long userId, BigDecimal amount) {
        return new PaymentSuccessEvent(orderId, paymentId, userId, amount, "1.0");
    }
}
