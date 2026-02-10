package com.pingxin403.cuckoo.order.job;

import com.pingxin403.cuckoo.order.entity.Order;
import com.pingxin403.cuckoo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时取消定时任务
 * 每 5 分钟扫描一次超时未支付的订单
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    /**
     * 订单超时时间（分钟），从 Nacos Config 动态读取
     */
    @Value("${order.timeout.minutes:30}")
    private int timeoutMinutes;

    /**
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedRate = 300000) // 5 分钟 = 300000 毫秒
    public void cancelTimeoutOrders() {
        log.info("开始扫描超时订单，超时时间: {} 分钟", timeoutMinutes);

        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> timeoutOrders = orderService.findTimeoutOrders(timeoutBefore);

        if (timeoutOrders.isEmpty()) {
            log.info("未发现超时订单");
            return;
        }

        log.info("发现 {} 个超时订单，开始取消", timeoutOrders.size());
        for (Order order : timeoutOrders) {
            try {
                orderService.cancelTimeoutOrder(order);
                log.info("订单超时取消成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("订单超时取消失败: orderId={}, orderNo={}", order.getId(), order.getOrderNo(), e);
            }
        }

        log.info("超时订单扫描完成，共取消 {} 个订单", timeoutOrders.size());
    }
}
