package com.pingxin403.cuckoo.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pingxin403.cuckoo.common.audit.AuditLogRepository;
import com.pingxin403.cuckoo.common.audit.AuditLogService;
import com.pingxin403.cuckoo.common.event.DomainEvent;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import com.pingxin403.cuckoo.common.message.LocalMessageRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide mock beans for dependencies from common module
 * that are not needed for Payment Service tests.
 * 
 * Note: Payment service does not use Redis, so Redis-related mocks are not included.
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

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
}
