package com.pingxin403.cuckoo.order.config;

import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.idempotency.IdempotencyService;
import com.pingxin403.cuckoo.common.idempotency.ProcessedEventRepository;
import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.client.ProductClient;
import com.pingxin403.cuckoo.order.dto.PaymentDTO;
import com.pingxin403.cuckoo.order.dto.ProductDTO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration providing mock beans for all external dependencies
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
    public EventPublisher eventPublisher() {
        return mock(EventPublisher.class);
    }

    @Bean
    public ProductClient productClient() {
        ProductClient client = mock(ProductClient.class);
        ProductDTO product = new ProductDTO();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(100.00));
        when(client.getProduct(anyLong())).thenReturn(product);
        return client;
    }

    @Bean
    public InventoryClient inventoryClient() {
        return mock(InventoryClient.class);
    }

    @Bean
    public PaymentClient paymentClient() {
        PaymentClient client = mock(PaymentClient.class);
        PaymentDTO payment = new PaymentDTO();
        payment.setId(1L);
        payment.setStatus("PENDING");
        when(client.createPayment(any())).thenReturn(payment);
        return client;
    }
}
