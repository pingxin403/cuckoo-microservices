# 链路追踪快速参考

## 快速开始

### 1. 本地开发环境

```bash
# 1. 启动 Jaeger (使用 port-forward)
kubectl port-forward -n observability svc/jaeger-query 16686:16686 &
kubectl port-forward -n observability svc/jaeger-collector 4318:4318 &

# 2. 启动服务（开发环境配置）
cd cuckoo-microservices/cuckoo-order
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. 访问 Jaeger UI
open http://localhost:16686
```

### 2. 查看追踪数据

1. 打开 Jaeger UI: http://localhost:16686
2. 选择服务: `order-service`
3. 点击 "Find Traces"
4. 点击任意 Trace 查看详情

## 配置速查

### 环境变量

| 变量 | 开发环境 | 生产环境 |
|------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` |
| `DEPLOYMENT_ENV` | `development` | `production` |
| 采样率 | 100% | 10% |
| Jaeger Endpoint | `localhost:4318` | `jaeger-collector.observability.svc.cluster.local:4318` |

### 启动命令

```bash
# 开发环境
java -jar service.jar --spring.profiles.active=dev

# 生产环境
java -jar service.jar --spring.profiles.active=prod

# 使用环境变量
export SPRING_PROFILES_ACTIVE=dev
java -jar service.jar
```

## 代码示例

### 自动追踪（无需代码）

以下组件自动追踪，无需额外代码：

- ✅ HTTP 请求 (Spring MVC)
- ✅ 数据库查询 (JDBC/JPA)
- ✅ Kafka 消息
- ✅ OpenFeign 调用
- ✅ RestTemplate 调用

### 手动添加 Span

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public void processOrder(Order order) {
        Span span = tracer.spanBuilder("processOrder")
            .setAttribute("order.id", order.getId())
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            // 业务逻辑
            doSomething();
        } finally {
            span.end();
        }
    }
}
```

### 添加 Span 属性

```java
Span currentSpan = Span.current();
currentSpan.setAttribute("user.id", userId);
currentSpan.setAttribute("order.amount", amount);
currentSpan.setAttribute("payment.method", "credit_card");
```

### 记录事件

```java
Span.current().addEvent("Order validated");
Span.current().addEvent("Payment processed");
```

### 记录异常

```java
try {
    processPayment();
} catch (Exception e) {
    Span.current().recordException(e);
    throw e;
}
```

## 日志集成

### 查看 TraceId

日志中自动包含 TraceId 和 SpanId：

```
2024-01-15 10:30:45.123 [http-nio-8084-exec-1] INFO  OrderController [traceId=abc123, spanId=def456] - Creating order
```

### 使用 TraceId 关联日志和追踪

1. 从日志中复制 TraceId
2. 在 Jaeger UI 中搜索该 TraceId
3. 查看完整的调用链

## 常用 Jaeger 查询

### 按服务查询

```
Service: order-service
Operation: All
Lookback: Last 1 hour
```

### 按时间范围查询

```
Service: order-service
Lookback: Custom Time Range
From: 2024-01-15 10:00
To: 2024-01-15 11:00
```

### 按标签查询

```
Service: order-service
Tags: http.status_code=500
```

### 按最小持续时间查询

```
Service: order-service
Min Duration: 1s
```

## 故障排查

### 看不到追踪数据？

```bash
# 1. 检查 Jaeger Collector
kubectl get pods -n observability | grep jaeger-collector

# 2. 检查服务配置
kubectl logs <pod-name> | grep -i opentelemetry

# 3. 测试连接
curl -v http://localhost:4318/v1/traces

# 4. 检查采样率
# 确认 management.tracing.sampling.probability 不为 0
```

### TraceId 在日志中缺失？

```bash
# 1. 检查 Logback 配置
# 确认包含 %X{traceId} 和 %X{spanId}

# 2. 检查 Spring Profile
echo $SPRING_PROFILES_ACTIVE

# 3. 重启服务
```

### 采样率不生效？

```bash
# 1. 检查 Profile
kubectl exec -it <pod-name> -- env | grep SPRING_PROFILES_ACTIVE

# 2. 检查配置文件
# application-dev.yml: probability: 1.0
# application-prod.yml: probability: 0.1
```

## 性能影响

| 组件 | 开销 | 说明 |
|------|------|------|
| CPU | 1-5% | 每个 Span 的处理开销 |
| 内存 | 1-2 KB/Span | Span 数据结构 |
| 网络 | 最小 | 批量异步发送 |
| 延迟 | <1ms | 异步处理，不阻塞业务 |

## 最佳实践

### ✅ 推荐

- 生产环境使用 10% 采样率
- 为 Span 添加有意义的属性
- 记录关键业务事件
- 捕获并记录异常
- 使用清晰的 Span 命名

### ❌ 避免

- 在生产环境使用 100% 采样率
- 添加敏感信息到 Span 属性（如密码、token）
- 创建过多的细粒度 Span
- 忘记调用 span.end()
- 在循环中创建 Span

## 相关端点

| 服务 | 端点 | 用途 |
|------|------|------|
| Jaeger UI | http://localhost:16686 | 查看追踪数据 |
| Jaeger Collector | http://localhost:4318 | 接收 OTLP 数据 |
| Service Health | http://localhost:8084/actuator/health | 健康检查 |
| Service Metrics | http://localhost:8084/actuator/prometheus | Prometheus 指标 |

## 有用的命令

```bash
# 查看 Jaeger 服务
kubectl get svc -n observability

# 查看 Jaeger Pods
kubectl get pods -n observability

# 查看 Collector 日志
kubectl logs -n observability -l app=jaeger,component=collector

# 查看 Query 日志
kubectl logs -n observability -l app=jaeger,component=query

# Port Forward Jaeger UI
kubectl port-forward -n observability svc/jaeger-query 16686:16686

# Port Forward Collector
kubectl port-forward -n observability svc/jaeger-collector 4318:4318

# 检查服务追踪配置
kubectl exec -it <pod-name> -- env | grep -E "OTEL|SPRING_PROFILES"
```

## 更多信息

- 详细配置: [OPENTELEMETRY_CONFIGURATION.md](OPENTELEMETRY_CONFIGURATION.md)
- Jaeger 部署: [../k8s/jaeger/README.md](../k8s/jaeger/README.md)
- 快速开始: [../k8s/jaeger/QUICK_START.md](../k8s/jaeger/QUICK_START.md)
