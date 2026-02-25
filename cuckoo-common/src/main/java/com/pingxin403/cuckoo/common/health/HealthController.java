package com.pingxin403.cuckoo.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查端点
 * 提供 Kubernetes 探针使用的健康检查接口
 */
@Slf4j
@RestController
@RequestMapping("/actuator/health")
public class HealthController {
    
    @Autowired
    private HealthCheckService healthCheckService;
    
    /**
     * 存活探针端点
     * Kubernetes livenessProbe 使用此端点检查服务是否存活
     * 如果返回非 200 状态码，Kubernetes 会重启 Pod
     * 
     * @return 健康状态响应
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        HealthStatus status = healthCheckService.checkLiveness();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", status.name());
        
        int httpStatus = status == HealthStatus.UP ? 200 : 503;
        
        log.debug("Liveness check: {}", status);
        
        return ResponseEntity
            .status(httpStatus)
            .body(response);
    }
    
    /**
     * 就绪探针端点
     * Kubernetes readinessProbe 使用此端点检查服务是否就绪
     * 如果返回非 200 状态码，Kubernetes 不会将流量路由到此 Pod
     * 
     * @return 健康状态响应
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        HealthStatus status = healthCheckService.checkReadiness();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", status.name());
        
        int httpStatus = status == HealthStatus.UP ? 200 : 503;
        
        if (status == HealthStatus.DOWN) {
            log.warn("Readiness check failed: service not ready");
        } else {
            log.debug("Readiness check: {}", status);
        }
        
        return ResponseEntity
            .status(httpStatus)
            .body(response);
    }
}
