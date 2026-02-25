package com.pingxin403.cuckoo.webbff.service;

import com.pingxin403.cuckoo.webbff.client.InventoryServiceClient;
import com.pingxin403.cuckoo.webbff.client.ProductServiceClient;
import com.pingxin403.cuckoo.webbff.client.ReviewServiceClient;
import com.pingxin403.cuckoo.webbff.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Web 端商品服务
 * 负责聚合多个后端服务的数据
 */
@Slf4j
@Service
public class WebProductService {

    @Autowired
    private ProductServiceClient productClient;

    @Autowired
    private InventoryServiceClient inventoryClient;

    @Autowired
    private ReviewServiceClient reviewClient;

    /**
     * 聚合商品详情页数据
     * 并行调用多个服务以减少延迟
     */
    public CompletableFuture<ProductPageResponse> aggregateProductPage(Long productId) {
        // 并行调用三个服务
        CompletableFuture<ProductDetailDTO> productFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return productClient.getProductDetail(productId);
            } catch (Exception e) {
                log.error("Failed to fetch product detail for productId: {}", productId, e);
                return createFallbackProduct(productId);
            }
        });

        CompletableFuture<InventoryDTO> inventoryFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return inventoryClient.getInventory(productId);
            } catch (Exception e) {
                log.error("Failed to fetch inventory for productId: {}", productId, e);
                return createFallbackInventory(productId);
            }
        });

        CompletableFuture<List<ReviewDTO>> reviewsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return reviewClient.getProductReviews(productId);
            } catch (Exception e) {
                log.error("Failed to fetch reviews for productId: {}", productId, e);
                return Collections.emptyList();
            }
        });

        // 等待所有调用完成并聚合结果
        return CompletableFuture.allOf(productFuture, inventoryFuture, reviewsFuture)
            .thenApply(v -> {
                ProductPageResponse response = new ProductPageResponse();
                response.setProduct(productFuture.join());
                response.setInventory(inventoryFuture.join());
                
                List<ReviewDTO> reviews = reviewsFuture.join();
                response.setReviews(reviews);
                response.setTotalReviews(reviews.size());
                response.setAverageRating(calculateAverageRating(reviews));
                
                log.info("Successfully aggregated product page data for productId: {}", productId);
                return response;
            });
    }

    /**
     * 构建降级响应
     * 当所有服务调用失败时返回
     */
    public ProductPageResponse buildDegradedResponse(Long productId) {
        log.warn("Building degraded response for productId: {}", productId);
        
        ProductPageResponse response = new ProductPageResponse();
        response.setProduct(createFallbackProduct(productId));
        response.setInventory(createFallbackInventory(productId));
        response.setReviews(Collections.emptyList());
        response.setTotalReviews(0);
        response.setAverageRating(0.0);
        
        return response;
    }

    /**
     * 创建降级商品信息
     */
    private ProductDetailDTO createFallbackProduct(Long productId) {
        ProductDetailDTO product = new ProductDetailDTO();
        product.setId(productId);
        product.setName("商品暂时无法加载");
        product.setPrice(BigDecimal.ZERO);
        product.setDescription("商品信息暂时无法获取，请稍后再试");
        return product;
    }

    /**
     * 创建降级库存信息
     */
    private InventoryDTO createFallbackInventory(Long productId) {
        InventoryDTO inventory = new InventoryDTO();
        inventory.setProductId(productId);
        inventory.setAvailableStock(0);
        inventory.setStatus("UNAVAILABLE");
        return inventory;
    }

    /**
     * 计算平均评分
     */
    private Double calculateAverageRating(List<ReviewDTO> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
            .mapToInt(ReviewDTO::getRating)
            .average()
            .orElse(0.0);
    }

}
