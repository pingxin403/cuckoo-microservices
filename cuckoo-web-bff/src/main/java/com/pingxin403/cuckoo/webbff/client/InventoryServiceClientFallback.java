package com.pingxin403.cuckoo.webbff.client;

import com.pingxin403.cuckoo.webbff.dto.InventoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存服务降级处理
 */
@Slf4j
@Component
public class InventoryServiceClientFallback implements InventoryServiceClient {

    @Override
    public InventoryDTO getInventory(Long productId) {
        log.warn("Inventory service fallback triggered for productId: {}", productId);
        InventoryDTO fallback = new InventoryDTO();
        fallback.setProductId(productId);
        fallback.setAvailableStock(0);
        fallback.setStatus("UNAVAILABLE");
        return fallback;
    }

}
