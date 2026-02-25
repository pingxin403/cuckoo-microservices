package com.pingxin403.cuckoo.order.saga;

import com.pingxin403.cuckoo.order.saga.steps.CreateOrderStep;
import com.pingxin403.cuckoo.order.saga.steps.ProcessPaymentStep;
import com.pingxin403.cuckoo.order.saga.steps.ReserveInventoryStep;
import com.pingxin403.cuckoo.order.saga.steps.SendNotificationStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单 Saga 定义
 * 定义订单创建的 Saga 流程
 */
@Component
@RequiredArgsConstructor
public class OrderSagaDefinition {
    
    private final CreateOrderStep createOrderStep;
    private final ReserveInventoryStep reserveInventoryStep;
    private final ProcessPaymentStep processPaymentStep;
    private final SendNotificationStep sendNotificationStep;
    
    /**
     * 创建订单 Saga
     * 流程：
     * 1. 创建订单
     * 2. 预留库存
     * 3. 处理支付
     * 4. 发送通知
     */
    public SagaDefinition createOrderSaga() {
        return SagaDefinition.builder()
                .sagaType("ORDER_CREATION")
                .timeout(Duration.ofMinutes(5))
                .build()
                .addStep(createOrderStep)
                .addStep(reserveInventoryStep)
                .addStep(processPaymentStep)
                .addStep(sendNotificationStep);
    }
}
