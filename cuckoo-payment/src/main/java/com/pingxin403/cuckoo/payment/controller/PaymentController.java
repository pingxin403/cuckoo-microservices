package com.pingxin403.cuckoo.payment.controller;

import com.pingxin403.cuckoo.payment.dto.CreatePaymentRequest;
import com.pingxin403.cuckoo.payment.dto.PaymentDTO;
import com.pingxin403.cuckoo.payment.entity.Payment;
import com.pingxin403.cuckoo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());

        Payment created = paymentService.createPayment(payment);
        return ResponseEntity.ok(PaymentDTO.fromEntity(created));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentDTO> confirmPayment(@PathVariable Long id) {
        Payment payment = paymentService.confirmPayment(id);
        return ResponseEntity.ok(PaymentDTO.fromEntity(payment));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<PaymentDTO> failPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Payment payment = paymentService.failPayment(id, reason);
        return ResponseEntity.ok(PaymentDTO.fromEntity(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(PaymentDTO.fromEntity(payment));
    }
}
