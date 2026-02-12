package com.pingxin403.cuckoo.order.client.fallback;

import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.dto.CreatePaymentRequest;
import com.pingxin403.cuckoo.order.dto.PaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PaymentClient 降级处理
 * 当支付服务不可用时，返回默认支付信息
 */
@Slf4j
@Component
public class PaymentClientFallback implements PaymentClient {

    @Override
    public PaymentDTO createPayment(CreatePaymentRequest request) {
        log.warn("支付服务调用失败，执行降级逻辑，订单ID: {}", request.getOrderId());
        
        // 返回默认支付信息，表示支付创建失败
        PaymentDTO fallbackPayment = new PaymentDTO();
        fallbackPayment.setOrderId(request.getOrderId());
        fallbackPayment.setUserId(request.getUserId());
        fallbackPayment.setAmount(request.getAmount());
        fallbackPayment.setStatus("PENDING");
        
        return fallbackPayment;
    }
}
