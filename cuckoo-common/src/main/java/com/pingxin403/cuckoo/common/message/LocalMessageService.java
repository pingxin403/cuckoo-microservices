package com.pingxin403.cuckoo.common.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表服务
 * 提供消息的保存、更新和查询功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LocalMessageService {
    
    private final LocalMessageRepository localMessageRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 保存消息（与业务操作在同一事务中）
     * 
     * @param event 领域事件
     */
    @Transactional
    public void saveMessage(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            LocalMessage message = LocalMessage.builder()
                    .messageId(event.getEventId())
                    .eventType(event.getEventType())
                    .payload(payload)
                    .status(MessageStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            localMessageRepository.save(message);
            log.debug("Saved local message: messageId={}, eventType={}", 
                     message.getMessageId(), message.getEventType());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON: eventId={}", event.getEventId(), e);
            throw new RuntimeException("Failed to save local message", e);
        }
    }
    
    /**
     * 标记消息为已发送
     * 
     * @param messageId 消息ID
     */
    @Transactional
    public void markAsSent(String messageId) {
        localMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            localMessageRepository.save(message);
            log.debug("Marked message as sent: messageId={}", messageId);
        });
    }
    
    /**
     * 标记消息为失败
     * 
     * @param messageId 消息ID
     * @param errorMessage 错误信息
     */
    @Transactional
    public void markAsFailed(String messageId, String errorMessage) {
        localMessageRepository.findById(messageId).ifPresent(message -> {
            message.setStatus(MessageStatus.FAILED);
            message.setErrorMessage(errorMessage);
            localMessageRepository.save(message);
            log.warn("Marked message as failed: messageId={}, error={}", messageId, errorMessage);
        });
    }
    
    /**
     * 增加消息重试次数
     * 
     * @param messageId 消息ID
     */
    @Transactional
    public void incrementRetryCount(String messageId) {
        localMessageRepository.findById(messageId).ifPresent(message -> {
            message.setRetryCount(message.getRetryCount() + 1);
            localMessageRepository.save(message);
            log.debug("Incremented retry count: messageId={}, retryCount={}", 
                     messageId, message.getRetryCount());
        });
    }
    
    /**
     * 获取待发送的消息
     * 
     * @param limit 限制数量
     * @return 待发送的消息列表
     */
    @Transactional(readOnly = true)
    public List<LocalMessage> getPendingMessages(int limit) {
        return localMessageRepository.findPendingMessages(MessageStatus.PENDING, limit);
    }
    
    /**
     * 清理已发送的旧消息
     * 
     * @param daysToKeep 保留天数
     * @return 删除的消息数量
     */
    @Transactional
    public int cleanupOldMessages(int daysToKeep) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
        List<LocalMessage> oldMessages = localMessageRepository
                .findByStatusAndSentAtBefore(MessageStatus.SENT, threshold);
        
        if (!oldMessages.isEmpty()) {
            localMessageRepository.deleteAll(oldMessages);
            log.info("Cleaned up {} old messages sent before {}", oldMessages.size(), threshold);
        }
        
        return oldMessages.size();
    }
    
    /**
     * 获取消息统计信息
     * 
     * @return 消息统计
     */
    @Transactional(readOnly = true)
    public MessageStats getStats() {
        long pendingCount = localMessageRepository.countByStatus(MessageStatus.PENDING);
        long sentCount = localMessageRepository.countByStatus(MessageStatus.SENT);
        long failedCount = localMessageRepository.countByStatus(MessageStatus.FAILED);
        
        return new MessageStats(pendingCount, sentCount, failedCount);
    }
    
    /**
     * 消息统计信息
     */
    public record MessageStats(long pending, long sent, long failed) {}
}
