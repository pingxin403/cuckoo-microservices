package com.pingxin403.cuckoo.inventory.config;

import com.pingxin403.cuckoo.common.audit.AuditLogRepository;
import com.pingxin403.cuckoo.common.audit.AuditLogService;
import com.pingxin403.cuckoo.common.event.DomainEvent;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import com.pingxin403.cuckoo.common.message.LocalMessageRepository;
import com.pingxin403.cuckoo.inventory.config.InventoryConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide mock beans for dependencies from common module
 * that are not needed for Inventory Service tests
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ProcessedEventRepository processedEventRepository() {
        return mock(ProcessedEventRepository.class);
    }

    @Bean
    @Primary
    public IdempotencyService idempotencyService(ProcessedEventRepository repository) {
        return new IdempotencyService(repository);
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.isSubscribed()).thenReturn(false);
        return factory;
    }

    @Bean
    @Primary
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        // Create a container but don't start it
        RedisMessageListenerContainer container = mock(RedisMessageListenerContainer.class);
        return container;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        
        // Mock RedisTemplate behavior to avoid actual Redis connection
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // Always cache miss
        
        return redisTemplate;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = 
            mock(org.springframework.data.redis.core.ValueOperations.class);
        
        // Mock lock acquisition to always succeed
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class)))
            .thenReturn(true);
        when(valueOps.get(anyString())).thenReturn(null);
        
        return template;
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
        KafkaTemplate<String, DomainEvent> kafkaTemplate = mock(KafkaTemplate.class);
        
        // Mock KafkaTemplate behavior to avoid actual Kafka connection
        SendResult<String, DomainEvent> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, DomainEvent>> future = 
            CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(anyString(), anyString(), any(DomainEvent.class)))
            .thenReturn(future);
        
        return kafkaTemplate;
    }

    @Bean
    @Primary
    public AuditLogRepository auditLogRepository() {
        return mock(AuditLogRepository.class);
    }

    @Bean
    @Primary
    public AuditLogService auditLogService() {
        return mock(AuditLogService.class);
    }

    @Bean
    @Primary
    public LocalMessageRepository localMessageRepository() {
        return mock(LocalMessageRepository.class);
    }

    @Bean
    @Primary
    public InventoryConfig inventoryConfig() {
        InventoryConfig config = mock(InventoryConfig.class);
        when(config.getLockTimeout()).thenReturn(5);
        return config;
    }
}
