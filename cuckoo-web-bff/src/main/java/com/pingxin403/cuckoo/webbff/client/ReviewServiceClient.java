package com.pingxin403.cuckoo.webbff.client;

import com.pingxin403.cuckoo.webbff.dto.ReviewDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 评价服务 Feign 客户端
 * 注意：评价服务暂未实现，这里作为示例
 */
@FeignClient(
    name = "cuckoo-review",
    path = "/api/reviews",
    fallback = ReviewServiceClientFallback.class
)
public interface ReviewServiceClient {

    /**
     * 获取商品评价列表
     */
    @GetMapping("/product/{productId}")
    List<ReviewDTO> getProductReviews(@PathVariable("productId") Long productId);

}
