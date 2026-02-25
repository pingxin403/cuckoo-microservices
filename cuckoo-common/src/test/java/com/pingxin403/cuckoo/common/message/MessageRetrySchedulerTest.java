package com.pingxin403.cuckoo.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pingxin403.cuckoo.common.event.DomainEvent;
import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * MessageRetryScheduler 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息重试调度器测试")
class MessageRetrySchedulerTest {
    
    @Mock
    private LocalMessageService localMessageService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private MessageRetryScheduler scheduler;
    
    private LocalMessage pendingMessage;
    private OrderCreatedEvent testEvent;
    
    @BeforeEach
    void setUp() throws Exception {
        // 创建测试事件
        testEvent = new OrderCreatedEvent();
        testEvent.setEventId("test-event-id");
        testEvent.setEventType("ORDER_CREATED");
        testEvent.setTimestamp(System.currentTimeMillis());
        testEvent.setVersion(1);
        
        // 创建待发送消息
        pendingMessage = LocalMessage.builder()
                .messageId("test-message-id")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"test-event-id\"}")
                .status(MessageStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("应该成功重试发送待发送的消息")
    void shouldRetryPendingMessagesSuccessfully() throws Exception {
        // Given
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Collections.singletonList(pendingMessage));
        
        when(objectMapper.readValue(anyString(), eq(DomainEvent.class)))
                .thenReturn(testEvent);
        
        CompletableFuture<SendResult<String, DomainEvent>> future = 
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(future);
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(localMessageService).getPendingMessages(100);
        verify(eventPublisher).publish(any(DomainEvent.class));
        verify(localMessageService).markAsSent("test-message-id");
        verify(localMessageService, never()).incrementRetryCount(anyString());
    }
    
    @Test
    @DisplayName("当发送失败时应该增加重试次数")
    void shouldIncrementRetryCountWhenSendFails() throws Exception {
        // Given
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Collections.singletonList(pendingMessage));
        
        when(objectMapper.readValue(anyString(), eq(DomainEvent.class)))
                .thenReturn(testEvent);
        
