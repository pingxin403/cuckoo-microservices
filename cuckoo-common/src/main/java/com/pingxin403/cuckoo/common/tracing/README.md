# 分布式链路追踪实现

本文档说明 cuckoo-microservices 项目中的分布式链路追踪实现。

## 概述

项目使用 **OpenTelemetry** 实现分布式链路追踪，通过 OTLP 协议将追踪数据发送到 Jaeger。

### 关键特性

- ✅ **自动追踪 HTTP 请求**: Gateway 和服务间的 HTTP 调用自动生成和传播 traceId
- ✅ **自动追踪 RestTemplate**: 服务间调用自动传递追踪上下文
- ✅ **自动追踪 Kafka 消息**: 消息发送和消费自动传播 traceId
- ✅ **自动追踪数据库查询**: JDBC 和 JPA 操作自动记录
- ✅ **日志集成**: traceId 和 spanId 自动注入到日志 MDC

## 架构

```
┌─────────────┐     HTTP Request      ┌─────────────┐
│   Gateway   │ ───────────────────> │   Service   │
│             │   traceparent header  │             │
└─────────────┘                       └─────────────┘
      │                                      │
      │ traceId: abc123                     │ traceId: abc123
      │ spanId: span1                       │ spanId: span2
      │                                      │
      ▼                                      ▼
┌─────────────────────────────────────────────────┐
│              Jaeger Collector                    │
│         (observability namespace)                │
└─────────────────────────────────────────────────┘
```

## TraceId 传播机制

### 1. HTTP 请求传播

OpenTelemetry 自动在 HTTP 请求头中注入和提取追踪上下文：

**请求头格式（W3C Trace Context）**:
```
traceparent: 00-{trace-id}-{span-id}-{trace-flags}
```

**示例**:
```
traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
```

**传播流程**:
1. Gateway 收到请求，OpenTelemetry 自动生成 traceId 和 spanId
2. Gateway 调用下游服务时，自动在请求头中添加 `traceparent`
3. 下游服务接收请求，自动提取 traceId 并创建新的 spanId
4. 所有 Span 关联到同一个 traceId，形成完整的调用链

**自动支持的组件**:
- Spring MVC Controllers
- RestTemplate
- WebClient
- OpenFeign

### 2. RestTemplate 追踪

OpenTelemetry 自动为 RestTemplate 添加拦截器，无需手动配置。

**自动行为**:
```java
@Service
public class OrderService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public Inventory getInventory(Long productId) {
        // OpenTelemetry 自动：
        // 1. 创建 CLIENT Span
        // 2. 在请求头中添加 traceparent
        // 3. 记录请求 URL、方法、状态码
        // 4. 记录响应时间
        return restTemplate.getForObject(
            "http://inventory-service/api/inventory/" + productId,
            Inventory.class
        );
    }
}
```

**手动创建 Span（可选）**:
```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public void processOrder(Order order) {
        Span span = TracingUtils.startInternalSpan(tracer, "processOrder");
        try {
            TracingUtils.setAttribute(span, "order.id", order.getId());
            TracingUtils.setAttribute(span, "order.amount", order.getAmount());
            
            // 业务逻辑
            validateOrder(order);
            TracingUtils.addEvent(span, "Order validated");
            
            saveOrder(order);
            TracingUtils.addEvent(span, "Order saved");
            
        } catch (Exception e) {
            TracingUtils.recordException(span, e);
            throw e;
        } finally {
            TracingUtils.endSpan(span);
        }
    }
}
```

### 3. Kafka 消息追踪

OpenTelemetry 自动为 Kafka 生产者和消费者添加追踪。

**生产者自动行为**:
```java
@Service
public class OrderService {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    public void createOrder(OrderRequest request) {
        Order order = saveOrder(request);
        
        // OpenTelemetry 自动：
        // 1. 创建 PRODUCER Span
        // 2. 在 Kafka 消息头中添加 traceparent
        // 3. 记录 topic、partition、offset
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        eventPublisher.publish(event);
    }
}
```

**消费者自动行为**:
```java
@Component
public class PaymentEventConsumer extends AbstractEventConsumer<PaymentSuccessEvent> {
    
    @Override
    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void consume(PaymentSuccessEvent event) {
        // OpenTelemetry 自动：
        // 1. 从 Kafka 消息头提取 traceparent
        // 2. 创建 CONSUMER Span，关联到生产者的 traceId
        // 3. 记录 topic、partition、offset
        
        // 业务逻辑
        processPaymentSuccess(event);
    }
}
```

**Kafka 消息头格式**:
```
Headers:
  traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
```

