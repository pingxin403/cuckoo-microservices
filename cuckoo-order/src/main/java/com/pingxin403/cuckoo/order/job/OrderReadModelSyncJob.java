package com.pingxin403.cuckoo.order.job;

import com.pingxin403.cuckoo.order.service.OrderReadModelUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单读模型同步定时任务
 * 定期重试失败的同步任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReadModelSyncJob {

    private final OrderReadModelUpdater orderReadModelUpdater;

    /**
     * 每 5 分钟重试一次失败的同步任务
     */
    @Scheduled(fixedDelay = 300000) // 5 分钟
    public void retryFailedSync() {
        log.info("开始执行订单读模型同步重试任务");
        try {
            orderReadModelUpdater.retryFailedSync();
            log.info("订单读模型同步重试任务执行完成");
        } catch (Exception e) {
            log.error("订单读模型同步重试任务执行失败", e);
        }
    }
}
