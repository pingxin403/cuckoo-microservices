package com.pingxin403.cuckoo.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 请求日志过滤器
 * 记录请求和响应信息，添加 X-Request-Id
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request);

        // 添加 X-Request-Id 到请求头
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-Request-Id", requestId)
            .build();

        // 添加 X-Request-Id 到响应头
        exchange.getResponse().getHeaders().add("X-Request-Id", requestId);

        // 记录请求日志
        log.info("请求开始: requestId={}, method={}, path={}, ip={}", 
            requestId, method, path, clientIp);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : 0;

                // 记录响应日志
                log.info("请求结束: requestId={}, method={}, path={}, status={}, duration={}ms",
                    requestId, method, path, statusCode, duration);
            }));
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
        }
        // 如果有多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public int getOrder() {
        return -80;  // 第三优先级，在 JWT 认证和限流之后
    }
}
