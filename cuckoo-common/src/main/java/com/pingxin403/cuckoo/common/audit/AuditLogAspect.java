package com.pingxin403.cuckoo.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 审计日志切面
 * 拦截标记了 @Auditable 注解的方法，记录审计日志
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(com.pingxin403.cuckoo.common.audit.Auditable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        String ipAddress = getIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        // 获取用户信息（从请求头或安全上下文中获取）
        Long userId = getUserId(request);
        String username = getUsername(request);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String requestParams = auditLogService.toJson(args);

        // 构建审计日志
        AuditLog.AuditLogBuilder logBuilder = auditLogService.buildAuditLog(
                auditable.value(),
                userId,
                username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestParams(requestParams)
                .resourceType(auditable.resourceType());

        Object result = null;
        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 记录成功日志
            logBuilder.operationResult(AuditLog.OperationResult.SUCCESS)
                    .responseData(auditLogService.toJson(result));

            // 尝试从结果中提取资源ID
            String resourceId = extractResourceId(result);
            if (resourceId != null) {
                logBuilder.resourceId(resourceId);
            }

            return result;
        } catch (Exception e) {
            // 记录失败日志
            logBuilder.operationResult(AuditLog.OperationResult.FAILURE)
                    .errorMessage(e.getMessage());
            throw e;
        } finally {
            // 异步保存审计日志
            auditLogService.saveAuditLog(logBuilder.build());
        }
    }

    /**
     * 获取 HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 从请求中获取用户ID
     * 实际项目中应从JWT token或Session中获取
     */
    private Long getUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // 从请求头获取用户ID（示例）
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID: {}", userIdHeader);
            }
        }
        
        return null;
    }

    /**
     * 从请求中获取用户名
     * 实际项目中应从JWT token或Session中获取
     */
    private String getUsername(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // 从请求头获取用户名（示例）
        return request.getHeader("X-Username");
    }

    /**
     * 从返回结果中提取资源ID
     * 尝试从常见的字段名中提取
     */
    private String extractResourceId(Object result) {
        if (result == null) {
            return null;
        }

        try {
            // 尝试获取 id 字段
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            // 忽略异常，返回null
            return null;
        }
    }
}
