package com.pingxin403.cuckoo.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 全局限流过滤器
 * 基于 IP 地址进行限流
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalRateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange.getRequest());
        String key = RATE_LIMIT_KEY_PREFIX + clientIp;

        return redisTemplate.opsForValue()
            .increment(key)
            .flatMap(count -> {
                if (count == 1) {
                    // 第一次请求，设置过期时间
                    return redisTemplate.expire(key, Duration.ofMinutes(1))
                        .then(processRequest(exchange, chain, count, clientIp));
                } else {
                    return processRequest(exchange, chain, count, clientIp);
                }
            })
            .onErrorResume(e -> {
                // Redis 异常时，降级处理：允许请求通过
                log.error("Redis 限流异常，降级处理: clientIp={}", clientIp, e);
                return chain.filter(exchange);
            });
    }

    private Mono<Void> processRequest(ServerWebExchange exchange, GatewayFilterChain chain, 
                                      Long count, String clientIp) {
        if (count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("请求超过限流阈值: clientIp={}, count={}, limit={}", 
                clientIp, count, MAX_REQUESTS_PER_MINUTE);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        log.debug("限流检查通过: clientIp={}, count={}, limit={}", 
            clientIp, count, MAX_REQUESTS_PER_MINUTE);
        return chain.filter(exchange);
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
        return -90;  // 次高优先级，在 JWT 认证之后
    }
}
