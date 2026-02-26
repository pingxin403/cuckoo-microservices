package com.pingxin403.cuckoo.gateway.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RequestLoggingFilter 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private RequestLoggingFilter filter;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        
        // 设置日志捕获
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        logger.setLevel(ch.qos.logback.classic.Level.INFO); // Set to INFO to capture logs
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    @Test
    void testFilter_ShouldAddRequestIdToRequestHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            String requestId = modifiedExchange.getRequest().getHeaders().getFirst("X-Request-Id");
            assertThat(requestId).isNotNull();
            assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
            return Mono.empty();
        });

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testFilter_ShouldAddRequestIdToResponseHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
        
        String requestId = exchange.getResponse().getHeaders().getFirst("X-Request-Id");
        assertThat(requestId).isNotNull();
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testFilter_ShouldLogRequestInfo() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When - block() to ensure reactive chain completes and logs are written
        filter.filter(exchange, chain).block();

        // Then
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(1);
        
        ILoggingEvent requestLog = logEvents.get(0);
        assertThat(requestLog.getFormattedMessage()).contains("请求开始");
        assertThat(requestLog.getFormattedMessage()).contains("method=GET");
        assertThat(requestLog.getFormattedMessage()).contains("path=/api/products/1");
        assertThat(requestLog.getFormattedMessage()).contains("ip=192.168.1.1");
    }

    @Test
    void testFilter_ShouldLogResponseInfo() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When - block() to ensure reactive chain completes and logs are written
        filter.filter(exchange, chain).block();

        // Then
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(2);
        
        ILoggingEvent responseLog = logEvents.get(1);
        assertThat(responseLog.getFormattedMessage()).contains("请求结束");
        assertThat(responseLog.getFormattedMessage()).contains("status=200");
        assertThat(responseLog.getFormattedMessage()).contains("duration=");
    }

    @Test
    void testFilter_ShouldExtractIpFromXForwardedFor() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When - block() to ensure reactive chain completes and logs are written
        filter.filter(exchange, chain).block();

        // Then
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(1);
        ILoggingEvent requestLog = logEvents.get(0);
        assertThat(requestLog.getFormattedMessage()).contains("ip=203.0.113.1");
    }

    @Test
    void testFilter_ShouldExtractIpFromXRealIp() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/products/1")
            .header("X-Real-IP", "203.0.113.1")
            .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When - block() to ensure reactive chain completes and logs are written
        filter.filter(exchange, chain).block();

        // Then
        List<ILoggingEvent> logEvents = logAppender.list;
        assertThat(logEvents).hasSizeGreaterThanOrEqualTo(1);
        ILoggingEvent requestLog = logEvents.get(0);
        assertThat(requestLog.getFormattedMessage()).contains("ip=203.0.113.1");
    }

    @Test
    void testFilterOrder_ShouldBeThirdPriority() {
        // When
        int order = filter.getOrder();

        // Then
        assertThat(order).isEqualTo(-80);
    }
}
