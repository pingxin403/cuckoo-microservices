package com.pingxin403.cuckoo.mobilebff.client;

import com.pingxin403.cuckoo.mobilebff.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 订单服务降级处理
 */
@Slf4j
@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public List<OrderDTO> getRecentOrders(Long userId, Integer limit) {
        log.warn("Order service fallback triggered for userId: {}", userId);
        return Collections.emptyList();
    }

}
