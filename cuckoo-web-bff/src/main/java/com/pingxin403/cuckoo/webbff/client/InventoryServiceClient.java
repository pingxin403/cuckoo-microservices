package com.pingxin403.cuckoo.webbff.client;

import com.pingxin403.cuckoo.webbff.dto.InventoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 库存服务 Feign 客户端
 */
@FeignClient(
    name = "cuckoo-inventory",
    path = "/api/inventory",
    fallback = InventoryServiceClientFallback.class
)
public interface InventoryServiceClient {

    /**
     * 获取商品库存信息
     */
    @GetMapping("/{productId}")
    InventoryDTO getInventory(@PathVariable("productId") Long productId);

}
