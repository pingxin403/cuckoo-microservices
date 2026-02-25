package com.pingxin403.cuckoo.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    /**
     * 异步保存审计日志
     * 使用新事务，不影响业务操作
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(AuditLog auditLog) {
        try {
            // 设置服务名称
            if (auditLog.getServiceName() == null) {
                auditLog.setServiceName(serviceName);
            }
            
            // 设置链路追踪ID
            if (auditLog.getTraceId() == null) {
                String traceId = MDC.get("traceId");
                if (traceId != null) {
                    auditLog.setTraceId(traceId);
                }
            }
            
            auditLogRepository.save(auditLog);
            log.debug("审计日志保存成功: {}", auditLog);
        } catch (Exception e) {
            // 审计日志保存失败不应影响业务，只记录错误日志
            log.error("审计日志保存失败", e);
        }
    }

    /**
     * 构建审计日志
     */
    public AuditLog.AuditLogBuilder buildAuditLog(
            AuditLog.OperationType operationType,
            Long userId,
            String username) {
        return AuditLog.builder()
                .operationType(operationType)
                .userId(userId)
                .username(username)
                .serviceName(serviceName)
                .traceId(MDC.get("traceId"));
    }

    /**
     * 将对象转换为JSON字符串
     */
    public String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("对象转JSON失败", e);
            return obj.toString();
        }
    }

    /**
     * 根据用户ID查询审计日志
     */
    public Page<AuditLog> findByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 根据操作类型查询审计日志
     */
    public Page<AuditLog> findByOperationType(AuditLog.OperationType operationType, Pageable pageable) {
        return auditLogRepository.findByOperationType(operationType, pageable);
    }

    /**
     * 根据时间范围查询审计日志
     */
    public Page<AuditLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(startTime, endTime, pageable);
    }

    /**
     * 根据用户ID和时间范围查询审计日志
     */
    public Page<AuditLog> findByUserIdAndTimeRange(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable) {
        return auditLogRepository.findByUserIdAndTimeRange(userId, startTime, endTime, pageable);
    }

    /**
     * 根据资源查询审计日志
     */
    public List<AuditLog> findByResource(String resourceType, String resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    /**
     * 根据链路追踪ID查询审计日志
     */
    public List<AuditLog> findByTraceId(String traceId) {
        return auditLogRepository.findByTraceId(traceId);
    }
}