**完整的 Kafka 追踪链**:
```
Order Service (Producer)
  └─> Span: kafka.send
      └─> traceId: abc123
      └─> spanId: span1
          │
          ▼ (Kafka Message with traceparent header)
          │
Payment Service (Consumer)
  └─> Span: kafka.receive
      └─> traceId: abc123  (same as producer)
      └─> spanId: span2
      └─> parentSpanId: span1
```

### 4. 日志集成

traceId 和 spanId 自动注入到 SLF4J MDC，可在日志中输出。

**Logback 配置**:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId}, spanId=%X{spanId}] - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

**日志输出示例**:
```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] INFO  c.p.c.order.OrderService [traceId=0af7651916cd43dd8448eb211c80319c, spanId=b7ad6b7169203331] - 创建订单: orderId=12345
```

**在代码中使用**:
```java
@Service
public class OrderService {
    
    public void createOrder(OrderRequest request) {
        // traceId 和 spanId 已自动注入到 MDC
        log.info("创建订单: userId={}", request.getUserId());
        
        // 也可以手动获取
        String traceId = TracingUtils.getCurrentTraceId();
        String spanId = TracingUtils.getCurrentSpanId();
        log.info("当前追踪: traceId={}, spanId={}", traceId, spanId);
    }
}
```

## 配置

### 1. 依赖配置

所有服务已自动包含 OpenTelemetry 依赖（通过父 POM）：

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

### 2. 应用配置

**公共配置 (application-common.yml)**:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 默认 100% 采样
  otlp:
    tracing:
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318/v1/traces

otel:
  service:
    name: ${spring.application.name}
  exporter:
    otlp:
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318
      protocol: http/protobuf
  traces:
    exporter: otlp
```

**开发环境 (application-dev.yml)**:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 采样

otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318
```

**生产环境 (application-prod.yml)**:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% 采样
```

### 3. 启动 Jaeger

```bash
cd cuckoo-microservices/k8s/jaeger
./deploy.sh
./verify.sh
```

### 4. 访问 Jaeger UI

```bash
# Port forward
kubectl port-forward -n observability svc/jaeger-query 16686:16686

# 访问
http://localhost:16686
```

## 使用示例

### 示例 1: 订单创建流程追踪

**请求流程**:
```
Client → Gateway → Order Service → Inventory Service
                 ↓
              Kafka (OrderCreatedEvent)
                 ↓
              Payment Service
```

**追踪结果**:
```
Trace ID: 0af7651916cd43dd8448eb211c80319c

├─ Gateway: POST /api/orders (span1)
│  └─ Order Service: POST /api/orders (span2)
│     ├─ Inventory Service: GET /api/inventory/123 (span3)
│     └─ Kafka Producer: order-events (span4)
│        └─ Payment Service: kafka.receive (span5)
│           └─ Payment Service: processPayment (span6)
```

### 示例 2: 手动创建 Span

```java
@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public void processComplexOrder(Order order) {
        Span span = TracingUtils.startInternalSpan(tracer, "processComplexOrder");
        
        try {
            // 添加业务属性
            TracingUtils.setAttribute(span, "order.id", order.getId());
            TracingUtils.setAttribute(span, "order.type", order.getType());
            TracingUtils.setAttribute(span, "order.amount", order.getAmount());
            
            // 步骤 1: 验证订单
            validateOrder(order);
            TracingUtils.addEvent(span, "Order validated");
            
            // 步骤 2: 检查库存
            checkInventory(order);
            TracingUtils.addEvent(span, "Inventory checked");
            
            // 步骤 3: 计算价格
            calculatePrice(order);
            TracingUtils.addEvent(span, "Price calculated");
            
            // 步骤 4: 保存订单
            saveOrder(order);
            TracingUtils.addEvent(span, "Order saved");
            
        } catch (Exception e) {
            // 记录异常
            TracingUtils.recordException(span, e);
            throw e;
        } finally {
            TracingUtils.endSpan(span);
        }
    }
}
```

### 示例 3: 异步操作追踪

```java
@Service
public class NotificationService {
    
    @Autowired
    private Tracer tracer;
    
