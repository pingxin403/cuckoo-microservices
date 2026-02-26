package com.pingxin403.cuckoo.notification.consumer;

import com.pingxin403.cuckoo.common.event.PaymentSuccessEvent;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PaymentSuccessEventConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PaymentSuccessEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentSuccessEventConsumer consumer;

    private PaymentSuccessEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = PaymentSuccessEvent.create(200L, 300L, 100L, new BigDecimal("99.99"), "ALIPAY");
    }

    @Test
    void onMessage_ShouldCreateNotification() {
        // Given
        when(idempotencyService.isDuplicate(testEvent.getEventId())).thenReturn(false);
        when(notificationService.createNotification(any(), any(), any(), any()))
                .thenReturn(new NotificationDTO(1L, 100L, 200L, "PAYMENT_SUCCESS",
                        "您的订单 200 支付成功，支付金额：99.99 元", Instant.now()));

        // When
        consumer.onMessage(testEvent);

        // Then
        verify(idempotencyService).isDuplicate(testEvent.getEventId());
        verify(notificationService).createNotification(
                eq(100L),
                eq(200L),
                eq("PAYMENT_SUCCESS"),
                any(String.class)
        );
        verify(idempotencyService).markProcessed(testEvent.getEventId());
    }

    @Test
    void onMessage_WhenDuplicateEvent_ShouldSkipProcessing() {
        // Given
        when(idempotencyService.isDuplicate(testEvent.getEventId())).thenReturn(true);

        // When
        consumer.onMessage(testEvent);

        // Then
        verify(idempotencyService).isDuplicate(testEvent.getEventId());
        verify(notificationService, never()).createNotification(any(), any(), any(), any());
        verify(idempotencyService, never()).markProcessed(any());
    }
}
