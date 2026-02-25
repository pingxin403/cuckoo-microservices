package com.pingxin403.cuckoo.order.saga;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Saga 定义
 * 定义 Saga 的类型、步骤和超时时间
 */
@Data
@Builder
public class SagaDefinition {
    
    /**
     * Saga 类型（如 ORDER_CREATION）
     */
    private String sagaType;
    
    /**
     * Saga 步骤列表
     */
    @Builder.Default
    private List<SagaStep> steps = new ArrayList<>();
    
    /**
     * Saga 超时时间
     * 默认 5 分钟
     */
    @Builder.Default
    private Duration timeout = Duration.ofMinutes(5);
    
    /**
     * 添加步骤
     */
    public SagaDefinition addStep(SagaStep step) {
        this.steps.add(step);
        return this;
    }
    
    /**
     * 获取步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }
    
    /**
     * 获取指定索引的步骤
     */
    public SagaStep getStep(int index) {
        return steps.get(index);
    }
}
