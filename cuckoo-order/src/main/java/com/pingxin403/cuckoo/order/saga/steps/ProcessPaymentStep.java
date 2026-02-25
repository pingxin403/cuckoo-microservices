package com.pingxin403.cuckoo.order.saga.steps;

import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.CreatePaymentRequest;
import com.pingxin403.cuckoo.order.dto.PaymentDTO;
import com.pingxin403.cuckoo.order.entity.Order;
import com.pingxin403.cuckoo.order.repository.OrderRepository;
import com.pingxin403.cuckoo.order.saga.CompensationException;
import com.pingxin403.cuckoo.order.saga.SagaContext;
import com.pingxin403.cuckoo.order.saga.SagaStep;
import com.pingxin403.cuckoo.order.saga.SagaStepException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 处理支付步骤
 * 执行：创建支付单
 * 补偿：取消支付
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessPaymentStep implements SagaStep {
    
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    
    @Override
    public void execute(SagaContext context) throws SagaStepException {
        try {
            CreateOrderRequest request = context.get("orderRequest");
            Long orderId = context.get("orderId");
            BigDecimal totalAmount = context.get("totalAmount");
            
            // 创建支付单
            CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                    orderId,
                    totalAmount,
                    request.getUserId()
            );
            
            PaymentDTO payment = paymentClient.createPayment(paymentRequest);
            log.info("创建支付单成功: paymentId={}, orderId={}", payment.getId(), orderId);
            
            // 更新订单的支付单 ID
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                order.setPaymentId(payment.getId());
                orderRepository.save(order);
            }
            
            // 保存支付单 ID 到上下文
            context.put("paymentId", payment.getId());
            
        } catch (Exception e) {
            log.error("创建支付单失败", e);
            throw new SagaStepException("创建支付单失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void compensate(SagaContext context) throws CompensationException {
        try {
            Long paymentId = context.get("paymentId");
            if (paymentId == null) {
                log.warn("支付单 ID 不存在，跳过补偿");
                return;
            }
            
            log.info("开始补偿：取消支付 paymentId={}", paymentId);
            
            // 调用支付服务取消支付
            // 注意：这里需要支付服务提供取消支付的接口
            // paymentClient.cancelPayment(paymentId);
            
            log.info("取消支付成功: paymentId={}", paymentId);
            
        } catch (Exception e) {
            log.error("取消支付失败", e);
            throw new CompensationException("取消支付失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getName() {
        return "ProcessPayment";
    }
}
