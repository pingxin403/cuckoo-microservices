package com.pingxin403.cuckoo.order.repository;

import com.pingxin403.cuckoo.order.entity.SagaStepExecution;
import com.pingxin403.cuckoo.order.entity.SagaStepExecution.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Saga 步骤执行记录仓储接口
 */
@Repository
public interface SagaStepExecutionRepository extends JpaRepository<SagaStepExecution, Long> {
    
    /**
     * 根据 Saga ID 查询所有步骤执行记录
     */
    List<SagaStepExecution> findBySagaId(String sagaId);
    
    /**
     * 根据 Saga ID 查询所有步骤执行记录，按完成时间倒序
     * 用于获取补偿顺序
     */
    List<SagaStepExecution> findBySagaIdOrderByCompletedAtDesc(String sagaId);
    
    /**
     * 根据 Saga ID 和状态查询步骤执行记录
     */
    List<SagaStepExecution> findBySagaIdAndStatus(String sagaId, StepStatus status);
    
    /**
     * 根据 Saga ID 查询所有步骤执行记录，按步骤顺序排序
     */
    List<SagaStepExecution> findBySagaIdOrderByStepOrderAsc(String sagaId);
}
