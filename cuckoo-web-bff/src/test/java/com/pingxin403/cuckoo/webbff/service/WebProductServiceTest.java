package com.pingxin403.cuckoo.webbff.service;

import com.pingxin403.cuckoo.webbff.client.InventoryServiceClient;
import com.pingxin403.cuckoo.webbff.client.ProductServiceClient;
import com.pingxin403.cuckoo.webbff.client.ReviewServiceClient;
import com.pingxin403.cuckoo.webbff.config.TestConfig;
import com.pingxin403.cuckoo.webbff.dto.InventoryDTO;
import com.pingxin403.cuckoo.webbff.dto.ProductDetailDTO;
import com.pingxin403.cuckoo.webbff.dto.ProductPageResponse;
import com.pingxin403.cuckoo.webbff.dto.ReviewDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for WebProductService
 * Tests product page data aggregation and fallback logic
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class WebProductServiceTest {

    @Autowired
    private WebProductService webProductService;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @MockBean
    private ReviewServiceClient reviewServiceClient;

    // Mock JPA-dependent beans from cuckoo-common to avoid loading them
    @MockBean
    private com.pingxin403.cuckoo.common.idempotency.IdempotencyService idempotencyService;

    @MockBean
    private com.pingxin403.cuckoo.common.audit.AuditLogService auditLogService;

    @MockBean
    private com.pingxin403.cuckoo.common.message.LocalMessageService localMessageService;

    @MockBean
    private com.pingxin403.cuckoo.common.message.MessageRetryScheduler messageRetryScheduler;

    @Test
    void testAggregateProductPage_Success() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 1L;
        
        ProductDetailDTO mockProduct = new ProductDetailDTO();
        mockProduct.setId(productId);
        mockProduct.setName("测试商品");
        mockProduct.setPrice(new BigDecimal("99.99"));
        mockProduct.setDescription("测试商品描述");
        mockProduct.setCategory("电子产品");
        
        InventoryDTO mockInventory = new InventoryDTO();
        mockInventory.setProductId(productId);
        mockInventory.setAvailableStock(100);
        mockInventory.setStatus("AVAILABLE");
        
        ReviewDTO mockReview = new ReviewDTO();
        mockReview.setId(1L);
        mockReview.setProductId(productId);
        mockReview.setUsername("用户A");
        mockReview.setRating(5);
        mockReview.setContent("非常好的商品！");
        mockReview.setCreatedAt(LocalDateTime.now());
        
        when(productServiceClient.getProductDetail(productId)).thenReturn(mockProduct);
        when(inventoryServiceClient.getInventory(productId)).thenReturn(mockInventory);
        when(reviewServiceClient.getProductReviews(productId)).thenReturn(Arrays.asList(mockReview));

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProduct()).isNotNull();
        assertThat(response.getProduct().getId()).isEqualTo(productId);
        assertThat(response.getProduct().getName()).isEqualTo("测试商品");
        assertThat(response.getProduct().getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        
        assertThat(response.getInventory()).isNotNull();
        assertThat(response.getInventory().getAvailableStock()).isEqualTo(100);
        assertThat(response.getInventory().getStatus()).isEqualTo("AVAILABLE");
        
        assertThat(response.getReviews()).isNotEmpty();
        assertThat(response.getReviews()).hasSize(1);
        assertThat(response.getTotalReviews()).isEqualTo(1);
        assertThat(response.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    void testAggregateProductPage_WithMultipleReviews() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 2L;
        
        ProductDetailDTO mockProduct = new ProductDetailDTO();
        mockProduct.setId(productId);
        mockProduct.setName("测试商品2");
        
        InventoryDTO mockInventory = new InventoryDTO();
        mockInventory.setProductId(productId);
        mockInventory.setAvailableStock(50);
        
        List<ReviewDTO> mockReviews = Arrays.asList(
            createReview(1L, productId, 5),
            createReview(2L, productId, 4),
            createReview(3L, productId, 5),
            createReview(4L, productId, 3)
        );
        
        when(productServiceClient.getProductDetail(productId)).thenReturn(mockProduct);
        when(inventoryServiceClient.getInventory(productId)).thenReturn(mockInventory);
        when(reviewServiceClient.getProductReviews(productId)).thenReturn(mockReviews);

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then
        assertThat(response.getReviews()).hasSize(4);
        assertThat(response.getTotalReviews()).isEqualTo(4);
        // Average: (5 + 4 + 5 + 3) / 4 = 4.25
        assertThat(response.getAverageRating()).isEqualTo(4.25);
    }

    @Test
    void testAggregateProductPage_WithNoReviews() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 3L;
        
        ProductDetailDTO mockProduct = new ProductDetailDTO();
        mockProduct.setId(productId);
        mockProduct.setName("测试商品3");
        
        InventoryDTO mockInventory = new InventoryDTO();
        mockInventory.setProductId(productId);
        mockInventory.setAvailableStock(0);
        mockInventory.setStatus("OUT_OF_STOCK");
        
        when(productServiceClient.getProductDetail(productId)).thenReturn(mockProduct);
        when(inventoryServiceClient.getInventory(productId)).thenReturn(mockInventory);
        when(reviewServiceClient.getProductReviews(productId)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then
        assertThat(response.getReviews()).isEmpty();
        assertThat(response.getTotalReviews()).isEqualTo(0);
        assertThat(response.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void testBuildDegradedResponse() {
        // Given
        Long productId = 4L;

        // When
        ProductPageResponse response = webProductService.buildDegradedResponse(productId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProduct()).isNotNull();
        assertThat(response.getProduct().getId()).isEqualTo(productId);
        assertThat(response.getProduct().getName()).isEqualTo("商品暂时无法加载");
        assertThat(response.getProduct().getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        
        assertThat(response.getInventory()).isNotNull();
        assertThat(response.getInventory().getAvailableStock()).isEqualTo(0);
        assertThat(response.getInventory().getStatus()).isEqualTo("UNAVAILABLE");
        
        assertThat(response.getReviews()).isEmpty();
        assertThat(response.getTotalReviews()).isEqualTo(0);
        assertThat(response.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void testAggregateProductPage_ProductServiceFails_UsesFallback() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 5L;
        
        InventoryDTO mockInventory = new InventoryDTO();
        mockInventory.setProductId(productId);
        mockInventory.setAvailableStock(100);
        
        when(productServiceClient.getProductDetail(productId)).thenThrow(new RuntimeException("Product service unavailable"));
        when(inventoryServiceClient.getInventory(productId)).thenReturn(mockInventory);
        when(reviewServiceClient.getProductReviews(productId)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then - Should use fallback product
        assertThat(response).isNotNull();
        assertThat(response.getProduct()).isNotNull();
        assertThat(response.getProduct().getId()).isEqualTo(productId);
        assertThat(response.getProduct().getName()).isEqualTo("商品暂时无法加载");
        assertThat(response.getProduct().getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // Other services should still work
        assertThat(response.getInventory().getAvailableStock()).isEqualTo(100);
    }

    @Test
    void testAggregateProductPage_InventoryServiceFails_UsesFallback() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 6L;
        
        ProductDetailDTO mockProduct = new ProductDetailDTO();
        mockProduct.setId(productId);
        mockProduct.setName("测试商品6");
        
        when(productServiceClient.getProductDetail(productId)).thenReturn(mockProduct);
        when(inventoryServiceClient.getInventory(productId)).thenThrow(new RuntimeException("Inventory service unavailable"));
        when(reviewServiceClient.getProductReviews(productId)).thenReturn(Collections.emptyList());

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then - Should use fallback inventory
        assertThat(response).isNotNull();
        assertThat(response.getProduct()).isNotNull();
        assertThat(response.getInventory()).isNotNull();
        assertThat(response.getInventory().getAvailableStock()).isEqualTo(0);
        assertThat(response.getInventory().getStatus()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void testAggregateProductPage_ReviewServiceFails_ReturnsEmptyList() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 7L;
        
        ProductDetailDTO mockProduct = new ProductDetailDTO();
        mockProduct.setId(productId);
        mockProduct.setName("测试商品7");
        
        InventoryDTO mockInventory = new InventoryDTO();
        mockInventory.setProductId(productId);
        mockInventory.setAvailableStock(100);
        
        when(productServiceClient.getProductDetail(productId)).thenReturn(mockProduct);
        when(inventoryServiceClient.getInventory(productId)).thenReturn(mockInventory);
        when(reviewServiceClient.getProductReviews(productId)).thenThrow(new RuntimeException("Review service unavailable"));

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then - Should return empty review list
        assertThat(response).isNotNull();
        assertThat(response.getProduct()).isNotNull();
        assertThat(response.getReviews()).isEmpty();
        assertThat(response.getTotalReviews()).isEqualTo(0);
        assertThat(response.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void testAggregateProductPage_AllServicesFail_UsesFallbackForAll() throws ExecutionException, InterruptedException {
        // Given
        Long productId = 8L;
        
        when(productServiceClient.getProductDetail(productId)).thenThrow(new RuntimeException("Product service unavailable"));
        when(inventoryServiceClient.getInventory(productId)).thenThrow(new RuntimeException("Inventory service unavailable"));
        when(reviewServiceClient.getProductReviews(productId)).thenThrow(new RuntimeException("Review service unavailable"));

        // When
        CompletableFuture<ProductPageResponse> future = webProductService.aggregateProductPage(productId);
        ProductPageResponse response = future.get();

        // Then - Should use fallback for all services
        assertThat(response).isNotNull();
        assertThat(response.getProduct()).isNotNull();
        assertThat(response.getProduct().getId()).isEqualTo(productId);
        assertThat(response.getProduct().getName()).isEqualTo("商品暂时无法加载");
        
        assertThat(response.getInventory()).isNotNull();
        assertThat(response.getInventory().getAvailableStock()).isEqualTo(0);
        assertThat(response.getInventory().getStatus()).isEqualTo("UNAVAILABLE");
        
        assertThat(response.getReviews()).isEmpty();
        assertThat(response.getTotalReviews()).isEqualTo(0);
        assertThat(response.getAverageRating()).isEqualTo(0.0);
    }

    private ReviewDTO createReview(Long id, Long productId, Integer rating) {
        ReviewDTO review = new ReviewDTO();
        review.setId(id);
        review.setProductId(productId);
        review.setUsername("用户" + id);
        review.setRating(rating);
        review.setContent("评论内容 " + id);
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}
