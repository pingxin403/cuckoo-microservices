package com.pingxin403.cuckoo.webbff.controller;

import com.pingxin403.cuckoo.webbff.dto.ProductPageResponse;
import com.pingxin403.cuckoo.webbff.service.WebProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Web 端商品控制器
 */
@Slf4j
@RestController
@RequestMapping("/web/api/products")
public class WebProductController {

    @Autowired
    private WebProductService productService;

    /**
     * 获取商品详情页数据
     * 聚合商品信息、库存信息和评价列表
     */
    @GetMapping("/{productId}")
    public CompletableFuture<ProductPageResponse> getProductPage(@PathVariable Long productId) {
        log.info("Fetching product page data for productId: {}", productId);
        
        return productService.aggregateProductPage(productId)
            .orTimeout(3, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                log.error("Failed to fetch product page data for productId: {}", productId, ex);
                return productService.buildDegradedResponse(productId);
            });
    }

}
