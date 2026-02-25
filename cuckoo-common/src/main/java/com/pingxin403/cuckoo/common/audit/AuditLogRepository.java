package com.pingxin403.cuckoo.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志仓储接口
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 根据用户ID查询审计日志
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据操作类型查询审计日志
     */
    Page<AuditLog> findByOperationType(AuditLog.OperationType operationType, Pageable pageable);

    /**
     * 根据时间范围查询审计日志
     */
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询审计日志
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.createdAt BETWEEN :startTime AND :endTime")
    Page<AuditLog> findByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * 根据资源类型和资源ID查询审计日志
     */
    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    /**
     * 根据链路追踪ID查询审计日志
     */
    List<AuditLog> findByTraceId(String traceId);
}
