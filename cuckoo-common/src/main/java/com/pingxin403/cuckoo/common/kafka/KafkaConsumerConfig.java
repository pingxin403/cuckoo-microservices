package com.pingxin403.cuckoo.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消费者配置
 * 提供统一的消费者配置，包括：
 * 1. 消费失败重试机制（最多 3 次）
 * 2. 死信队列支持
 * 3. 指数退避策略
 * 
 * Requirements: 1.6, 1.7
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * 配置死信队列的 KafkaTemplate
     * 用于将失败的消息发送到 dead-letter-queue
     */
    @Bean
    public KafkaTemplate<String, Object> deadLetterQueueTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * 配置错误处理器
     * 1. 最多重试 3 次
     * 2. 使用指数退避策略（初始 1 秒，倍数 2.0，最大 10 秒）
     * 3. 重试失败后发送到死信队列
     */
    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> deadLetterQueueTemplate) {
        // 配置指数退避策略
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);      // 初始间隔 1 秒
        backOff.setMultiplier(2.0);             // 每次重试间隔翻倍
        backOff.setMaxInterval(10000L);         // 最大间隔 10 秒

        // 配置死信队列发布器
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            deadLetterQueueTemplate,
            (record, ex) -> {
                // 所有失败的消息都发送到 dead-letter-queue
                log.error("消息处理失败，发送到死信队列: topic={}, partition={}, offset={}, key={}, error={}",
                    record.topic(), record.partition(), record.offset(), record.key(), ex.getMessage());
                return new org.apache.kafka.common.TopicPartition("dead-letter-queue", 0);
            }
        );

        // 创建错误处理器
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // 配置错误日志
        errorHandler.setLogLevel(org.springframework.kafka.KafkaException.Level.ERROR);
        
        // 添加重试监听器
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("消息处理重试: topic={}, partition={}, offset={}, attempt={}/{}, error={}",
                record.topic(), record.partition(), record.offset(), 
                deliveryAttempt, 3, ex.getMessage());
        });

        return errorHandler;
    }

    /**
     * 配置 Kafka 监听器容器工厂
     * 应用错误处理器到所有消费者
     * 
     * 注意：各服务需要在自己的配置中创建 ConsumerFactory 并使用此方法配置
     */
    public void configureContainerFactory(
        ConcurrentKafkaListenerContainerFactory<String, Object> factory,
        CommonErrorHandler errorHandler
    ) {
        factory.setCommonErrorHandler(errorHandler);
        
        // 配置并发数（可根据需要调整）
        factory.setConcurrency(3);
        
        log.info("Kafka 消费者配置完成: 最大重试次数=3, 死信队列=dead-letter-queue");
    }
}
