package com.pingxin403.cuckoo.order.client;

import com.pingxin403.cuckoo.common.feign.BaseFeignConfig;
import com.pingxin403.cuckoo.order.client.fallback.PaymentClientFallback;
import com.pingxin403.cuckoo.order.dto.CreatePaymentRequest;
import com.pingxin403.cuckoo.order.dto.PaymentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 支付服务 Feign 客户端
 */
@FeignClient(
    name = "payment-service",
    configuration = BaseFeignConfig.class,
    fallback = PaymentClientFallback.class
)
public interface PaymentClient {

    /**
     * 创建支付单
     */
    @PostMapping("/api/payments")
    PaymentDTO createPayment(@RequestBody CreatePaymentRequest request);
}
