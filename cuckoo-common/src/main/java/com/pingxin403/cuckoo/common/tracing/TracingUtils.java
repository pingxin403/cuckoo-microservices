package com.pingxin403.cuckoo.common.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 追踪工具类
 * 
 * 提供便捷的追踪操作方法：
 * 1. 获取当前 traceId 和 spanId
 * 2. 创建自定义 Span
 * 3. 记录 Span 事件和异常
 * 4. 设置 Span 属性
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4
 */
@Slf4j
public class TracingUtils {

    /**
     * 获取当前请求的 traceId
     * 
     * @return traceId，如果不存在返回 null
     */
    public static String getCurrentTraceId() {
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                return currentSpan.getSpanContext().getTraceId();
            }
        } catch (Exception e) {
            log.debug("无法获取当前 traceId", e);
        }
        
        // 尝试从 MDC 获取
        return MDC.get("traceId");
    }

    /**
     * 获取当前请求的 spanId
     * 
     * @return spanId，如果不存在返回 null
     */
    public static String getCurrentSpanId() {
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                return currentSpan.getSpanContext().getSpanId();
            }
        } catch (Exception e) {
            log.debug("无法获取当前 spanId", e);
        }
        
        // 尝试从 MDC 获取
        return MDC.get("spanId");
    }

    /**
     * 创建一个新的 Span
     * 
     * @param tracer Tracer 实例
     * @param operationName 操作名称
     * @param spanKind Span 类型
     * @return 新创建的 Span
     */
    public static Span startSpan(Tracer tracer, String operationName, SpanKind spanKind) {
        return tracer.spanBuilder(operationName)
            .setSpanKind(spanKind)
            .setParent(Context.current())
            .startSpan();
    }

    /**
     * 创建一个 SERVER 类型的 Span
     */
    public static Span startServerSpan(Tracer tracer, String operationName) {
        return startSpan(tracer, operationName, SpanKind.SERVER);
    }

    /**
     * 创建一个 CLIENT 类型的 Span
     */
    public static Span startClientSpan(Tracer tracer, String operationName) {
        return startSpan(tracer, operationName, SpanKind.CLIENT);
    }

    /**
     * 创建一个 INTERNAL 类型的 Span
     */
    public static Span startInternalSpan(Tracer tracer, String operationName) {
        return startSpan(tracer, operationName, SpanKind.INTERNAL);
    }

    /**
     * 记录 Span 异常
     * 
     * @param span Span 实例
     * @param exception 异常对象
     */
    public static void recordException(Span span, Throwable exception) {
        if (span != null && exception != null) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getMessage());
        }
    }

    /**
     * 添加 Span 事件
     * 
     * @param span Span 实例
     * @param eventName 事件名称
     */
    public static void addEvent(Span span, String eventName) {
        if (span != null && eventName != null) {
            span.addEvent(eventName);
        }
    }

    /**
     * 设置 Span 属性
     * 
     * @param span Span 实例
     * @param key 属性键
     * @param value 属性值
     */
    public static void setAttribute(Span span, String key, String value) {
        if (span != null && key != null && value != null) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 设置 Span 属性（long 类型）
     */
    public static void setAttribute(Span span, String key, long value) {
        if (span != null && key != null) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 设置 Span 属性（boolean 类型）
     */
    public static void setAttribute(Span span, String key, boolean value) {
        if (span != null && key != null) {
            span.setAttribute(key, value);
        }
    }

    /**
     * 结束 Span
     * 
     * @param span Span 实例
     */
    public static void endSpan(Span span) {
        if (span != null) {
            span.end();
        }
    }

    /**
     * 检查当前是否在追踪上下文中
     * 
     * @return true 如果存在有效的追踪上下文
     */
    public static boolean isTracing() {
        try {
            Span currentSpan = Span.current();
            return currentSpan != null && currentSpan.getSpanContext().isValid();
        } catch (Exception e) {
            return false;
        }
    }
}
