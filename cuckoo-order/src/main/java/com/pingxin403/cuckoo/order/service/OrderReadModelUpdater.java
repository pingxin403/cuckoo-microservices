package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.common.event.OrderCreatedEvent;
import com.pingxin403.cuckoo.order.entity.OrderRead;
import com.pingxin403.cuckoo.order.entity.OrderReadSyncStatus;
import com.pingxin403.cuckoo.order.entity.OrderWrite;
import com.pingxin403.cuckoo.order.repository.OrderReadRepository;
import com.pingxin403.cuckoo.order.repository.OrderReadSyncStatusRepository;
import com.pingxin403.cuckoo.order.repository.OrderWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 订单读模型更新器
 * 消费 OrderCreatedEvent 并更新 order_read 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReadModelUpdater {

    private final OrderReadRepository orderReadRepository;
    private final OrderWriteRepository orderWriteRepository;
    private final OrderReadSyncStatusRepository syncStatusRepository;

    /**
     * 消费 OrderCreatedEvent 并更新读模型
     * 1. 检查事件是否已处理（幂等性）
     * 2. 从 order_write 表查询订单详情
     * 3. 更新 order_read 表
     * 4. 记录同步状态
     */
    @KafkaListener(topics = "order-events", groupId = "order-read-model-updater")
    @Transactional
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("收到订单创建事件: eventId={}, orderId={}", event.getEventId(), event.getOrderId());

        try {
            // 1. 检查事件是否已处理（幂等性）
            Optional<OrderReadSyncStatus> existingStatus = syncStatusRepository.findByEventId(event.getEventId());
            if (existingStatus.isPresent()) {
                log.info("事件已处理，跳过: eventId={}", event.getEventId());
                return;
            }

            // 2. 记录同步状态为 PENDING
            OrderReadSyncStatus syncStatus = new OrderReadSyncStatus();
            syncStatus.setOrderId(event.getOrderId().toString());
            syncStatus.setEventId(event.getEventId());
            syncStatus.setEventType("OrderCreatedEvent");
            syncStatus.setSyncStatus(OrderReadSyncStatus.SyncStatus.PENDING);
            syncStatus.setRetryCount(0);
            syncStatus.setCreatedAt(LocalDateTime.now());
            syncStatus.setUpdatedAt(LocalDateTime.now());
            syncStatus = syncStatusRepository.save(syncStatus);

            // 3. 从 order_write 表查询订单详情
            String orderId = event.getOrderId().toString();
            OrderWrite orderWrite = orderWriteRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

            // 4. 更新 order_read 表
            OrderRead orderRead = new OrderRead();
            orderRead.setOrderId(orderId);
            orderRead.setUserId(orderWrite.getUserId());
            orderRead.setUserName(null); // TODO: 从用户服务获取用户名
            orderRead.setTotalAmount(orderWrite.getTotalAmount());
            orderRead.setStatus(orderWrite.getStatus().name());
            orderRead.setStatusDisplay(OrderRead.getStatusDisplay(orderWrite.getStatus().name()));
            orderRead.setItemCount(1); // 简化实现，假设只有一个商品
            orderRead.setProductNames(orderWrite.getProductName());
            orderRead.setSkuIds(orderWrite.getSkuId().toString());
            orderRead.setPaymentId(orderWrite.getPaymentId());
            orderRead.setCancelReason(orderWrite.getCancelReason());
            orderRead.setCreatedAt(orderWrite.getCreatedAt());
            orderRead.setUpdatedAt(orderWrite.getUpdatedAt());

            orderReadRepository.save(orderRead);
            log.info("订单读模型已更新: orderId={}", orderId);

            // 5. 更新同步状态为 SUCCESS
            syncStatus.setSyncStatus(OrderReadSyncStatus.SyncStatus.SUCCESS);
            syncStatus.setUpdatedAt(LocalDateTime.now());
            syncStatusRepository.save(syncStatus);
            log.info("订单读模型同步成功: eventId={}, orderId={}", event.getEventId(), orderId);

        } catch (Exception e) {
            log.error("订单读模型同步失败: eventId={}, orderId={}", event.getEventId(), event.getOrderId(), e);

            // 更新同步状态为 FAILED
            Optional<OrderReadSyncStatus> statusOpt = syncStatusRepository.findByEventId(event.getEventId());
            if (statusOpt.isPresent()) {
                OrderReadSyncStatus syncStatus = statusOpt.get();
                syncStatus.setSyncStatus(OrderReadSyncStatus.SyncStatus.FAILED);
                syncStatus.setRetryCount(syncStatus.getRetryCount() + 1);
                syncStatus.setErrorMessage(e.getMessage());
                syncStatus.setUpdatedAt(LocalDateTime.now());
                syncStatusRepository.save(syncStatus);
            }

            // 重新抛出异常，触发 Kafka 重试
            throw new RuntimeException("订单读模型同步失败", e);
        }
    }

    /**
     * 手动同步订单读模型
     * 用于数据修复
     */
    @Transactional
    public void syncOrderReadModel(String orderId) {
        log.info("手动同步订单读模型: orderId={}", orderId);

        try {
            // 1. 从 order_write 表查询订单详情
            OrderWrite orderWrite = orderWriteRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

            // 2. 更新 order_read 表
            OrderRead orderRead = orderReadRepository.findById(orderId)
                    .orElse(new OrderRead());

            orderRead.setOrderId(orderId);
            orderRead.setUserId(orderWrite.getUserId());
            orderRead.setUserName(null); // TODO: 从用户服务获取用户名
            orderRead.setTotalAmount(orderWrite.getTotalAmount());
            orderRead.setStatus(orderWrite.getStatus().name());
            orderRead.setStatusDisplay(OrderRead.getStatusDisplay(orderWrite.getStatus().name()));
            orderRead.setItemCount(1); // 简化实现
            orderRead.setProductNames(orderWrite.getProductName());
            orderRead.setSkuIds(orderWrite.getSkuId().toString());
            orderRead.setPaymentId(orderWrite.getPaymentId());
            orderRead.setCancelReason(orderWrite.getCancelReason());
            orderRead.setCreatedAt(orderWrite.getCreatedAt());
            orderRead.setUpdatedAt(orderWrite.getUpdatedAt());

            orderReadRepository.save(orderRead);
            log.info("订单读模型手动同步成功: orderId={}", orderId);

        } catch (Exception e) {
            log.error("订单读模型手动同步失败: orderId={}", orderId, e);
            throw new RuntimeException("订单读模型手动同步失败", e);
        }
    }

    /**
     * 重试失败的同步任务
     */
    @Transactional
    public void retryFailedSync() {
        log.info("开始重试失败的同步任务");

        // 查询失败的同步任务（重试次数 < 3）
        syncStatusRepository.findBySyncStatus(OrderReadSyncStatus.SyncStatus.FAILED)
                .stream()
                .filter(status -> status.getRetryCount() < 3)
                .forEach(status -> {
                    try {
                        log.info("重试同步: orderId={}, retryCount={}", status.getOrderId(), status.getRetryCount());
                        syncOrderReadModel(status.getOrderId());

                        // 更新同步状态为 SUCCESS
                        status.setSyncStatus(OrderReadSyncStatus.SyncStatus.SUCCESS);
                        status.setErrorMessage(null);
                        status.setUpdatedAt(LocalDateTime.now());
                        syncStatusRepository.save(status);

                    } catch (Exception e) {
                        log.error("重试同步失败: orderId={}", status.getOrderId(), e);

                        // 更新重试次数
                        status.setRetryCount(status.getRetryCount() + 1);
                        status.setErrorMessage(e.getMessage());
                        status.setUpdatedAt(LocalDateTime.now());
                        syncStatusRepository.save(status);
                    }
                });

        log.info("失败同步任务重试完成");
    }
}
