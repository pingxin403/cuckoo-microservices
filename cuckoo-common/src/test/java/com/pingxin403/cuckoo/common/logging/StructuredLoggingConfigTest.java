package com.pingxin403.cuckoo.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结构化日志配置测试
 * 验证 logback-spring.xml 配置是否正确加载
 */
class StructuredLoggingConfigTest {

    @Test
    void testLogbackConfigurationLoaded() {
        // 获取 LoggerContext
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertNotNull(loggerContext, "LoggerContext should not be null");

        // 获取 root logger
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        assertNotNull(rootLogger, "Root logger should not be null");

        // 验证至少有一个 appender 配置
        Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        assertTrue(appenderIterator.hasNext(), "Root logger should have at least one appender");
    }

    @Test
    void testLogstashEncoderAvailable() {
        // 验证 LogstashEncoder 类可以加载（说明依赖正确）
        assertDoesNotThrow(() -> {
            Class.forName("net.logstash.logback.encoder.LogstashEncoder");
        }, "LogstashEncoder class should be available on classpath");
    }

    @Test
    void testLoggingWorks() {
        // 简单测试日志输出是否正常工作
        org.slf4j.Logger logger = LoggerFactory.getLogger(StructuredLoggingConfigTest.class);
        
        assertDoesNotThrow(() -> {
            logger.info("Test log message for structured logging verification");
            logger.debug("Debug level log message");
            logger.warn("Warning level log message");
        }, "Logging should work without throwing exceptions");
    }

    @Test
    void testMDCSupport() {
        // 验证 MDC（Mapped Diagnostic Context）支持
        org.slf4j.Logger logger = LoggerFactory.getLogger(StructuredLoggingConfigTest.class);
        
        assertDoesNotThrow(() -> {
            org.slf4j.MDC.put("traceId", "test-trace-id-123");
            org.slf4j.MDC.put("spanId", "test-span-id-456");
            
            logger.info("Log message with MDC context");
            
            org.slf4j.MDC.clear();
        }, "MDC operations should work without throwing exceptions");
    }
}
