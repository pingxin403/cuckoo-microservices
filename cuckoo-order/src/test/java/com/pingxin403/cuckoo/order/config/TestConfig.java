package com.pingxin403.cuckoo.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pingxin403.cuckoo.common.audit.AuditLogRepository;
import com.pingxin403.cuckoo.common.audit.AuditLogService;
import com.pingxin403.cuckoo.common.event.DomainEvent;
import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import com.pingxin403.cuckoo.common.message.LocalMessageRepository;
import com.pingxin403.cuckoo.common.message.LocalMessageService;
import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.client.ProductClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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
 * that are not needed for Order Service tests
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
        AuditLogService service = mock(AuditLogService.class);
        // Configure default behavior: return a builder that can be chained
        when(service.buildAuditLog(any(), any(), anyString())).thenAnswer(invocation -> {
            return com.pingxin403.cuckoo.common.audit.AuditLog.builder()
                    .operationType((com.pingxin403.cuckoo.common.audit.AuditLog.OperationType) invocation.getArgument(0))
                    .userId((Long) invocation.getArgument(1))
                    .username(invocation.getArgument(2));
        });
        return service;
    }

    @Bean
    @Primary
    public LocalMessageRepository localMessageRepository() {
        return mock(LocalMessageRepository.class);
    }

    @Bean
    @Primary
    public LocalMessageService localMessageService() {
        return mock(LocalMessageService.class);
    }

    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return mock(EventPublisher.class);
    }

    @Bean
    @Primary
    public ProductClient productClient() {
        ProductClient client = mock(ProductClient.class);
        // Configure default behavior: return a valid product for any skuId
        when(client.getProduct(any(Long.class))).thenAnswer(invocation -> {
            Long skuId = invocation.getArgument(0);
            return com.pingxin403.cuckoo.order.dto.ProductDTO.builder()
                    .id(skuId)
                    .name("Test Product " + skuId)
                    .price(new java.math.BigDecimal("100.00"))
                    .description("Test Description")
                    .build();
        });
        return client;
    }

    @Bean
    @Primary
    public PaymentClient paymentClient() {
        PaymentClient client = mock(PaymentClient.class);
        // Configure default behavior: return a valid payment for any request
        when(client.createPayment(any())).thenAnswer(invocation -> {
            com.pingxin403.cuckoo.order.dto.CreatePaymentRequest request = invocation.getArgument(0);
            return com.pingxin403.cuckoo.order.dto.PaymentDTO.builder()
                    .id(1L)
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .status("PENDING")
                    .build();
        });
        return client;
    }

    @Bean
    @Primary
    public InventoryClient inventoryClient() {
        InventoryClient client = mock(InventoryClient.class);
        // Configure default behavior: do nothing (successful reservation)
        return client;
    }
}
