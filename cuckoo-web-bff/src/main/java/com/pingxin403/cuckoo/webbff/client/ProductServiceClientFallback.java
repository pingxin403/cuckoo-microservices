package com.pingxin403.cuckoo.webbff.client;

import com.pingxin403.cuckoo.webbff.dto.ProductDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 商品服务降级处理
 */
@Slf4j
@Component
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ProductDetailDTO getProductDetail(Long productId) {
        log.warn("Product service fallback triggered for productId: {}", productId);
        ProductDetailDTO fallback = new ProductDetailDTO();
        fallback.setId(productId);
        fallback.setName("商品暂时无法加载");
        fallback.setPrice(BigDecimal.ZERO);
        fallback.setDescription("商品信息暂时无法获取，请稍后再试");
        return fallback;
    }

}
