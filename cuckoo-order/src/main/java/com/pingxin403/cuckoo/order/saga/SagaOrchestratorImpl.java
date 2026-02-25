package com.pingxin403.cuckoo.order.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.order.entity.SagaInstance;
import com.pingxin403.cuckoo.order.entity.SagaInstance.SagaStatus;
import com.pingxin403.cuckoo.order.entity.SagaStepExecution;
import com.pingxin403.cuckoo.order.entity.SagaStepExecution.StepStatus;
import com.pingxin403.cuckoo.order.repository.SagaInstanceRepository;
import com.pingxin403.cuckoo.order.repository.SagaStepExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga 编排器实现
 * 负责 Saga 的执行、补偿和状态管理
 */
@Slf4j
@Service
public class SagaOrchestratorImpl implements SagaOrchestrator {
    
    @Autowired
    private SagaInstanceRepository sagaInstanceRepository;
    
    @Autowired
    private SagaStepExecutionRepository sagaStepExecutionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public String startSaga(SagaDefinition definition, Map<String, Object> initialContext) {
        // 1. 创建 Saga 实例
        String sagaId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutAt = now.plus(definition.getTimeout());
        
        SagaContext context = new SagaContext();
        context.putAll(initialContext);
        
        SagaInstance sagaInstance = SagaInstance.builder()
                .sagaId(sagaId)
                .sagaType(definition.getSagaType())
                .status(SagaStatus.RUNNING)
                .currentStep(0)
                .context(serializeContext(context))
                .executionLog("")
                .startedAt(now)
                .timeoutAt(timeoutAt)
                .build();
        
        sagaInstanceRepository.save(sagaInstance);
        
        log.info("Started Saga: sagaId={}, sagaType={}, timeout={}", 
                sagaId, definition.getSagaType(), timeoutAt);
        
        // 2. 异步执行 Saga
        executeSagaAsync(sagaId, definition, context);
        
        return sagaId;
    }
    
    /**
     * 异步执行 Saga
     */
    private void executeSagaAsync(String sagaId, SagaDefinition definition, SagaContext context) {
        new Thread(() -> {
            try {
                executeSaga(sagaId, definition, context);
            } catch (Exception e) {
                log.error("Saga execution failed: sagaId={}", sagaId, e);
            }
        }).start();
    }
    
    /**
     * 执行 Saga
     */
    private void executeSaga(String sagaId, SagaDefinition definition, SagaContext context) {
        List<SagaStep> steps = definition.getSteps();
        int currentStep = 0;
        
        while (currentStep < steps.size()) {
            SagaStep step = steps.get(currentStep);
            
            try {
                // 记录步骤开始
                recordStepStart(sagaId, step, currentStep);
                
                // 执行步骤
                log.info("Executing Saga step: sagaId={}, step={}, order={}", 
                        sagaId, step.getName(), currentStep);
                step.execute(context);
                
                // 记录步骤完成
                recordStepComplete(sagaId, step, currentStep);
                updateSagaStatus(sagaId, SagaStatus.RUNNING, currentStep + 1, context);
                
                currentStep++;
                
            } catch (Exception e) {
                log.error("Saga step failed: sagaId={}, step={}, order={}", 
                        sagaId, step.getName(), currentStep, e);
                
                // 记录步骤失败
                recordStepFailed(sagaId, step, currentStep, e.getMessage());
                
                // 触发补偿
                compensateSaga(sagaId, definition, context, currentStep);
                return;
            }
        }
        
        // 所有步骤执行成功
        completeSaga(sagaId, context);
    }
    
    @Override
    public SagaInstance getSagaStatus(String sagaId) {
        return sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga not found: " + sagaId));
    }
    
