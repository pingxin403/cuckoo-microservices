package com.pingxin403.cuckoo.common.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询控制器
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志查询接口")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 根据用户ID查询审计日志
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询审计日志")
    public ResponseEntity<Page<AuditLog>> findByUserId(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogService.findByUserId(userId, pageRequest);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据操作类型查询审计日志
     */
    @GetMapping("/operation/{operationType}")
    @Operation(summary = "根据操作类型查询审计日志")
    public ResponseEntity<Page<AuditLog>> findByOperationType(
            @Parameter(description = "操作类型") @PathVariable AuditLog.OperationType operationType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogService.findByOperationType(operationType, pageRequest);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据时间范围查询审计日志
     */
    @GetMapping("/time-range")
    @Operation(summary = "根据时间范围查询审计日志")
    public ResponseEntity<Page<AuditLog>> findByTimeRange(
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogService.findByTimeRange(startTime, endTime, pageRequest);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据用户ID和时间范围查询审计日志
     */
    @GetMapping("/user/{userId}/time-range")
    @Operation(summary = "根据用户ID和时间范围查询审计日志")
    public ResponseEntity<Page<AuditLog>> findByUserIdAndTimeRange(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "开始时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogService.findByUserIdAndTimeRange(userId, startTime, endTime, pageRequest);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据资源查询审计日志
     */
    @GetMapping("/resource")
    @Operation(summary = "根据资源查询审计日志")
    public ResponseEntity<List<AuditLog>> findByResource(
            @Parameter(description = "资源类型") @RequestParam String resourceType,
            @Parameter(description = "资源ID") @RequestParam String resourceId) {
        List<AuditLog> logs = auditLogService.findByResource(resourceType, resourceId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据链路追踪ID查询审计日志
     */
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "根据链路追踪ID查询审计日志")
    public ResponseEntity<List<AuditLog>> findByTraceId(
            @Parameter(description = "链路追踪ID") @PathVariable String traceId) {
        List<AuditLog> logs = auditLogService.findByTraceId(traceId);
        return ResponseEntity.ok(logs);
    }
}
