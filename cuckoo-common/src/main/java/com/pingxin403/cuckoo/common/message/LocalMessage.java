package com.pingxin403.cuckoo.common.message;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 本地消息表实体
 * 用于保证事件发布的可靠性，避免消息丢失
 */
@Entity
@Table(name = "local_message", indexes = {
    @Index(name = "idx_status_created", columnList = "status,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalMessage {
    
    /**
     * 消息ID（UUID）
     */
    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;
    
    /**
     * 事件类型
     */
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;
    
    /**
     * 消息内容（JSON格式）
     */
    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;
    
    /**
     * 消息状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MessageStatus status;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 发送成功时间
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