    @Override
    @Transactional
    public void compensate(String sagaId) {
        SagaInstance sagaInstance = getSagaStatus(sagaId);
        
        if (sagaInstance.getStatus() != SagaStatus.RUNNING) {
            log.warn("Cannot compensate Saga in status: {}", sagaInstance.getStatus());
            return;
        }
        
        // 获取 Saga 定义（这里简化处理，实际应该从注册表获取）
        // 触发补偿
        log.info("Manually triggering compensation for Saga: {}", sagaId);
        
        // 更新状态为补偿中
        sagaInstance.setStatus(SagaStatus.COMPENSATING);
        sagaInstanceRepository.save(sagaInstance);
        
        // 执行补偿逻辑
        compensateCompletedSteps(sagaId);
    }
    
    /**
     * 补偿 Saga
     */
    private void compensateSaga(String sagaId, SagaDefinition definition, 
                               SagaContext context, int failedStep) {
        log.info("Starting Saga compensation: sagaId={}, failedStep={}", sagaId, failedStep);
        
        // 更新状态为补偿中
        updateSagaStatus(sagaId, SagaStatus.COMPENSATING, failedStep, context);
        
        // 按照相反顺序补偿已完成的步骤
        for (int i = failedStep - 1; i >= 0; i--) {
            SagaStep step = definition.getStep(i);
            
            try {
                log.info("Compensating Saga step: sagaId={}, step={}, order={}", 
                        sagaId, step.getName(), i);
                step.compensate(context);
                
                // 记录补偿成功
                recordStepCompensated(sagaId, step, i);
                
            } catch (Exception e) {
                log.error("Compensation failed for step: sagaId={}, step={}, order={}", 
                        sagaId, step.getName(), i, e);
                
                // 补偿失败，标记为 FAILED
                failSaga(sagaId, context, "Compensation failed at step: " + step.getName());
                return;
            }
        }
        
        // 所有补偿成功
        compensatedSaga(sagaId, context);
    }
    
    /**
     * 补偿已完成的步骤（用于手动触发补偿）
     */
    private void compensateCompletedSteps(String sagaId) {
        // 获取所有已完成的步骤，按完成时间倒序
        List<SagaStepExecution> completedSteps = sagaStepExecutionRepository
                .findBySagaIdOrderByCompletedAtDesc(sagaId);
        
        for (SagaStepExecution stepExecution : completedSteps) {
            if (stepExecution.getStatus() == StepStatus.COMPLETED) {
                // 这里需要根据步骤名称找到对应的 SagaStep 并执行补偿
                // 简化处理，实际应该从注册表获取
                log.info("Compensating step: sagaId={}, step={}", 
                        sagaId, stepExecution.getStepName());
                
                stepExecution.setStatus(StepStatus.COMPENSATED);
                stepExecution.setCompletedAt(LocalDateTime.now());
                sagaStepExecutionRepository.save(stepExecution);
            }
        }
        
        // 更新 Saga 状态为已补偿
        SagaInstance sagaInstance = getSagaStatus(sagaId);
        sagaInstance.setStatus(SagaStatus.COMPENSATED);
        sagaInstance.setCompletedAt(LocalDateTime.now());
        sagaInstanceRepository.save(sagaInstance);
    }
    
