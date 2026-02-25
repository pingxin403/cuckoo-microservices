package com.pingxin403.cuckoo.mobilebff.client;

import com.pingxin403.cuckoo.mobilebff.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 订单服务 Feign 客户端
 */
@FeignClient(
    name = "cuckoo-order",
    path = "/api/orders",
    fallback = OrderServiceClientFallback.class
)
public interface OrderServiceClient {

    /**
     * 获取用户最近订单
     */
    @GetMapping("/recent")
    List<OrderDTO> getRecentOrders(@RequestParam("userId") Long userId, @RequestParam("limit") Integer limit);

}
