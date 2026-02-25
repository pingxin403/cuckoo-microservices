package com.pingxin403.cuckoo.order.saga;

/**
 * Saga 步骤接口
 * 每个 Saga 步骤需要实现此接口
 */
public interface SagaStep {
    
    /**
     * 执行步骤
     * @param context Saga 上下文
     * @throws SagaStepException 步骤执行失败时抛出
     */
    void execute(SagaContext context) throws SagaStepException;
    
    /**
     * 补偿操作
     * 当 Saga 失败时，按照相反顺序执行补偿
     * @param context Saga 上下文
     * @throws CompensationException 补偿失败时抛出
     */
    void compensate(SagaContext context) throws CompensationException;
    
    /**
     * 获取步骤名称
     */
    String getName();
    
    /**
     * 获取超时时间（毫秒）
     * 默认 30 秒
     */
    default long getTimeout() {
        return 30000;
    }
}
