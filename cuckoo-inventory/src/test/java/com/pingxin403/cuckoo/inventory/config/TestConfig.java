package com.pingxin403.cuckoo.inventory.config;

import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
    public ProcessedEventRepository processedEventRepository() {
        return mock(ProcessedEventRepository.class);
    }

    @Bean
    public IdempotencyService idempotencyService(ProcessedEventRepository repository) {
        return new IdempotencyService(repository);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        
        // Mock Redis operations for distributed lock
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(valueOps.get(anyString())).thenReturn(null);
        
        return template;
    }
}
