package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.common.event.OrderCancelledEvent;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.client.ProductClient;
import com.pingxin403.cuckoo.order.dto.*;
import com.pingxin403.cuckoo.order.entity.Order;
import com.pingxin403.cuckoo.order.repository.OrderRepository;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
        ProductDTO product = productClient.getProduct(request.getSkuId());
        if (product == null) {
            throw new ResourceNotFoundException("商品不存在: skuId=" + request.getSkuId());
        }
        log.info("查询商品信息成功: productName={}, price={}", product.getName(), product.getPrice());

        // 2. 预占库存
        String orderNo = generateOrderNo();
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(
                request.getSkuId(),
                request.getQuantity(),
                orderNo
        );
        inventoryClient.reserveInventory(reserveRequest);
        log.info("预占库存成功: skuId={}, quantity={}, orderNo={}", 
                request.getSkuId(), request.getQuantity(), orderNo);

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
        PaymentDTO payment = paymentClient.createPayment(paymentRequest);
        
        // 更新订单的支付单 ID
        order.setPaymentId(payment.getId());
        order = orderRepository.save(order);
        log.info("创建支付单成功: paymentId={}, orderId={}", payment.getId(), order.getId());

        return OrderDTO.fromEntity(order);
    }

    /**
     * 查询订单
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: id=" + id));
        return OrderDTO.fromEntity(order);
    }

    /**
     * 查询用户订单列表
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
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

        // 发布 OrderCancelledEvent
        OrderCancelledEvent event = OrderCancelledEvent.create(
                order.getId(),
                order.getUserId(),
                order.getSkuId(),
                order.getQuantity(),
                "用户主动取消"
        );
        kafkaTemplate.send("order-events", order.getId().toString(), event);
        log.info("发布 OrderCancelledEvent: eventId={}, orderId={}", event.getEventId(), order.getId());

        return OrderDTO.fromEntity(order);
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

        // 发布 OrderCancelledEvent
        OrderCancelledEvent event = OrderCancelledEvent.create(
                order.getId(),
                order.getUserId(),
                order.getSkuId(),
                order.getQuantity(),
                "支付超时"
        );
        kafkaTemplate.send("order-events", order.getId().toString(), event);
        log.info("发布 OrderCancelledEvent: eventId={}, orderId={}", event.getEventId(), order.getId());
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
