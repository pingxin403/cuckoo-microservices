package com.pingxin403.cuckoo.order.saga.steps;

import com.pingxin403.cuckoo.common.exception.BusinessException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.common.exception.SystemException;
import com.pingxin403.cuckoo.order.client.ProductClient;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.ProductDTO;
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
import java.util.UUID;

/**
 * 创建订单步骤
 * 执行：创建订单记录（状态为待支付）
 * 补偿：取消订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderStep implements SagaStep {
    
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    
    @Override
    public void execute(SagaContext context) throws SagaStepException {
        try {
            CreateOrderRequest request = context.get("orderRequest");
            
            // 1. 查询商品信息
            ProductDTO product;
            try {
                product = productClient.getProduct(request.getSkuId());
                if (product == null) {
                    throw new ResourceNotFoundException("商品不存在: skuId=" + request.getSkuId());
                }
                log.info("查询商品信息成功: productName={}, price={}", product.getName(), product.getPrice());
            } catch (BusinessException e) {
                log.error("查询商品信息失败（业务异常）: skuId={}, error={}", request.getSkuId(), e.getMessage());
                throw new SagaStepException("查询商品信息失败: " + e.getMessage(), e);
            } catch (SystemException e) {
                log.error("查询商品信息失败（系统异常）: skuId={}, error={}", request.getSkuId(), e.getMessage());
                throw new SagaStepException("查询商品信息失败: " + e.getMessage(), e);
            }
            
            // 2. 创建订单
            String orderNo = generateOrderNo();
            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setSkuId(request.getSkuId());
            order.setProductName(product.getName());
            order.setQuantity(request.getQuantity());
            order.setUnitPrice(product.getPrice());
            order.setTotalAmount(totalAmount);
            order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
            
            order = orderRepository.save(order);
            log.info("创建订单成功: orderId={}, orderNo={}, totalAmount={}", 
                    order.getId(), order.getOrderNo(), order.getTotalAmount());
            
            // 3. 保存订单信息到上下文
            context.put("orderId", order.getId());
            context.put("orderNo", orderNo);
            context.put("totalAmount", totalAmount);
            context.put("productName", product.getName());
            
        } catch (SagaStepException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建订单失败", e);
            throw new SagaStepException("创建订单失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void compensate(SagaContext context) throws CompensationException {
        try {
            Long orderId = context.get("orderId");
            if (orderId == null) {
                log.warn("订单 ID 不存在，跳过补偿");
                return;
            }
            
            log.info("开始补偿：取消订单 orderId={}", orderId);
            
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("订单不存在，跳过补偿: orderId={}", orderId);
                return;
            }
            
            // 更新订单状态为已取消
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setCancelReason("Saga 补偿");
            orderRepository.save(order);
            
            log.info("订单取消成功: orderId={}", orderId);
            
        } catch (Exception e) {
            log.error("订单取消失败", e);
            throw new CompensationException("订单取消失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getName() {
        return "CreateOrder";
    }
    
    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
