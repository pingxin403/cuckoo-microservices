package com.pingxin403.cuckoo.product.config;

import com.pingxin403.cuckoo.common.cache.BloomFilterService;
import com.pingxin403.cuckoo.common.cache.MultiLevelCacheManager;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import com.pingxin403.cuckoo.common.message.LocalMessageRepository;
import com.pingxin403.cuckoo.common.audit.AuditLogRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide mock beans for dependencies from common module
 * that are not needed for Product Service tests
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
    public MultiLevelCacheManager multiLevelCacheManager() {
        MultiLevelCacheManager cacheManager = mock(MultiLevelCacheManager.class);
        // Mock cache to always return null (cache miss)
        when(cacheManager.get(anyString(), any())).thenReturn(null);
        return cacheManager;
    }

    @Bean
    @Primary
    public BloomFilterService bloomFilterService() {
        BloomFilterService bloomFilterService = mock(BloomFilterService.class);
        // Mock bloom filter to always return false (item doesn't exist) by default
        // This prevents false positives in tests and allows cache penetration scenarios to be tested
        when(bloomFilterService.mightContain(anyString())).thenReturn(false);
        return bloomFilterService;
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        
        try {
            // Mock lock to always succeed
            when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(redissonClient.getLock(anyString())).thenReturn(lock);
        } catch (InterruptedException e) {
            // This won't happen in mock setup
        }
        
        return redissonClient;
    }

    @Bean
    @Primary
    public LocalMessageRepository localMessageRepository() {
        return mock(LocalMessageRepository.class);
    }
}
