package com.pingxin403.cuckoo.payment.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.payment.dto.CreatePaymentRequest;
import com.pingxin403.cuckoo.payment.dto.PaymentDTO;
import com.pingxin403.cuckoo.payment.entity.Payment;
import com.pingxin403.cuckoo.payment.mapper.PaymentMapper;
import com.pingxin403.cuckoo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    @SentinelResource(value = "POST:/api/payments")
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody CreatePaymentRequest request) {
        logRequest("创建支付", request.getOrderId(), request.getUserId(), request.getAmount());
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());

        Payment created = paymentService.createPayment(payment);
        logResponse("创建支付", created.getId());
        return created(paymentMapper.toDTO(created));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentDTO> confirmPayment(@PathVariable Long id) {
        logRequest("确认支付", id);
        Payment payment = paymentService.confirmPayment(id);
        logResponse("确认支付", payment.getId());
        return ok(paymentMapper.toDTO(payment));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<PaymentDTO> failPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        logRequest("支付失败", id, reason);
        Payment payment = paymentService.failPayment(id, reason);
        logResponse("支付失败", payment.getId());
        return ok(paymentMapper.toDTO(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long id) {
        logRequest("查询支付", id);
        Payment payment = paymentService.getPaymentById(id);
        logResponse("查询支付", payment.getId());
        return ok(paymentMapper.toDTO(payment));
    }
}
