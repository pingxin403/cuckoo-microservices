package com.pingxin403.cuckoo.order.saga;

import com.pingxin403.cuckoo.order.entity.SagaInstance;
import com.pingxin403.cuckoo.order.entity.SagaInstance.SagaStatus;
import com.pingxin403.cuckoo.order.repository.SagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 超时调度器
 * 定时扫描超时的 Saga 实例并触发补偿
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutScheduler {
    
    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaOrchestrator sagaOrchestrator;
    
    /**
     * 每分钟检查一次超时的 Saga 实例
     */
    @Scheduled(fixedDelay = 60000) // 60 秒
    public void checkTimeoutSagas() {
        log.debug("开始检查超时的 Saga 实例");
        
        LocalDateTime now = LocalDateTime.now();
        
        // 查询超时的 RUNNING 状态的 Saga 实例
        List<SagaInstance> timeoutSagas = sagaInstanceRepository
                .findByStatusAndTimeoutAtBefore(SagaStatus.RUNNING, now);
        
        if (timeoutSagas.isEmpty()) {
            log.debug("没有超时的 Saga 实例");
            return;
        }
        
        log.warn("发现 {} 个超时的 Saga 实例", timeoutSagas.size());
        
        for (SagaInstance saga : timeoutSagas) {
            try {
                log.warn("Saga 超时，触发补偿: sagaId={}, sagaType={}, startedAt={}, timeoutAt={}", 
                        saga.getSagaId(), saga.getSagaType(), saga.getStartedAt(), saga.getTimeoutAt());
                
                // 触发补偿
                sagaOrchestrator.compensate(saga.getSagaId());
                
                log.info("Saga 超时补偿已触发: sagaId={}", saga.getSagaId());
                
            } catch (Exception e) {
                log.error("触发 Saga 超时补偿失败: sagaId={}", saga.getSagaId(), e);
            }
        }
    }
}
