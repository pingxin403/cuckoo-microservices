package com.pingxin403.cuckoo.webbff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pingxin403.cuckoo.webbff.client.InventoryServiceClient;
import com.pingxin403.cuckoo.webbff.client.ProductServiceClient;
import com.pingxin403.cuckoo.webbff.client.ReviewServiceClient;
import com.pingxin403.cuckoo.webbff.dto.InventoryDTO;
import com.pingxin403.cuckoo.webbff.dto.ProductDetailDTO;
import com.pingxin403.cuckoo.webbff.dto.ReviewDTO;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration to provide mock beans for Web BFF tests
 * Mocks all Feign clients to avoid external service dependencies
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public ProductServiceClient productServiceClient() {
        ProductServiceClient client = mock(ProductServiceClient.class);
        
        // Configure default behavior: return a valid product for any productId
        when(client.getProductDetail(any(Long.class))).thenAnswer(invocation -> {
            Long productId = invocation.getArgument(0);
            ProductDetailDTO product = new ProductDetailDTO();
            product.setId(productId);
            product.setName("测试商品 " + productId);
            product.setPrice(new BigDecimal("99.99"));
            product.setDescription("这是一个测试商品的描述");
            product.setCategory("电子产品");
            product.setBrand("测试品牌");
            product.setImageUrl("https://example.com/product/" + productId + ".jpg");
            return product;
        });
        
        return client;
    }

    @Bean
    public InventoryServiceClient inventoryServiceClient() {
        InventoryServiceClient client = mock(InventoryServiceClient.class);
        
        // Configure default behavior: return inventory with stock
        when(client.getInventory(any(Long.class))).thenAnswer(invocation -> {
            Long productId = invocation.getArgument(0);
            InventoryDTO inventory = new InventoryDTO();
            inventory.setProductId(productId);
            inventory.setAvailableStock(100);
            inventory.setReservedStock(10);
            inventory.setStatus("AVAILABLE");
            return inventory;
        });
        
        return client;
    }

    @Bean
    public ReviewServiceClient reviewServiceClient() {
        ReviewServiceClient client = mock(ReviewServiceClient.class);
        
        // Configure default behavior: return a list of reviews
        when(client.getProductReviews(any(Long.class))).thenAnswer(invocation -> {
            Long productId = invocation.getArgument(0);
            
            return Arrays.asList(
                createMockReview(1L, productId, "用户A", 5, "非常好的商品！"),
                createMockReview(2L, productId, "用户B", 4, "质量不错，值得购买"),
                createMockReview(3L, productId, "用户C", 5, "超出预期，强烈推荐")
            );
        });
        
        return client;
    }

    /**
     * Helper method to create mock review
     */
    private ReviewDTO createMockReview(Long id, Long productId, String username, Integer rating, String content) {
        ReviewDTO review = new ReviewDTO();
        review.setId(id);
        review.setProductId(productId);
        review.setUsername(username);
        review.setRating(rating);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now().minusDays(id));
        return review;
    }
}
