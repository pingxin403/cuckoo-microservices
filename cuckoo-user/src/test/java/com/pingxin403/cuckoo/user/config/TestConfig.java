package com.pingxin403.cuckoo.user.config;

import com.pingxin403.cuckoo.common.audit.AuditLogRepository;
import com.pingxin403.cuckoo.common.audit.AuditLogService;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import com.pingxin403.cuckoo.common.message.LocalMessageRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide mock beans for dependencies from common module
 * that are not needed for User Service tests.
 * 
 * This configuration is necessary for multi-module builds where cuckoo-common's
 * conditional beans (like KafkaEventPublisher) are loaded because their conditional
 * classes are on the classpath, but the actual beans they depend on are not configured.
 */
@TestConfiguration
@ComponentScan(
    basePackages = {"com.pingxin403.cuckoo.user", "com.pingxin403.cuckoo.common"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.pingxin403.cuckoo.common.cache.CacheConfig.class}
    )
)
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

    /**
     * Mock KafkaTemplate to satisfy KafkaEventPublisher's dependency.
     * This is needed in multi-module builds where KafkaTemplate class is on the classpath,
     * causing @ConditionalOnClass to pass, but no actual KafkaTemplate bean is configured.
     * 
     * We use Object type and cast to avoid compile-time dependency on Kafka classes.
     */
    @Bean(name = "kafkaTemplate")
    @Primary
    @SuppressWarnings("unchecked")
    public Object kafkaTemplate() {
        // Create a mock without importing KafkaTemplate class
        // This works because the bean name matches what KafkaEventPublisher expects
        try {
            Class<?> kafkaTemplateClass = Class.forName("org.springframework.kafka.core.KafkaTemplate");
            return mock(kafkaTemplateClass);
        } catch (ClassNotFoundException e) {
            // If Kafka is not on classpath, return null (won't be used anyway)
            return null;
        }
    }
}
