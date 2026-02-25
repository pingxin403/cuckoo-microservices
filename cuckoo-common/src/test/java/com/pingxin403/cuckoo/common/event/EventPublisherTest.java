package com.pingxin403.cuckoo.common.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EventPublisher 单元测试
 * 验证事件发布器的核心功能：
 * 1. 自动设置 eventId
 * 2. 自动设置 timestamp
 * 3. 正确发布事件到 Kafka
 * 4. 记录日志
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    private KafkaEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new KafkaEventPublisher(kafkaTemplate);
    }

    @Test
    void publish_withKey_shouldAutoSetEventIdWhenNull() {
        // Given: 创建一个没有 eventId 的事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventType("ORDER_CREATED");
        event.setTimestamp(System.currentTimeMillis());
        event.setVersion(1);
        event.setOrderId(1L);
        event.setUserId(100L);
        event.setSkuId(200L);
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When: 发布事件
        eventPublisher.publish("order-events", "order-1", event);

        // Then: eventId 应该被自动设置
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
        assertThat(event.getEventId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        // 验证 KafkaTemplate 被调用
        verify(kafkaTemplate).send(eq("order-events"), eq("order-1"), eq(event));
    }

    @Test
    void publish_withKey_shouldAutoSetTimestampWhenNull() {
        // Given: 创建一个没有 timestamp 的事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId("test-event-id");
        event.setEventType("ORDER_CREATED");
        event.setVersion(1);
        event.setOrderId(1L);
        event.setUserId(100L);
        event.setSkuId(200L);
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        long before = System.currentTimeMillis();

        // When: 发布事件
        eventPublisher.publish("order-events", "order-1", event);

        long after = System.currentTimeMillis();

        // Then: timestamp 应该被自动设置为当前时间
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBetween(before, after);

        // 验证 KafkaTemplate 被调用
        verify(kafkaTemplate).send(eq("order-events"), eq("order-1"), eq(event));
    }

    @Test
    void publish_withKey_shouldNotOverrideExistingEventId() {
        // Given: 创建一个已有 eventId 的事件
        String existingEventId = "existing-event-id";
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(existingEventId);
        event.setEventType("ORDER_CREATED");
        event.setTimestamp(System.currentTimeMillis());
        event.setVersion(1);
        event.setOrderId(1L);
        event.setUserId(100L);
        event.setSkuId(200L);
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When: 发布事件
        eventPublisher.publish("order-events", "order-1", event);

        // Then: eventId 不应该被覆盖
        assertThat(event.getEventId()).isEqualTo(existingEventId);

        // 验证 KafkaTemplate 被调用
        verify(kafkaTemplate).send(eq("order-events"), eq("order-1"), eq(event));
    }

    @Test
    void publish_withKey_shouldNotOverrideExistingTimestamp() {
        // Given: 创建一个已有 timestamp 的事件
        long existingTimestamp = 1704067200000L; // 2024-01-01T00:00:00Z
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId("test-event-id");
        event.setEventType("ORDER_CREATED");
        event.setTimestamp(existingTimestamp);
        event.setVersion(1);
        event.setOrderId(1L);
        event.setUserId(100L);
        event.setSkuId(200L);
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When: 发布事件
        eventPublisher.publish("order-events", "order-1", event);

        // Then: timestamp 不应该被覆盖
        assertThat(event.getTimestamp()).isEqualTo(existingTimestamp);

        // 验证 KafkaTemplate 被调用
        verify(kafkaTemplate).send(eq("order-events"), eq("order-1"), eq(event));
    }

    @Test
    void publish_withoutKey_shouldUseEventIdAsKey() {
        // Given: 创建一个事件
        OrderCreatedEvent event = OrderCreatedEvent.create(1L, 100L, 200L, 2, BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When: 发布事件（不指定 key）
        eventPublisher.publish("order-events", event);

        // Then: 应该使用 eventId 作为 key
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("order-events"), keyCaptor.capture(), eq(event));
        assertThat(keyCaptor.getValue()).isEqualTo(event.getEventId());
    }

    @Test
    void publish_withoutKey_shouldAutoSetEventIdWhenNull() {
        // Given: 创建一个没有 eventId 的事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventType("ORDER_CREATED");
        event.setTimestamp(System.currentTimeMillis());
        event.setVersion(1);
        event.setOrderId(1L);
        event.setUserId(100L);
        event.setSkuId(200L);
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When: 发布事件（不指定 key）
        eventPublisher.publish("order-events", event);

        // Then: eventId 应该被自动设置，并用作 key
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("order-events"), keyCaptor.capture(), eq(event));
        assertThat(keyCaptor.getValue()).isEqualTo(event.getEventId());
    }

    @Test
    void publish_shouldHandleKafkaException() {
        // Given: KafkaTemplate 抛出异常
        OrderCreatedEvent event = OrderCreatedEvent.create(1L, 100L, 200L, 2, BigDecimal.valueOf(100));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

        // When: 发布事件
        // Then: 不应该抛出异常（异常被捕获并记录日志）
        eventPublisher.publish("order-events", "order-1", event);

        // 验证 KafkaTemplate 被调用
        verify(kafkaTemplate).send(eq("order-events"), eq("order-1"), eq(event));
    }

    @Test
    void publish_shouldSetBothEventIdAndTimestampWhenBothNull() {
        // Given: 创建一个没有 eventId 和 timestamp 的事件
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventType("ORDER_CREATED");
        event.setVersion(1);
        event.setOrderId(1L);
        event.setUserId(100L);
        event.setSkuId(200L);
        event.setQuantity(2);
        event.setTotalAmount(BigDecimal.valueOf(100));
        
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        long before = System.currentTimeMillis();

        // When: 发布事件
        eventPublisher.publish("order-events", "order-1", event);

        long after = System.currentTimeMillis();

        // Then: eventId 和 timestamp 都应该被自动设置
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBetween(before, after);

        // 验证 KafkaTemplate 被调用
        verify(kafkaTemplate).send(eq("order-events"), eq("order-1"), eq(event));
    }
}
