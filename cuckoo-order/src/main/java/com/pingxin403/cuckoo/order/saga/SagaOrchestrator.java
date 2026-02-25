package com.pingxin403.cuckoo.order.saga;

import com.pingxin403.cuckoo.order.entity.SagaInstance;

import java.util.Map;

/**
 * Saga 编排器接口
 * 负责 Saga 的执行、补偿和状态管理
 */
public interface SagaOrchestrator {
    
    /**
     * 开始 Saga 执行
     * @param definition Saga 定义
     * @param initialContext 初始上下文数据
     * @return Saga 实例 ID
     */
    String startSaga(SagaDefinition definition, Map<String, Object> initialContext);
    
    /**
     * 获取 Saga 状态
     * @param sagaId Saga 实例 ID
     * @return Saga 实例
     */
    SagaInstance getSagaStatus(String sagaId);
    
    /**
     * 手动触发补偿
     * @param sagaId Saga 实例 ID
     */
    void compensate(String sagaId);
}
