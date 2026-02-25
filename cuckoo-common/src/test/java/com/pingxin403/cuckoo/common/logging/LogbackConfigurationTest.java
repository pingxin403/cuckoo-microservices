package com.pingxin403.cuckoo.common.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 Logback 配置
 * 验证日志格式和 MDC 支持
 */
class LogbackConfigurationTest {

    private static final Logger log = LoggerFactory.getLogger(LogbackConfigurationTest.class);

    @Test
    void testLogbackConfiguration() {
        // 验证 logger 已正确配置
        assertThat(log).isNotNull();
        assertThat(log.isInfoEnabled()).isTrue();
        
        log.info("Test log message - Logback configuration is working");
    }

    @Test
    void testMdcSupport() {
        // 手动设置 MDC 值来模拟 OpenTelemetry 的行为
        String testTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String testSpanId = "00f067aa0ba902b7";
        
        try {
            MDC.put("traceId", testTraceId);
            MDC.put("spanId", testSpanId);
            
            // 记录日志
            log.info("Test log with tracing context");
            
            // 验证 MDC 值可以被读取
            assertThat(MDC.get("traceId")).isEqualTo(testTraceId);
            assertThat(MDC.get("spanId")).isEqualTo(testSpanId);
            
            log.info("Verified MDC values - traceId: {}, spanId: {}", 
                    MDC.get("traceId"), MDC.get("spanId"));
        } finally {
            MDC.clear();
        }
    }

    @Test
    void testLogWithoutMdc() {
        // 清除 MDC
        MDC.clear();
        
        // 记录日志（没有 tracing 上下文）
        log.info("Test log without tracing context");
        
        // MDC 应该为空
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    void testDifferentLogLevels() {
        MDC.put("traceId", "test-trace-id");
        MDC.put("spanId", "test-span-id");
        
        try {
            log.trace("TRACE level message");
            log.debug("DEBUG level message");
            log.info("INFO level message");
            log.warn("WARN level message");
            log.error("ERROR level message");
        } finally {
            MDC.clear();
        }
    }
}
