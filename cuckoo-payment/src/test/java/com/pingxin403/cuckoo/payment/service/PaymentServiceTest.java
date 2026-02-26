package com.pingxin403.cuckoo.payment.service;

import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.common.message.LocalMessageService;
import com.pingxin403.cuckoo.payment.entity.Payment;
import com.pingxin403.cuckoo.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private LocalMessageService localMessageService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setUserId(1L);
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setStatus(Payment.PaymentStatus.PENDING);
    }

    @Test
    void createPayment_shouldCreateWithPendingStatus() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.createPayment(testPayment);

        assertNotNull(result);
        assertEquals(Payment.PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void confirmPayment_shouldUpdateStatusAndPublishEvent() {
        testPayment.setStatus(Payment.PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.confirmPayment(1L);

        assertEquals(Payment.PaymentStatus.SUCCESS, result.getStatus());
        verify(eventPublisher).publish(anyString(), anyString(), any());
    }

    @Test
    void confirmPayment_shouldThrowExceptionWhenNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.confirmPayment(999L);
        });
    }

    @Test
    void failPayment_shouldUpdateStatusAndPublishEvent() {
        testPayment.setStatus(Payment.PaymentStatus.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.failPayment(1L, "Insufficient funds");

        assertEquals(Payment.PaymentStatus.FAILED, result.getStatus());
        verify(eventPublisher).publish(anyString(), anyString(), any());
    }

    @Test
    void getPaymentById_shouldReturnPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getPaymentById_shouldThrowExceptionWhenNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.getPaymentById(999L);
        });
    }
}
