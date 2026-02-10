package com.pingxin403.cuckoo.order.client.fallback;

import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.dto.ReserveInventoryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存服务 Sentinel 降级处理器
 */
@Slf4j
@Component
public class InventoryClientFallback implements InventoryClient {

    @Override
    public void reserveInventory(ReserveInventoryRequest request) {
        log.error("库存服务调用失败，触发降级逻辑: skuId={}, quantity={}, orderId={}",
                request.getSkuId(), request.getQuantity(), request.getOrderId());
        throw new RuntimeException("服务暂时不可用，请稍后重试");
    }
}
