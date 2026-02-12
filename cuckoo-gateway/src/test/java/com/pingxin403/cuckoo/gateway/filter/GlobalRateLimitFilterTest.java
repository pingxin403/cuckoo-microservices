package com.pingxin403.cuckoo.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * GlobalRateLimitFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
class GlobalRateLimitFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private GatewayFilterChain chain;

    private GlobalRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GlobalRateLimitFilter(redisTemplate);
    }

    @Test
    void testFirstRequest_ShouldPassThrough() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testWithinLimit_ShouldPassThrough() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(50L));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testExceedLimit_ShouldReturn429() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(101L));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void testRedisError_ShouldFallbackToAllowRequest() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.error(new RuntimeException("Redis error")));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testXForwardedForHeader_ShouldExtractCorrectIp() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq("rate_limit:203.0.113.1"))).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testFilterOrder_ShouldBeSecondPriority() {
        // When
        int order = filter.getOrder();

        // Then
        assertThat(order).isEqualTo(-90);
    }
}
