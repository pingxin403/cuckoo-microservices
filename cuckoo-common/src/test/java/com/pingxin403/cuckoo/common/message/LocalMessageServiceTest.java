package com.pingxin403.cuckoo.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LocalMessageService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("本地消息服务测试")
class LocalMessageServiceTest {
    
    @Mock
    private LocalMessageRepository localMessageRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private LocalMessageService localMessageService;
    
    private OrderCreatedEvent testEvent;
    
    @BeforeEach
    void setUp() {
        testEvent = new OrderCreatedEvent();
        testEvent.setEventId("test-event-id");
        testEvent.setEventType("OrderCreatedEvent");
    }
    
    @Test
    @DisplayName("应该成功保存消息")
    void shouldSaveMessage() throws Exception {
        // Given
        String payload = "{\"eventId\":\"test-event-id\"}";
        when(objectMapper.writeValueAsString(testEvent)).thenReturn(payload);
        
        // When
        localMessageService.saveMessage(testEvent);
        
        // Then
        ArgumentCaptor<LocalMessage> messageCaptor = ArgumentCaptor.forClass(LocalMessage.class);
        verify(localMessageRepository).save(messageCaptor.capture());
        
        LocalMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getMessageId()).isEqualTo("test-event-id");
        assertThat(savedMessage.getEventType()).isEqualTo("OrderCreatedEvent");
        assertThat(savedMessage.getPayload()).isEqualTo(payload);
        assertThat(savedMessage.getStatus()).isEqualTo(MessageStatus.PENDING);
        assertThat(savedMessage.getRetryCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("应该标记消息为已发送")
    void shouldMarkMessageAsSent() {
        // Given
        String messageId = "test-message-id";
        LocalMessage message = LocalMessage.builder()
                .messageId(messageId)
                .status(MessageStatus.PENDING)
                .build();
        
        when(localMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        
        // When
        localMessageService.markAsSent(messageId);
        
        // Then
        verify(localMessageRepository).save(message);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(message.getSentAt()).isNotNull();
    }
    
    @Test
    @DisplayName("应该标记消息为失败")
    void shouldMarkMessageAsFailed() {
        // Given
        String messageId = "test-message-id";
        String errorMessage = "Kafka connection failed";
        LocalMessage message = LocalMessage.builder()
                .messageId(messageId)
                .status(MessageStatus.PENDING)
                .build();
        
        when(localMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        
        // When
        localMessageService.markAsFailed(messageId, errorMessage);
        
        // Then
        verify(localMessageRepository).save(message);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.FAILED);
        assertThat(message.getErrorMessage()).isEqualTo(errorMessage);
    }
    
    @Test
    @DisplayName("应该增加重试次数")
    void shouldIncrementRetryCount() {
        // Given
        String messageId = "test-message-id";
        LocalMessage message = LocalMessage.builder()
                .messageId(messageId)
                .retryCount(0)
                .build();
        
        when(localMessageRepository.findById(messageId)).thenReturn(Optional.of(message));
        
        // When
        localMessageService.incrementRetryCount(messageId);
        
        // Then
        verify(localMessageRepository).save(message);
        assertThat(message.getRetryCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("应该获取待发送的消息")
    void shouldGetPendingMessages() {
        // Given
        int limit = 10;
        List<LocalMessage> pendingMessages = List.of(
                LocalMessage.builder().messageId("msg1").status(MessageStatus.PENDING).build(),
                LocalMessage.builder().messageId("msg2").status(MessageStatus.PENDING).build()
        );
        
        when(localMessageRepository.findPendingMessages(MessageStatus.PENDING, limit))
                .thenReturn(pendingMessages);
        
        // When
        List<LocalMessage> result = localMessageService.getPendingMessages(limit);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMessageId()).isEqualTo("msg1");
        assertThat(result.get(1).getMessageId()).isEqualTo("msg2");
    }
    
    @Test
    @DisplayName("应该清理旧消息")
    void shouldCleanupOldMessages() {
        // Given
        int daysToKeep = 7;
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
        List<LocalMessage> oldMessages = List.of(
                LocalMessage.builder().messageId("old1").status(MessageStatus.SENT).build(),
                LocalMessage.builder().messageId("old2").status(MessageStatus.SENT).build()
        );
        
        when(localMessageRepository.findByStatusAndSentAtBefore(eq(MessageStatus.SENT), any(LocalDateTime.class)))
                .thenReturn(oldMessages);
        
        // When
        int deletedCount = localMessageService.cleanupOldMessages(daysToKeep);
        
        // Then
        assertThat(deletedCount).isEqualTo(2);
        verify(localMessageRepository).deleteAll(oldMessages);
    }
    
    @Test
    @DisplayName("应该获取消息统计信息")
    void shouldGetStats() {
        // Given
        when(localMessageRepository.countByStatus(MessageStatus.PENDING)).thenReturn(5L);
        when(localMessageRepository.countByStatus(MessageStatus.SENT)).thenReturn(100L);
        when(localMessageRepository.countByStatus(MessageStatus.FAILED)).thenReturn(2L);
        
        // When
        LocalMessageService.MessageStats stats = localMessageService.getStats();
        
        // Then
        assertThat(stats.pending()).isEqualTo(5L);
        assertThat(stats.sent()).isEqualTo(100L);
        assertThat(stats.failed()).isEqualTo(2L);
    }
}
