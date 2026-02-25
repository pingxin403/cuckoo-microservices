package com.pingxin403.cuckoo.webbff.client;

import com.pingxin403.cuckoo.webbff.dto.ProductDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端
 */
@FeignClient(
    name = "cuckoo-product",
    path = "/api/products",
    fallback = ProductServiceClientFallback.class
)
public interface ProductServiceClient {

    /**
     * 获取商品详情
     */
    @GetMapping("/{productId}")
    ProductDetailDTO getProductDetail(@PathVariable("productId") Long productId);

}
