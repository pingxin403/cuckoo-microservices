package com.pingxin403.cuckoo.payment.service;

import com.pingxin403.cuckoo.common.event.EventPublisherUtil;
import com.pingxin403.cuckoo.common.event.PaymentFailedEvent;
import com.pingxin403.cuckoo.common.event.PaymentSuccessEvent;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.common.message.LocalMessageService;
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
    private final EventPublisherUtil eventPublisher;
    private final LocalMessageService localMessageService;

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

        // 在同一事务中保存 PaymentSuccessEvent 到本地消息表
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setEventType("PAYMENT_SUCCESS");
        event.setVersion(1);  // Changed from "1.0" to 1 (Integer)
        event.setOrderId(payment.getOrderId());
        event.setPaymentId(payment.getId());
        event.setUserId(payment.getUserId());
        event.setAmount(payment.getAmount());

        localMessageService.saveMessage(event);
        log.info("支付成功事件已保存到本地消息表: eventId={}, paymentId={}", event.getEventId(), payment.getId());

        // 异步发布事件到 Kafka（失败不影响事务提交）
        try {
            eventPublisher.publish(PAYMENT_EVENTS_TOPIC, payment.getOrderId().toString(), event);
            localMessageService.markAsSent(event.getEventId());
            log.info("支付成功事件已发布到 Kafka: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("发布支付成功事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
            // 消息保持 PENDING 状态，等待定时任务重试
        }

        return updated;
    }

    @Transactional
    public Payment failPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        Payment updated = paymentRepository.save(payment);

        // 在同一事务中保存 PaymentFailedEvent 到本地消息表
        PaymentFailedEvent event = new PaymentFailedEvent();
        event.setEventType("PAYMENT_FAILED");
        event.setVersion(1);  // Changed from "1.0" to 1 (Integer)
        event.setOrderId(payment.getOrderId());
        event.setPaymentId(payment.getId());
        event.setUserId(payment.getUserId());
        event.setReason(reason != null ? reason : "Payment failed");

        localMessageService.saveMessage(event);
        log.info("支付失败事件已保存到本地消息表: eventId={}, paymentId={}", event.getEventId(), payment.getId());

        // 异步发布事件到 Kafka（失败不影响事务提交）
        try {
            eventPublisher.publish(PAYMENT_EVENTS_TOPIC, payment.getOrderId().toString(), event);
            localMessageService.markAsSent(event.getEventId());
            log.info("支付失败事件已发布到 Kafka: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("发布支付失败事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
            // 消息保持 PENDING 状态，等待定时任务重试
        }

        return updated;
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }
}
