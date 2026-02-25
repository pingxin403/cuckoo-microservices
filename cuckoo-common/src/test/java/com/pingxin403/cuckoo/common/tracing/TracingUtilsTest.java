package com.pingxin403.cuckoo.common.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TracingUtils 单元测试
 * 
 * 验证追踪工具类的基本功能：
 * 1. 创建不同类型的 Span
 * 2. 记录异常
 * 3. 添加事件
 * 4. 设置属性
 * 5. 获取当前 traceId 和 spanId
 */
class TracingUtilsTest {

    private InMemorySpanExporter spanExporter;
    private Tracer tracer;

    @BeforeEach
    void setUp() {
        // 创建内存中的 Span 导出器用于测试
        spanExporter = InMemorySpanExporter.create();
        
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build();
        
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();
        
        tracer = openTelemetry.getTracer("test-tracer");
    }

    @Test
    void shouldCreateServerSpan() {
        // When: 创建 SERVER Span
        Span span = TracingUtils.startServerSpan(tracer, "testOperation");
        span.end();
        
        // Then: 验证 Span 类型
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName()).isEqualTo("testOperation");
        assertThat(spans.get(0).getKind()).isEqualTo(SpanKind.SERVER);
    }

    @Test
    void shouldCreateClientSpan() {
        // When: 创建 CLIENT Span
        Span span = TracingUtils.startClientSpan(tracer, "httpCall");
        span.end();
        
        // Then: 验证 Span 类型
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName()).isEqualTo("httpCall");
        assertThat(spans.get(0).getKind()).isEqualTo(SpanKind.CLIENT);
    }

    @Test
    void shouldCreateInternalSpan() {
        // When: 创建 INTERNAL Span
        Span span = TracingUtils.startInternalSpan(tracer, "businessLogic");
        span.end();
        
        // Then: 验证 Span 类型
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getName()).isEqualTo("businessLogic");
        assertThat(spans.get(0).getKind()).isEqualTo(SpanKind.INTERNAL);
    }

    @Test
    void shouldRecordException() {
        // Given: 创建 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        
        // When: 记录异常
        Exception exception = new RuntimeException("Test error");
        TracingUtils.recordException(span, exception);
        span.end();
        
        // Then: 验证异常被记录
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(spans.get(0).getStatus().getDescription()).isEqualTo("Test error");
        assertThat(spans.get(0).getEvents()).isNotEmpty();
    }

    @Test
    void shouldAddEvent() {
        // Given: 创建 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        
        // When: 添加事件
        TracingUtils.addEvent(span, "Step 1 completed");
        TracingUtils.addEvent(span, "Step 2 completed");
        span.end();
        
        // Then: 验证事件被记录
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getEvents()).hasSize(2);
        assertThat(spans.get(0).getEvents().get(0).getName()).isEqualTo("Step 1 completed");
        assertThat(spans.get(0).getEvents().get(1).getName()).isEqualTo("Step 2 completed");
    }

    @Test
    void shouldSetStringAttribute() {
        // Given: 创建 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        
        // When: 设置字符串属性
        TracingUtils.setAttribute(span, "user.id", "user123");
        TracingUtils.setAttribute(span, "order.id", "order456");
        span.end();
        
        // Then: 验证属性被设置
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")))
            .isEqualTo("user123");
        assertThat(spans.get(0).getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("order.id")))
            .isEqualTo("order456");
    }

    @Test
    void shouldSetLongAttribute() {
        // Given: 创建 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        
        // When: 设置 long 属性
        TracingUtils.setAttribute(span, "order.amount", 12345L);
        span.end();
        
        // Then: 验证属性被设置
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getAttributes().get(io.opentelemetry.api.common.AttributeKey.longKey("order.amount")))
            .isEqualTo(12345L);
    }

    @Test
    void shouldSetBooleanAttribute() {
        // Given: 创建 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        
        // When: 设置 boolean 属性
        TracingUtils.setAttribute(span, "is.valid", true);
        span.end();
        
        // Then: 验证属性被设置
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getAttributes().get(io.opentelemetry.api.common.AttributeKey.booleanKey("is.valid")))
            .isEqualTo(true);
    }

    @Test
    void shouldGetCurrentTraceIdAndSpanId() {
        // Given: 创建并激活 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        
        try (Scope scope = span.makeCurrent()) {
            // When: 获取当前 traceId 和 spanId
            String traceId = TracingUtils.getCurrentTraceId();
            String spanId = TracingUtils.getCurrentSpanId();
            
            // Then: 验证可以获取到 ID
            assertThat(traceId).isNotNull().isNotEmpty();
            assertThat(spanId).isNotNull().isNotEmpty();
            
            // 验证 ID 格式（16 进制字符串）
            assertThat(traceId).matches("[0-9a-f]{32}");
            assertThat(spanId).matches("[0-9a-f]{16}");
        } finally {
            span.end();
        }
    }

    @Test
    void shouldCheckIfTracing() {
        // When: 没有活跃的 Span
        boolean isTracingBefore = TracingUtils.isTracing();
        
        // Then: 应该返回 false
        assertThat(isTracingBefore).isFalse();
        
        // When: 创建并激活 Span
        Span span = TracingUtils.startInternalSpan(tracer, "operation");
        try (Scope scope = span.makeCurrent()) {
            boolean isTracingDuring = TracingUtils.isTracing();
            
            // Then: 应该返回 true
            assertThat(isTracingDuring).isTrue();
        } finally {
            span.end();
        }
        
        // When: Span 结束后
        boolean isTracingAfter = TracingUtils.isTracing();
        
        // Then: 应该返回 false
        assertThat(isTracingAfter).isFalse();
    }

    @Test
    void shouldHandleNullSpanGracefully() {
        // When: 对 null Span 执行操作
        TracingUtils.recordException(null, new RuntimeException("test"));
        TracingUtils.addEvent(null, "test event");
        TracingUtils.setAttribute(null, "key", "value");
        TracingUtils.endSpan(null);
        
        // Then: 不应该抛出异常
        // 测试通过即表示成功
    }

    @Test
    void shouldCreateNestedSpans() {
        // Given: 创建父 Span
        Span parentSpan = TracingUtils.startInternalSpan(tracer, "parentOperation");
        
        try (Scope parentScope = parentSpan.makeCurrent()) {
            // When: 创建子 Span
            Span childSpan = TracingUtils.startInternalSpan(tracer, "childOperation");
            childSpan.end();
        } finally {
            parentSpan.end();
        }
        
        // Then: 验证创建了两个 Span
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(2);
        
        // 验证父子关系
        SpanData childSpanData = spans.get(0);
        SpanData parentSpanData = spans.get(1);
        
        assertThat(childSpanData.getName()).isEqualTo("childOperation");
        assertThat(parentSpanData.getName()).isEqualTo("parentOperation");
        assertThat(childSpanData.getParentSpanId()).isEqualTo(parentSpanData.getSpanId());
        assertThat(childSpanData.getTraceId()).isEqualTo(parentSpanData.getTraceId());
    }
}
