package com.pingxin403.cuckoo.order.client;

import com.pingxin403.cuckoo.order.client.fallback.ProductClientFallback;
import com.pingxin403.cuckoo.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务 Feign 客户端
 */
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    /**
     * 查询商品信息
     */
    @GetMapping("/api/products/{id}")
    ProductDTO getProduct(@PathVariable("id") Long id);
}
