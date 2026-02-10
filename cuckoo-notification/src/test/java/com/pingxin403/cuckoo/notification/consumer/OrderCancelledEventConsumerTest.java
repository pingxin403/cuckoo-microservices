package com.pingxin403.cuckoo.notification.consumer;

import com.pingxin403.cuckoo.common.event.OrderCancelledEvent;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.notification.dto.NotificationDTO;
import com.pingxin403.cuckoo.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderCancelledEventConsumer 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderCancelledEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderCancelledEventConsumer consumer;

    private OrderCancelledEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = OrderCancelledEvent.create(200L, 100L, 1001L, 2, "支付超时");
    }

    @Test
    void onMessage_ShouldCreateNotification() {
        // Given
        when(idempotencyService.isDuplicate(testEvent.getEventId())).thenReturn(false);
        when(notificationService.createNotification(any(), any(), any(), any()))
                .thenReturn(new NotificationDTO(1L, 100L, 200L, "ORDER_CANCELLED",
                        "您的订单 200 已取消，取消原因：支付超时", Instant.now()));

        // When
        consumer.onMessage(testEvent);

        // Then
        verify(idempotencyService).isDuplicate(testEvent.getEventId());
        verify(notificationService).createNotification(
                eq(100L),
                eq(200L),
                eq("ORDER_CANCELLED"),
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