    /**
     * 记录步骤开始
     */
    private void recordStepStart(String sagaId, SagaStep step, int stepOrder) {
        SagaStepExecution execution = SagaStepExecution.builder()
                .sagaId(sagaId)
                .stepName(step.getName())
                .stepOrder(stepOrder)
                .status(StepStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        
        sagaStepExecutionRepository.save(execution);
    }
    
    /**
     * 记录步骤完成
     */
    private void recordStepComplete(String sagaId, SagaStep step, int stepOrder) {
        List<SagaStepExecution> executions = sagaStepExecutionRepository
                .findBySagaIdAndStatus(sagaId, StepStatus.RUNNING);
        
        for (SagaStepExecution execution : executions) {
            if (execution.getStepName().equals(step.getName()) && 
                execution.getStepOrder() == stepOrder) {
                execution.setStatus(StepStatus.COMPLETED);
                execution.setCompletedAt(LocalDateTime.now());
                sagaStepExecutionRepository.save(execution);
                break;
            }
        }
    }
    
    /**
     * 记录步骤失败
     */
    private void recordStepFailed(String sagaId, SagaStep step, int stepOrder, String errorMessage) {
        List<SagaStepExecution> executions = sagaStepExecutionRepository
                .findBySagaIdAndStatus(sagaId, StepStatus.RUNNING);
        
        for (SagaStepExecution execution : executions) {
            if (execution.getStepName().equals(step.getName()) && 
                execution.getStepOrder() == stepOrder) {
                execution.setStatus(StepStatus.FAILED);
                execution.setCompletedAt(LocalDateTime.now());
                execution.setErrorMessage(errorMessage);
                sagaStepExecutionRepository.save(execution);
                break;
            }
        }
    }
    
    /**
     * 记录步骤已补偿
     */
    private void recordStepCompensated(String sagaId, SagaStep step, int stepOrder) {
        List<SagaStepExecution> executions = sagaStepExecutionRepository
                .findBySagaIdOrderByStepOrderAsc(sagaId);
        
        for (SagaStepExecution execution : executions) {
            if (execution.getStepName().equals(step.getName()) && 
                execution.getStepOrder() == stepOrder) {
                execution.setStatus(StepStatus.COMPENSATED);
                execution.setCompletedAt(LocalDateTime.now());
                sagaStepExecutionRepository.save(execution);
                break;
            }
        }
    }
    
    /**
     * 更新 Saga 状态
     */
    @Transactional
    private void updateSagaStatus(String sagaId, SagaStatus status, int currentStep, SagaContext context) {
        SagaInstance sagaInstance = getSagaStatus(sagaId);
        sagaInstance.setStatus(status);
        sagaInstance.setCurrentStep(currentStep);
        sagaInstance.setContext(serializeContext(context));
        sagaInstanceRepository.save(sagaInstance);
    }
    
    /**
     * 完成 Saga
     */
    @Transactional
    private void completeSaga(String sagaId, SagaContext context) {
        log.info("Saga completed successfully: sagaId={}", sagaId);
        
        SagaInstance sagaInstance = getSagaStatus(sagaId);
        sagaInstance.setStatus(SagaStatus.COMPLETED);
        sagaInstance.setCompletedAt(LocalDateTime.now());
        sagaInstance.setContext(serializeContext(context));
        sagaInstanceRepository.save(sagaInstance);
    }
    
    /**
     * Saga 补偿完成
     */
    @Transactional
    private void compensatedSaga(String sagaId, SagaContext context) {
        log.info("Saga compensated successfully: sagaId={}", sagaId);
        
        SagaInstance sagaInstance = getSagaStatus(sagaId);
        sagaInstance.setStatus(SagaStatus.COMPENSATED);
        sagaInstance.setCompletedAt(LocalDateTime.now());
        sagaInstance.setContext(serializeContext(context));
        sagaInstanceRepository.save(sagaInstance);
    }
    
    /**
     * Saga 失败
     */
    @Transactional
    private void failSaga(String sagaId, SagaContext context, String errorMessage) {
        log.error("Saga failed: sagaId={}, error={}", sagaId, errorMessage);
        
        SagaInstance sagaInstance = getSagaStatus(sagaId);
        sagaInstance.setStatus(SagaStatus.FAILED);
        sagaInstance.setCompletedAt(LocalDateTime.now());
        sagaInstance.setContext(serializeContext(context));
        sagaInstance.setExecutionLog(sagaInstance.getExecutionLog() + "\nError: " + errorMessage);
        sagaInstanceRepository.save(sagaInstance);
    }
    
    /**
     * 序列化上下文
     */
    private String serializeContext(SagaContext context) {
        try {
            return objectMapper.writeValueAsString(context.getData());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Saga context", e);
            return "{}";
        }
    }
}
