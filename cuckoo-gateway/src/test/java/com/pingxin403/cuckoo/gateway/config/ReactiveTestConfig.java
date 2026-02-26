package com.pingxin403.cuckoo.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Gateway Reactive 测试配置
 * <p>
 * 为 Spring Cloud Gateway 的 Reactive 测试提供必要的 mock beans。
 * Gateway 使用 Reactive 技术栈（WebFlux），需要特殊的测试配置。
 * </p>
 *
 * @author pingxin403
 */
@TestConfiguration
public class ReactiveTestConfig {

    /**
     * 提供 ReactiveRedisConnectionFactory mock bean
     * <p>
     * 用于 Reactive Redis 操作，避免测试时连接真实 Redis 服务器。
     * </p>
     */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        ReactiveRedisConnectionFactory factory = mock(ReactiveRedisConnectionFactory.class);
        ReactiveRedisConnection connection = mock(ReactiveRedisConnection.class);
        
        // 配置默认行为
        when(factory.getReactiveConnection()).thenReturn(connection);
        
        return factory;
    }

    /**
     * 提供 ReactiveRedisTemplate mock bean
     * <p>
     * 用于限流等功能的 Redis 操作。配置默认的 mock 行为以支持测试。
     * </p>
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        ReactiveRedisTemplate<String, String> template = mock(ReactiveRedisTemplate.class);
        ReactiveValueOperations<String, String> valueOps = mock(ReactiveValueOperations.class);
        
        // 配置默认行为
        when(template.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));
        when(template.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        
        return template;
    }

    /**
     * 提供 ReactiveStringRedisTemplate mock bean
     * <p>
     * 用于 Gateway 的限流配置。Spring Cloud Gateway 的 RedisRateLimiter 需要此 bean。
     * </p>
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public org.springframework.data.redis.core.ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        return mock(org.springframework.data.redis.core.ReactiveStringRedisTemplate.class);
    }

    /**
     * 提供 WebClient.Builder mock bean
     * <p>
     * 用于服务间调用的 WebClient 构建。避免测试时进行真实的 HTTP 调用。
     * </p>
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient webClient = mock(WebClient.class);
        
        // 配置默认行为
        when(builder.build()).thenReturn(webClient);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
        
        return builder;
    }
}
