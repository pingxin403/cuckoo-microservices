package com.pingxin403.cuckoo.product.config;

import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

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
}
