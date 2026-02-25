package com.pingxin403.cuckoo.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 事件发布器配置
 * 启用 Spring Retry 支持事件发布失败重试
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@Configuration
@EnableRetry
public class EventPublisherConfig {
    // Spring Retry 配置通过 @Retryable 注解在 KafkaEventPublisher 中定义
}
