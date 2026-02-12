package com.pingxin403.cuckoo.order.client.fallback;

import com.pingxin403.cuckoo.order.client.ProductClient;
import com.pingxin403.cuckoo.order.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ProductClient 降级处理
 * 当商品服务不可用时，返回默认商品信息
 */
@Slf4j
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ProductDTO getProduct(Long id) {
        log.warn("商品服务调用失败，执行降级逻辑，商品ID: {}", id);
        
        // 返回默认商品信息
        ProductDTO fallbackProduct = new ProductDTO();
        fallbackProduct.setId(id);
        fallbackProduct.setName("商品暂时不可用");
        fallbackProduct.setPrice(BigDecimal.ZERO);
        fallbackProduct.setDescription("商品服务暂时不可用");
        
        return fallbackProduct;
    }
}
