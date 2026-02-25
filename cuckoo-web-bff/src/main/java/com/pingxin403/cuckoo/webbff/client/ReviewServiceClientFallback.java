package com.pingxin403.cuckoo.webbff.client;

import com.pingxin403.cuckoo.webbff.dto.ReviewDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 评价服务降级处理
 */
@Slf4j
@Component
public class ReviewServiceClientFallback implements ReviewServiceClient {

    @Override
    public List<ReviewDTO> getProductReviews(Long productId) {
        log.warn("Review service fallback triggered for productId: {}", productId);
        return Collections.emptyList();
    }

}
