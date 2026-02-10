package com.pingxin403.cuckoo.order.client;

import com.pingxin403.cuckoo.order.client.fallback.InventoryClientFallback;
import com.pingxin403.cuckoo.order.dto.ReserveInventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务 Feign 客户端
 */
@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    /**
     * 预占库存
     */
    @PostMapping("/api/inventory/reserve")
    void reserveInventory(@RequestBody ReserveInventoryRequest request);
}
