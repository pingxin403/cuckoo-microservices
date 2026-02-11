package com.pingxin403.cuckoo.payment.service;

import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.event.PaymentFailedEvent;
import com.pingxin403.cuckoo.common.event.PaymentSuccessEvent;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.payment.entity.Payment;
import com.pingxin403.cuckoo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    @Transactional
    public Payment createPayment(Payment payment) {
        payment.setStatus(Payment.PaymentStatus.PENDING);
        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: id={}, orderId={}, amount={}", 
                saved.getId(), saved.getOrderId(), saved.getAmount());
        return saved;
    }

    @Transactional
    public Payment confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        Payment updated = paymentRepository.save(payment);

        // 发布支付成功事件
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setEventType("PAYMENT_SUCCESS");
        event.setVersion("1.0");
        event.setOrderId(payment.getOrderId());
        event.setPaymentId(payment.getId());
        event.setUserId(payment.getUserId());
        event.setAmount(payment.getAmount());

        eventPublisher.publish(PAYMENT_EVENTS_TOPIC, payment.getOrderId().toString(), event);

        return updated;
    }

    @Transactional
    public Payment failPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        Payment updated = paymentRepository.save(payment);

        // 发布支付失败事件
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventType("PAYMENT_FAILED");
        event.setVersion("1.0");
        event.setOrderId(payment.getOrderId());
        event.setPaymentId(payment.getId());
        event.setUserId(payment.getUserId());
        event.setReason(reason != null ? reason : "Payment failed");

        eventPublisher.publish(PAYMENT_EVENTS_TOPIC, payment.getOrderId().toString(), event);

        return updated;
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }
}
