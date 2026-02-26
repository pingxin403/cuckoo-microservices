package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.common.event.EventPublisherUtil;
import com.pingxin403.cuckoo.common.event.OrderCancelledEvent;
import com.pingxin403.cuckoo.common.event.OrderCreatedEvent;
import com.pingxin403.cuckoo.common.exception.BusinessException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.common.exception.SystemException;
import com.pingxin403.cuckoo.common.message.LocalMessageService;
import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.client.ProductClient;
import com.pingxin403.cuckoo.order.dto.*;
import com.pingxin403.cuckoo.order.entity.Order;
import com.pingxin403.cuckoo.order.mapper.OrderMapper;
import com.pingxin403.cuckoo.order.repository.OrderRepository;
import com.pingxin403.cuckoo.order.saga.OrderSagaDefinition;
import com.pingxin403.cuckoo.order.saga.SagaDefinition;
import com.pingxin403.cuckoo.order.saga.SagaOrchestrator;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final EventPublisherUtil eventPublisher;
    private final LocalMessageService localMessageService;
    private final SagaOrchestrator sagaOrchestrator;
    private final OrderSagaDefinition orderSagaDefinition;

    /**
     * 创建订单（Seata 全局事务）
     * 1. 查询商品信息
     * 2. 预占库存
     * 3. 创建订单（状态为待支付）
     * 4. 创建支付单
     */
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("开始创建订单: userId={}, skuId={}, quantity={}",
                request.getUserId(), request.getSkuId(), request.getQuantity());

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
            throw e;
        } catch (SystemException e) {
            log.error("查询商品信息失败（系统异常）: skuId={}, error={}", request.getSkuId(), e.getMessage());
            throw e;
        }

        // 2. 预占库存
        String orderNo = generateOrderNo();
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(
                request.getSkuId(),
                request.getQuantity(),
                orderNo
        );
        try {
            inventoryClient.reserveInventory(reserveRequest);
            log.info("预占库存成功: skuId={}, quantity={}, orderNo={}", 
                    request.getSkuId(), request.getQuantity(), orderNo);
        } catch (BusinessException e) {
            log.error("预占库存失败（业务异常）: skuId={}, error={}", request.getSkuId(), e.getMessage());
            throw e;
        } catch (SystemException e) {
            log.error("预占库存失败（系统异常）: skuId={}, error={}", request.getSkuId(), e.getMessage());
            throw e;
        }

        // 3. 创建订单（状态为待支付）
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

        // 4. 创建支付单
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                order.getId(),
                totalAmount,
                request.getUserId()
        );
        PaymentDTO payment;
        try {
            payment = paymentClient.createPayment(paymentRequest);
            
            // 更新订单的支付单 ID
            order.setPaymentId(payment.getId());
            order = orderRepository.save(order);
            log.info("创建支付单成功: paymentId={}, orderId={}", payment.getId(), order.getId());
        } catch (BusinessException e) {
            log.error("创建支付单失败（业务异常）: orderId={}, error={}", order.getId(), e.getMessage());
            throw e;
        } catch (SystemException e) {
            log.error("创建支付单失败（系统异常）: orderId={}, error={}", order.getId(), e.getMessage());
            throw e;
        }

        // 5. 在同一事务中保存 OrderCreatedEvent 到本地消息表
        OrderCreatedEvent event = OrderCreatedEvent.create(
                String.valueOf(order.getId()),  // Convert Long to String
                order.getUserId(),
                order.getSkuId(),
                order.getQuantity(),
                order.getTotalAmount()
        );
        localMessageService.saveMessage(event);
        log.info("订单创建事件已保存到本地消息表: eventId={}, orderId={}", event.getEventId(), order.getId());

        // 6. 异步发布事件到 Kafka（失败不影响事务提交）
        try {
            eventPublisher.publish("order-events", order.getId().toString(), event);
            localMessageService.markAsSent(event.getEventId());
            log.info("订单创建事件已发布到 Kafka: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("发布订单创建事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
            // 消息保持 PENDING 状态，等待定时任务重试
        }

        return orderMapper.toDTO(order);
    }
    
    /**
     * 创建订单（使用 Saga 模式）
     * 1. 创建订单
     * 2. 预留库存
     * 3. 处理支付
     * 4. 发送通知
     */
    public String createOrderWithSaga(CreateOrderRequest request) {
        log.info("开始创建订单（Saga 模式）: userId={}, skuId={}, quantity={}",
                request.getUserId(), request.getSkuId(), request.getQuantity());
        
        // 准备 Saga 上下文
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("orderRequest", request);
        
        // 创建 Saga 定义
        SagaDefinition sagaDefinition = orderSagaDefinition.createOrderSaga();
        
        // 启动 Saga
        String sagaId = sagaOrchestrator.startSaga(sagaDefinition, initialContext);
        log.info("订单 Saga 已启动: sagaId={}", sagaId);
        
        return sagaId;
    }

    /**
     * 查询订单
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: id=" + id));
        return orderMapper.toDTO(order);
    }

    /**
     * 查询用户订单列表
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orderMapper.toDTOList(orders);
    }

    /**
     * 取消订单
     * 1. 更新订单状态为已取消
     * 2. 发布 OrderCancelledEvent 到 Kafka
     */
    @Transactional
    public OrderDTO cancelOrder(Long id) {
        log.info("开始取消订单: orderId={}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: id=" + id));

        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("只能取消待支付状态的订单");
        }

        // 更新订单状态
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelReason("用户主动取消");
        order = orderRepository.save(order);
        log.info("订单状态已更新为已取消: orderId={}", id);

        // 在同一事务中保存 OrderCancelledEvent 到本地消息表
        OrderCancelledEvent event = OrderCancelledEvent.create(
                order.getId(),
                order.getUserId(),
                order.getSkuId(),
                order.getQuantity(),
                "用户主动取消"
        );
        localMessageService.saveMessage(event);
        log.info("订单取消事件已保存到本地消息表: eventId={}, orderId={}", event.getEventId(), order.getId());

        // 异步发布事件到 Kafka（失败不影响事务提交）
        try {
            eventPublisher.publish("order-events", order.getId().toString(), event);
            localMessageService.markAsSent(event.getEventId());
            log.info("订单取消事件已发布到 Kafka: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("发布订单取消事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
            // 消息保持 PENDING 状态，等待定时任务重试
        }

        return orderMapper.toDTO(order);
    }

    /**
     * 更新订单状态为已支付
     * 由 Kafka 消费者调用
     */
    @Transactional
    public void updateOrderToPaid(Long orderId, Long paymentId) {
        log.info("更新订单状态为已支付: orderId={}, paymentId={}", orderId, paymentId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: id=" + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            log.warn("订单状态不是待支付，跳过更新: orderId={}, currentStatus={}", orderId, order.getStatus());
            return;
        }

        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentId(paymentId);
        orderRepository.save(order);
        log.info("订单状态已更新为已支付: orderId={}", orderId);
    }

    /**
     * 订单超时取消
     * 由定时任务调用
     */
    @Transactional
    public void cancelTimeoutOrder(Order order) {
        log.info("订单超时取消: orderId={}, orderNo={}", order.getId(), order.getOrderNo());

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelReason("支付超时");
        orderRepository.save(order);

        // 在同一事务中保存 OrderCancelledEvent 到本地消息表
        OrderCancelledEvent event = OrderCancelledEvent.create(
                order.getId(),
                order.getUserId(),
                order.getSkuId(),
                order.getQuantity(),
                "支付超时"
        );
        localMessageService.saveMessage(event);
        log.info("订单超时取消事件已保存到本地消息表: eventId={}, orderId={}", event.getEventId(), order.getId());

        // 异步发布事件到 Kafka（失败不影响事务提交）
        try {
            eventPublisher.publish("order-events", order.getId().toString(), event);
            localMessageService.markAsSent(event.getEventId());
            log.info("订单超时取消事件已发布到 Kafka: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("发布订单超时取消事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
            // 消息保持 PENDING 状态，等待定时任务重试
        }
    }

    /**
     * 查询超时未支付的订单
     */
    @Transactional(readOnly = true)
    public List<Order> findTimeoutOrders(LocalDateTime timeoutBefore) {
        return orderRepository.findByStatusAndCreatedAtBefore(
                Order.OrderStatus.PENDING_PAYMENT,
                timeoutBefore
        );
    }

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
