package com.pingxin403.cuckoo.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry 追踪配置
 * 
 * 功能：
 * 1. 配置 Tracer Bean 供手动创建 Span 使用
 * 2. 配置 TextMapPropagator 用于跨服务传播追踪上下文
 * 3. 提供追踪相关的工具方法
 * 
 * 自动追踪的组件（由 OpenTelemetry Spring Boot Starter 提供）：
 * - HTTP 请求和响应（Spring MVC、RestTemplate、WebClient、OpenFeign）
 * - 数据库查询（JDBC、JPA/Hibernate）
 * - Kafka 消息发送和消费
 * - Spring @Async 异步方法
 * - Spring @Scheduled 定时任务
 * 
 * Requirements: 5.1, 5.2, 5.3
 */
@Slf4j
@Configuration
public class TracingConfig {

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * 配置 Tracer Bean
     * 用于手动创建自定义 Span
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        log.info("配置 OpenTelemetry Tracer: serviceName={}", serviceName);
        return openTelemetry.getTracer(serviceName);
    }

    /**
     * 配置 TextMapPropagator Bean
     * 用于在服务间传播追踪上下文
     * 
     * OpenTelemetry 默认使用 W3C Trace Context 格式：
     * - traceparent: 00-{trace-id}-{span-id}-{trace-flags}
     * - tracestate: 可选的供应商特定追踪信息
     */
    @Bean
    public TextMapPropagator textMapPropagator(OpenTelemetry openTelemetry) {
        return openTelemetry.getPropagators().getTextMapPropagator();
    }
}
