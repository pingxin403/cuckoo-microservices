package com.pingxin403.cuckoo.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.event.DomainEvent;
import com.pingxin403.cuckoo.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息重试调度器
 * 定时扫描本地消息表中的待发送消息，并重试发送到 Kafka。
 * 
 * 功能：
 * 1. 每 30 秒扫描一次 PENDING 状态的消息
 * 2. 重试发送失败的消息
 * 3. 超过 5 次重试后标记为 FAILED 并告警
 * 4. 定期清理 7 天前已发送的旧消息
 * 
 * Requirements: 1.5, 1.6
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(EventPublisher.class)
@ConditionalOnProperty(
    prefix = "cuckoo.message.retry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MessageRetryScheduler {
    
    private final LocalMessageService localMessageService;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 5;
    
    /**
     * 每次扫描的消息数量限制
     */
    private static final int BATCH_SIZE = 100;
    
    /**
     * 消息保留天数
     */
    private static final int MESSAGE_RETENTION_DAYS = 7;
    
    /**
     * 发送超时时间（秒）
     */
    private static final int SEND_TIMEOUT_SECONDS = 5;
    
    /**
     * 定时任务：重试待发送的消息
     * 每 30 秒执行一次
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void retryPendingMessages() {
        log.debug("开始扫描待发送消息...");
        
        try {
            List<LocalMessage> pendingMessages = localMessageService.getPendingMessages(BATCH_SIZE);
            
            if (pendingMessages.isEmpty()) {
                log.debug("没有待发送的消息");
                return;
            }
            
            log.info("发现 {} 条待发送消息，开始重试", pendingMessages.size());
            
            int successCount = 0;
            int failedCount = 0;
            int maxRetryExceededCount = 0;
            
            for (LocalMessage message : pendingMessages) {
                try {
                    // 检查重试次数
                    if (message.getRetryCount() >= MAX_RETRY_COUNT) {
                        handleMaxRetryExceeded(message);
                        maxRetryExceededCount++;
                        continue;
                    }
                    
                    // 反序列化事件
                    DomainEvent event = deserializeEvent(message);
                    
                    // 重试发送
                    log.debug("重试发送消息: messageId={}, eventType={}, retryCount={}", 
                             message.getMessageId(), message.getEventType(), message.getRetryCount());
                    
                    eventPublisher.publish(event)
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    
                    // 发送成功，更新状态
                    localMessageService.markAsSent(message.getMessageId());
                    successCount++;
                    
                    log.info("消息重试发送成功: messageId={}, eventType={}, retryCount={}", 
                            message.getMessageId(), message.getEventType(), message.getRetryCount());
                    
                } catch (Exception e) {
                    // 发送失败，增加重试次数
                    localMessageService.incrementRetryCount(message.getMessageId());
                    failedCount++;
                    
                    log.warn("消息重试发送失败: messageId={}, eventType={}, retryCount={}, error={}", 
                            message.getMessageId(), message.getEventType(), 
                            message.getRetryCount(), e.getMessage());
                }
            }
            
            log.info("消息重试完成: total={}, success={}, failed={}, maxRetryExceeded={}", 
                    pendingMessages.size(), successCount, failedCount, maxRetryExceededCount);
            
        } catch (Exception e) {
            log.error("消息重试调度器执行失败", e);
        }
    }
    
    /**
     * 定时任务：清理旧消息
     * 每天凌晨 2 点执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldMessages() {
        log.info("开始清理旧消息...");
        
        try {
            int deletedCount = localMessageService.cleanupOldMessages(MESSAGE_RETENTION_DAYS);
            log.info("旧消息清理完成: deletedCount={}, retentionDays={}", 
                    deletedCount, MESSAGE_RETENTION_DAYS);
        } catch (Exception e) {
            log.error("清理旧消息失败", e);
        }
    }
    
    /**
     * 处理超过最大重试次数的消息
     * 
     * @param message 消息
     */
    private void handleMaxRetryExceeded(LocalMessage message) {
        String errorMessage = String.format(
            "消息重试次数超过最大限制: maxRetryCount=%d", MAX_RETRY_COUNT);
        
        localMessageService.markAsFailed(message.getMessageId(), errorMessage);
        
        // 发送告警
        sendAlert(message, errorMessage);
        
        log.error("消息重试失败，已标记为 FAILED: messageId={}, eventType={}, retryCount={}", 
                 message.getMessageId(), message.getEventType(), message.getRetryCount());
    }
    
    /**
     * 发送告警通知
     * 
     * @param message 失败的消息
     * @param errorMessage 错误信息
     */
    private void sendAlert(LocalMessage message, String errorMessage) {
        // TODO: 集成告警系统（如钉钉、企业微信、邮件等）
        // 当前仅记录错误日志
        log.error("【告警】消息发送失败: messageId={}, eventType={}, retryCount={}, error={}, createdAt={}", 
                 message.getMessageId(), 
                 message.getEventType(), 
                 message.getRetryCount(), 
                 errorMessage,
                 message.getCreatedAt());
    }
    
    /**
     * 反序列化事件
     * 
     * @param message 本地消息
     * @return 领域事件
     * @throws Exception 反序列化失败
     */
    private DomainEvent deserializeEvent(LocalMessage message) throws Exception {
        try {
            // 使用 Jackson 的多态反序列化
            // DomainEvent 类上的 @JsonTypeInfo 注解会自动处理类型信息
            return objectMapper.readValue(message.getPayload(), DomainEvent.class);
        } catch (Exception e) {
            log.error("反序列化事件失败: messageId={}, eventType={}, payload={}", 
                     message.getMessageId(), message.getEventType(), message.getPayload(), e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
    
    /**
     * 获取消息统计信息（用于监控）
     * 
     * @return 消息统计
     */
    public LocalMessageService.MessageStats getMessageStats() {
        return localMessageService.getStats();
    }
}
