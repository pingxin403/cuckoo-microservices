package com.pingxin403.cuckoo.gateway.filter;

import com.pingxin403.cuckoo.gateway.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * JwtAuthenticationFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Test
    void testWhitelistPath_ShouldPassThrough() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/users/register")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testValidToken_ShouldAddUserIdHeader() {
        // Given
        String validToken = "valid.jwt.token";
        Long userId = 123L;
        
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testInvalidToken_ShouldReturn401() {
        // Given
        String invalidToken = "invalid.jwt.token";
        
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testMissingToken_ShouldReturn401() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testFilterOrder_ShouldBeHighestPriority() {
        // When
        int order = filter.getOrder();

        // Then
        assertThat(order).isEqualTo(-100);
    }

    @Test
    void testActuatorHealthPath_ShouldPassThrough() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/actuator/health")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testLoginPath_ShouldPassThrough() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .post("/api/users/login")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }
}