        CompletableFuture<SendResult<String, DomainEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(future);
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(localMessageService).incrementRetryCount("test-message-id");
        verify(localMessageService, never()).markAsSent(anyString());
    }
    
    @Test
    @DisplayName("当重试次数超过5次时应该标记为失败并告警")
    void shouldMarkAsFailedWhenMaxRetryExceeded() {
        // Given
        LocalMessage maxRetriedMessage = LocalMessage.builder()
                .messageId("max-retry-message")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"test-event-id\"}")
                .status(MessageStatus.PENDING)
                .retryCount(5)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Collections.singletonList(maxRetriedMessage));
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(localMessageService).markAsFailed(
                eq("max-retry-message"), 
                errorMessageCaptor.capture()
        );
        
        assertThat(errorMessageCaptor.getValue())
                .contains("消息重试次数超过最大限制")
                .contains("maxRetryCount=5");
        
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
    
    @Test
    @DisplayName("应该批量处理多条待发送消息")
    void shouldProcessMultiplePendingMessages() throws Exception {
        // Given
        LocalMessage message1 = LocalMessage.builder()
                .messageId("message-1")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"event-1\"}")
                .status(MessageStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        
        LocalMessage message2 = LocalMessage.builder()
                .messageId("message-2")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"event-2\"}")
                .status(MessageStatus.PENDING)
                .retryCount(1)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Arrays.asList(message1, message2));
        
        when(objectMapper.readValue(anyString(), eq(DomainEvent.class)))
                .thenReturn(testEvent);
        
        CompletableFuture<SendResult<String, DomainEvent>> future = 
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(future);
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(eventPublisher, times(2)).publish(any(DomainEvent.class));
        verify(localMessageService).markAsSent("message-1");
        verify(localMessageService).markAsSent("message-2");
    }
    
    @Test
    @DisplayName("当没有待发送消息时应该直接返回")
    void shouldReturnWhenNoPendingMessages() {
        // Given
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Collections.emptyList());
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(localMessageService).getPendingMessages(100);
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
        verify(localMessageService, never()).markAsSent(anyString());
    }
    
    @Test
    @DisplayName("应该清理7天前的旧消息")
    void shouldCleanupOldMessages() {
        // Given
        when(localMessageService.cleanupOldMessages(7))
                .thenReturn(10);
        
        // When
        scheduler.cleanupOldMessages();
        
        // Then
        verify(localMessageService).cleanupOldMessages(7);
    }
    
    @Test
    @DisplayName("当反序列化失败时应该增加重试次数")
    void shouldIncrementRetryCountWhenDeserializationFails() throws Exception {
        // Given
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Collections.singletonList(pendingMessage));
        
        when(objectMapper.readValue(anyString(), eq(DomainEvent.class)))
                .thenThrow(new RuntimeException("Deserialization failed"));
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(localMessageService).incrementRetryCount("test-message-id");
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }
    
    @Test
    @DisplayName("应该获取消息统计信息")
    void shouldGetMessageStats() {
        // Given
        LocalMessageService.MessageStats stats = 
                new LocalMessageService.MessageStats(5, 100, 2);
        when(localMessageService.getStats()).thenReturn(stats);
        
        // When
        LocalMessageService.MessageStats result = scheduler.getMessageStats();
        
        // Then
        assertThat(result.pending()).isEqualTo(5);
        assertThat(result.sent()).isEqualTo(100);
        assertThat(result.failed()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("当发送超时时应该增加重试次数")
    void shouldIncrementRetryCountWhenSendTimeout() throws Exception {
        // Given
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Collections.singletonList(pendingMessage));
        
        when(objectMapper.readValue(anyString(), eq(DomainEvent.class)))
                .thenReturn(testEvent);
        
        // 创建一个永远不会完成的 Future 来模拟超时
        CompletableFuture<SendResult<String, DomainEvent>> future = new CompletableFuture<>();
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(future);
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(localMessageService).incrementRetryCount("test-message-id");
        verify(localMessageService, never()).markAsSent(anyString());
    }
    
    @Test
    @DisplayName("应该处理混合成功和失败的消息")
    void shouldHandleMixedSuccessAndFailure() throws Exception {
        // Given
        LocalMessage successMessage = LocalMessage.builder()
                .messageId("success-message")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"success-event\"}")
                .status(MessageStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        
        LocalMessage failMessage = LocalMessage.builder()
                .messageId("fail-message")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"fail-event\"}")
                .status(MessageStatus.PENDING)
                .retryCount(1)
                .createdAt(LocalDateTime.now())
                .build();
        
        LocalMessage maxRetryMessage = LocalMessage.builder()
                .messageId("max-retry-message")
                .eventType("ORDER_CREATED")
                .payload("{\"eventId\":\"max-retry-event\"}")
                .status(MessageStatus.PENDING)
                .retryCount(5)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(localMessageService.getPendingMessages(100))
                .thenReturn(Arrays.asList(successMessage, failMessage, maxRetryMessage));
        
        when(objectMapper.readValue(anyString(), eq(DomainEvent.class)))
                .thenReturn(testEvent);
        
        // 第一个消息成功
        CompletableFuture<SendResult<String, DomainEvent>> successFuture = 
                CompletableFuture.completedFuture(mock(SendResult.class));
        
        // 第二个消息失败
        CompletableFuture<SendResult<String, DomainEvent>> failFuture = new CompletableFuture<>();
        failFuture.completeExceptionally(new RuntimeException("Send failed"));
        
        when(eventPublisher.publish(any(DomainEvent.class)))
                .thenReturn(successFuture)
                .thenReturn(failFuture);
        
        // When
        scheduler.retryPendingMessages();
        
        // Then
        verify(localMessageService).markAsSent("success-message");
        verify(localMessageService).incrementRetryCount("fail-message");
        verify(localMessageService).markAsFailed(eq("max-retry-message"), anyString());
    }
}
