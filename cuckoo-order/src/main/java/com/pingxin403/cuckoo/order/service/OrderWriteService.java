package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.common.event.EventPublisherUtil;
import com.pingxin403.cuckoo.common.event.OrderCreatedEvent;
import com.pingxin403.cuckoo.common.message.LocalMessageService;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.entity.OrderItemWrite;
import com.pingxin403.cuckoo.order.entity.OrderWrite;
import com.pingxin403.cuckoo.order.repository.OrderItemWriteRepository;
import com.pingxin403.cuckoo.order.repository.OrderWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单写模型服务（CQRS 写模型）
 * 负责处理命令操作：创建、更新、删除订单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWriteService {

    private final OrderWriteRepository orderWriteRepository;
    private final OrderItemWriteRepository orderItemWriteRepository;
    private final EventPublisherUtil eventPublisher;
    private final LocalMessageService localMessageService;

    /**
     * 创建订单（写入 order_write 表）
     * 1. 写入 order_write 表
     * 2. 写入 order_item_write 表
     * 3. 在同一事务中保存 OrderCreatedEvent 到本地消息表
     * 4. 异步发布 OrderCreatedEvent 到 Kafka
     */
    @Transactional
    public OrderWrite createOrder(CreateOrderRequest request, String productName, BigDecimal unitPrice) {
        log.info("开始创建订单（写模型）: userId={}, skuId={}, quantity={}",
                request.getUserId(), request.getSkuId(), request.getQuantity());

        // 1. 生成订单 ID
        String orderId = UUID.randomUUID().toString();

        // 2. 计算总金额
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // 3. 创建订单写模型
        OrderWrite orderWrite = new OrderWrite();
        orderWrite.setOrderId(orderId);
        orderWrite.setUserId(request.getUserId());
        orderWrite.setSkuId(request.getSkuId());
        orderWrite.setProductName(productName);
        orderWrite.setQuantity(request.getQuantity());
        orderWrite.setUnitPrice(unitPrice);
        orderWrite.setTotalAmount(totalAmount);
        orderWrite.setStatus(OrderWrite.OrderStatus.PENDING_PAYMENT);
        orderWrite.setCreatedAt(LocalDateTime.now());
        orderWrite.setUpdatedAt(LocalDateTime.now());

        orderWrite = orderWriteRepository.save(orderWrite);
        log.info("订单写模型已保存: orderId={}, totalAmount={}", orderId, totalAmount);

        // 4. 创建订单明细写模型
        OrderItemWrite orderItem = new OrderItemWrite();
        orderItem.setOrderId(orderId);
        orderItem.setSkuId(request.getSkuId());
        orderItem.setProductName(productName);
        orderItem.setQuantity(request.getQuantity());
        orderItem.setUnitPrice(unitPrice);
        orderItem.setSubtotal(totalAmount);

        orderItemWriteRepository.save(orderItem);
        log.info("订单明细写模型已保存: orderId={}, skuId={}", orderId, request.getSkuId());

        // 5. 在同一事务中保存 OrderCreatedEvent 到本地消息表
        OrderCreatedEvent event = OrderCreatedEvent.create(
                orderId,
                request.getUserId(),
                request.getSkuId(),
                request.getQuantity(),
                totalAmount
        );
        localMessageService.saveMessage(event);
        log.info("订单创建事件已保存到本地消息表: eventId={}, orderId={}", event.getEventId(), orderId);

        // 6. 异步发布事件到 Kafka（失败不影响事务提交）
        try {
            eventPublisher.publish("order-events", orderId, event);
            localMessageService.markAsSent(event.getEventId());
            log.info("订单创建事件已发布到 Kafka: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("发布订单创建事件失败，将由定时任务重试: eventId={}", event.getEventId(), e);
            // 消息保持 PENDING 状态，等待定时任务重试
        }

        return orderWrite;
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public OrderWrite updateOrderStatus(String orderId, OrderWrite.OrderStatus status, String cancelReason) {
        log.info("更新订单状态（写模型）: orderId={}, status={}", orderId, status);

        OrderWrite orderWrite = orderWriteRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        orderWrite.setStatus(status);
        if (cancelReason != null) {
            orderWrite.setCancelReason(cancelReason);
        }
        orderWrite.setUpdatedAt(LocalDateTime.now());

        orderWrite = orderWriteRepository.save(orderWrite);
        log.info("订单状态已更新（写模型）: orderId={}, status={}", orderId, status);

        return orderWrite;
    }

    /**
     * 更新支付单 ID
     */
    @Transactional
    public OrderWrite updatePaymentId(String orderId, Long paymentId) {
        log.info("更新支付单 ID（写模型）: orderId={}, paymentId={}", orderId, paymentId);

        OrderWrite orderWrite = orderWriteRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        orderWrite.setPaymentId(paymentId);
        orderWrite.setUpdatedAt(LocalDateTime.now());

        orderWrite = orderWriteRepository.save(orderWrite);
        log.info("支付单 ID 已更新（写模型）: orderId={}, paymentId={}", orderId, paymentId);

        return orderWrite;
    }

    /**
     * 根据订单 ID 查询订单（写模型）
     */
    @Transactional(readOnly = true)
    public OrderWrite getOrderById(String orderId) {
        return orderWriteRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));
    }
}
