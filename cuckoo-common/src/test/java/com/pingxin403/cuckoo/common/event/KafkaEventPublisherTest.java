package com.pingxin403.cuckoo.common.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * KafkaEventPublisher 单元测试
 * 验证事件发布的核心功能
 */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    private KafkaEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new KafkaEventPublisher(kafkaTemplate);
    }

    @Test
    void shouldPublishEventAsynchronously() {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        CompletableFuture<SendResult<String, DomainEvent>> result = eventPublisher.publish(event);

        // Then
        assertThat(result).isNotNull();
        verify(kafkaTemplate).send(eq("order-events"), any(String.class), eq(event));
    }

    @Test
    void shouldPublishEventToSpecificTopic() {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish("custom-topic", event);

        // Then
        verify(kafkaTemplate).send(eq("custom-topic"), any(String.class), eq(event));
    }

    @Test
    void shouldPublishEventWithCustomKey() {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        String customKey = "order-1";
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish("order-events", customKey, event);

        // Then
        verify(kafkaTemplate).send(eq("order-events"), eq(customKey), eq(event));
    }

    @Test
    void shouldAutoSetEventIdIfNotPresent() {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        event.setEventId(null); // 清空 eventId
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish(event);

        // Then
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
    }

    @Test
    void shouldAutoSetTimestampIfNotPresent() {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        event.setTimestamp(null); // 清空 timestamp
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish(event);

        // Then
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    void shouldPublishBatchEvents() {
        // Given
        OrderCreatedEvent event1 = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        OrderCreatedEvent event2 = OrderCreatedEvent.create("order-2", 2L, 2L, 3, new BigDecimal("200.00"));
        List<DomainEvent> events = Arrays.asList(event1, event2);
        
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        CompletableFuture<List<SendResult<String, DomainEvent>>> result = eventPublisher.publishBatch(events);

        // Then
        assertThat(result).isNotNull();
        verify(kafkaTemplate, times(2)).send(any(), any(), any());
    }

    @Test
    void shouldRouteOrderEventsToOrderTopic() {
        // Given
        OrderCreatedEvent event = OrderCreatedEvent.create("order-1", 1L, 1L, 2, new BigDecimal("100.00"));
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("order-events");
    }

    @Test
    void shouldRoutePaymentEventsToPaymentTopic() {
        // Given
        PaymentSuccessEvent event = PaymentSuccessEvent.create(1L, 1L, 1L, new BigDecimal("100.00"), "ALIPAY");
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("payment-events");
    }

    @Test
    void shouldRouteInventoryEventsToInventoryTopic() {
        // Given
        InventoryDeductedEvent event = InventoryDeductedEvent.create("1", Arrays.asList());
        CompletableFuture<SendResult<String, DomainEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(future);

        // When
        eventPublisher.publish(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), any(), any());
        assertThat(topicCaptor.getValue()).isEqualTo("inventory-events");
    }
}
