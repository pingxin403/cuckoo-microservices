package com.pingxin403.cuckoo.common.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作类型
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 用户名
     */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * 资源类型
     */
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    /**
     * 资源ID
     */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /**
     * 操作结果
     */
    @Column(name = "operation_result", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OperationResult operationResult;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 请求参数（JSON格式）
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 响应数据（JSON格式）
     */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 链路追踪ID
     */
    @Column(name = "trace_id", length = 100)
    private String traceId;

    /**
     * 服务名称
     */
    @Column(name = "service_name", length = 50)
    private String serviceName;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        LOGIN,              // 用户登录
        LOGOUT,             // 用户登出
        CREATE_ORDER,       // 创建订单
        CANCEL_ORDER,       // 取消订单
        UPDATE_PRODUCT,     // 修改商品
        DELETE_PRODUCT,     // 删除商品
        CREATE_USER,        // 创建用户
        UPDATE_USER,        // 修改用户
        DELETE_USER,        // 删除用户
        PAYMENT,            // 支付操作
        REFUND              // 退款操作
    }

    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS,    // 成功
        FAILURE     // 失败
    }
}
