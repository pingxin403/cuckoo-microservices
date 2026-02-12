package com.pingxin403.cuckoo.gateway.filter;

import com.pingxin403.cuckoo.gateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 * 负责验证 JWT Token 并提取用户信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    // 白名单路径（无需认证）
    private static final List<String> WHITELIST = Arrays.asList(
        "/api/users/register",
        "/api/users/login",
        "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 白名单路径直接放行
        if (WHITELIST.stream().anyMatch(path::startsWith)) {
            log.debug("白名单路径，直接放行: {}", path);
            return chain.filter(exchange);
        }

        // 提取 JWT Token
        String token = extractToken(exchange.getRequest());
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("JWT Token 无效或缺失: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 提取用户信息并传递给下游服务
        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId.toString())
                .build();

            log.debug("JWT 认证成功: userId={}, path={}", userId, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.error("提取用户信息失败: path={}", path, e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 从请求头中提取 JWT Token
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -100;  // 最高优先级
    }
}
