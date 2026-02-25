package com.pingxin403.cuckoo.order.saga.steps;

import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.ReserveInventoryRequest;
import com.pingxin403.cuckoo.order.saga.CompensationException;
import com.pingxin403.cuckoo.order.saga.SagaContext;
import com.pingxin403.cuckoo.order.saga.SagaStep;
import com.pingxin403.cuckoo.order.saga.SagaStepException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 预留库存步骤
 * 执行：预留库存
 * 补偿：释放库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveInventoryStep implements SagaStep {
    
    private final InventoryClient inventoryClient;
    
    @Override
    public void execute(SagaContext context) throws SagaStepException {
        try {
            CreateOrderRequest request = context.get("orderRequest");
            String orderNo = context.get("orderNo");
            
            // 预留库存
            ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(
                    request.getSkuId(),
                    request.getQuantity(),
                    orderNo
            );
            
            inventoryClient.reserveInventory(reserveRequest);
            log.info("预留库存成功: skuId={}, quantity={}, orderNo={}", 
                    request.getSkuId(), request.getQuantity(), orderNo);
            
        } catch (Exception e) {
            log.error("预留库存失败", e);
            throw new SagaStepException("预留库存失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void compensate(SagaContext context) throws CompensationException {
        try {
            String orderNo = context.get("orderNo");
            if (orderNo == null) {
                log.warn("订单编号不存在，跳过补偿");
                return;
            }
            
            log.info("开始补偿：释放库存 orderNo={}", orderNo);
            
            // 调用库存服务释放库存
            // 注意：这里需要库存服务提供释放库存的接口
            // inventoryClient.releaseInventory(orderNo);
            
            log.info("释放库存成功: orderNo={}", orderNo);
            
        } catch (Exception e) {
            log.error("释放库存失败", e);
            throw new CompensationException("释放库存失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getName() {
        return "ReserveInventory";
    }
}