    @Async
    public CompletableFuture<Void> sendNotificationAsync(String userId, String message) {
        // OpenTelemetry 自动传播追踪上下文到异步线程
        Span span = TracingUtils.startInternalSpan(tracer, "sendNotificationAsync");
        
        try {
            TracingUtils.setAttribute(span, "user.id", userId);
            TracingUtils.setAttribute(span, "message.type", "order_created");
            
            // 发送通知
            sendEmail(userId, message);
            TracingUtils.addEvent(span, "Email sent");
            
            sendSMS(userId, message);
            TracingUtils.addEvent(span, "SMS sent");
            
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            TracingUtils.recordException(span, e);
            throw e;
        } finally {
            TracingUtils.endSpan(span);
        }
    }
}
```

## 最佳实践

### 1. Span 命名

使用清晰、一致的 Span 名称：

```java
// ✅ 好的命名
TracingUtils.startInternalSpan(tracer, "OrderService.createOrder");
TracingUtils.startInternalSpan(tracer, "InventoryService.deductStock");
TracingUtils.startInternalSpan(tracer, "PaymentService.processPayment");

// ❌ 避免的命名
TracingUtils.startInternalSpan(tracer, "process");
TracingUtils.startInternalSpan(tracer, "doSomething");
```

### 2. 添加有意义的属性

```java
// 业务属性
TracingUtils.setAttribute(span, "order.id", orderId);
TracingUtils.setAttribute(span, "order.amount", amount);
TracingUtils.setAttribute(span, "user.id", userId);

// 技术属性
TracingUtils.setAttribute(span, "http.method", "POST");
TracingUtils.setAttribute(span, "http.url", "/api/orders");
TracingUtils.setAttribute(span, "http.status_code", 200);
```

### 3. 记录关键事件

```java
TracingUtils.addEvent(span, "Order validated");
TracingUtils.addEvent(span, "Inventory reserved");
TracingUtils.addEvent(span, "Payment processed");
TracingUtils.addEvent(span, "Notification sent");
```

### 4. 异常处理

```java
try {
    // 业务逻辑
} catch (Exception e) {
    // 记录异常到 Span
    TracingUtils.recordException(span, e);
    throw e;
} finally {
    // 确保 Span 被关闭
    TracingUtils.endSpan(span);
}
```

### 5. 避免过度追踪

不要为每个小方法都创建 Span，只追踪关键操作：

```java
// ✅ 追踪关键业务操作
public void createOrder(OrderRequest request) {
    Span span = TracingUtils.startInternalSpan(tracer, "createOrder");
    // ...
}

// ❌ 不要追踪简单的 getter/setter
public String getOrderId() {
    // 不需要创建 Span
    return orderId;
}
```

## 性能考虑

### 开销

- **CPU**: 每个 Span 约增加 1-5% CPU 开销
- **内存**: 每个 Span 约占用 1-2 KB 内存
- **网络**: 批量发送到 Collector，默认每 5 秒或 512 个 Span

### 优化建议

1. **生产环境使用低采样率**: 10% 通常足够发现问题
2. **批量导出**: 使用默认的批量导出配置
3. **异步发送**: OpenTelemetry 默认异步发送，不阻塞业务线程
4. **避免过度追踪**: 只追踪关键操作

## 故障排查

### 问题 1: 看不到追踪数据

**检查清单**:
1. 确认 Jaeger Collector 正在运行
2. 检查服务配置中的 OTLP endpoint
3. 检查网络连接
4. 查看服务日志中的 OpenTelemetry 相关信息

### 问题 2: TraceId 在日志中缺失

**解决方案**:
1. 确认 Logback 配置包含 MDC
2. 确认请求经过了 Spring MVC 拦截器
3. 检查异步线程是否正确传播上下文

### 问题 3: Kafka 消息追踪断链

**解决方案**:
1. 确认 OpenTelemetry Kafka instrumentation 已启用
2. 检查 Kafka 消息头是否包含 traceparent
3. 确认消费者正确提取追踪上下文

## 相关文档

- [OpenTelemetry 配置指南](../../../docs/OPENTELEMETRY_CONFIGURATION.md)
- [Jaeger 部署指南](../../../k8s/jaeger/README.md)
- [Requirements 5.1, 5.2, 5.3](../../../../.kiro/specs/microservice-evolution/requirements.md)

## 验证清单

- [x] OpenTelemetry 依赖已添加
- [x] 配置文件已更新
- [x] Jaeger 已部署
- [x] HTTP 请求自动追踪
- [x] RestTemplate 自动追踪
- [x] Kafka 消息自动追踪
- [x] TraceId 和 SpanId 注入到日志
- [x] 可以在 Jaeger UI 中看到完整的调用链

## Requirements 验证

- ✅ **Requirement 5.1**: Gateway 生成 traceId（OpenTelemetry 自动实现）
- ✅ **Requirement 5.2**: Service 提取和传递 traceId（OpenTelemetry 自动实现）
- ✅ **Requirement 5.3**: 创建新的 spanId 并关联到 traceId（OpenTelemetry 自动实现）
