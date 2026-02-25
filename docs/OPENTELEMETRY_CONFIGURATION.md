# OpenTelemetry 配置指南

本文档说明如何在 cuckoo-microservices 项目中配置和使用 OpenTelemetry 进行分布式链路追踪。

## 概述

项目已集成 OpenTelemetry SDK，通过 OTLP (OpenTelemetry Protocol) 协议将追踪数据发送到 Jaeger Collector。

### 关键特性

- ✅ 自动追踪 HTTP 请求和响应
- ✅ 自动追踪数据库查询
- ✅ 自动追踪 Kafka 消息发送和消费
- ✅ 自动追踪 OpenFeign 服务间调用
- ✅ 环境特定的采样率配置
- ✅ TraceId 和 SpanId 自动注入到日志

## 架构

```
┌─────────────┐     OTLP HTTP      ┌──────────────────┐
│  Services   │ ─────────────────> │ Jaeger Collector │
│ (微服务)     │   Port 4318        │  (observability) │
└─────────────┘                     └──────────────────┘
                                            │
                                            ▼
                                    ┌──────────────┐
                                    │ Elasticsearch│
                                    │   (存储)      │
                                    └──────────────┘
                                            │
                                            ▼
                                    ┌──────────────┐
                                    │ Jaeger Query │
                                    │   (UI查询)    │
                                    └──────────────┘
```

## 依赖配置

### 父 POM (pom.xml)

已在父 POM 中配置 OpenTelemetry 依赖版本管理：

```xml
<properties>
    <opentelemetry.version>1.36.0</opentelemetry.version>
    <opentelemetry-instrumentation.version>2.2.0</opentelemetry-instrumentation.version>
    <opentelemetry-spring-boot.version>2.2.0-alpha</opentelemetry-spring-boot.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <version>${opentelemetry.version}</version>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk</artifactId>
            <version>${opentelemetry.version}</version>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-otlp</artifactId>
            <version>${opentelemetry.version}</version>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry.instrumentation</groupId>
            <artifactId>opentelemetry-spring-boot-starter</artifactId>
            <version>${opentelemetry-spring-boot.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 服务 POM

所有微服务已添加 OpenTelemetry Spring Boot Starter：

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

## 配置文件

### 公共配置 (application-common.yml)

所有服务共享的基础配置：

```yaml
# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  tracing:
    sampling:
      probability: 1.0  # 默认 100% 采样，生产环境通过 profile 覆盖
  otlp:
    tracing:
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318/v1/traces

# OpenTelemetry 配置
otel:
  exporter:
    otlp:
      # OTLP HTTP endpoint for Jaeger Collector
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318
      # 使用 HTTP 协议而不是 gRPC
      protocol: http/protobuf
  traces:
    exporter: otlp
  metrics:
    exporter: none  # 暂不导出指标到 OTLP
  logs:
    exporter: none  # 暂不导出日志到 OTLP
  resource:
    attributes:
      deployment.environment: ${DEPLOYMENT_ENV:development}
```

### 开发环境配置 (application-dev.yml)

开发环境使用 100% 采样率和 localhost 端点：

```yaml
# Actuator 配置 - 开发环境
management:
  tracing:
    sampling:
      probability: 1.0  # 开发环境 100% 采样率

# OpenTelemetry 配置 - 开发环境
otel:
  exporter:
    otlp:
      # 开发环境使用 localhost
      endpoint: http://localhost:4318
  resource:
    attributes:
      deployment.environment: development
```

### 生产环境配置 (application-prod.yml)

生产环境使用 10% 采样率以减少性能开销：

```yaml
# Actuator 配置 - 生产环境
management:
  tracing:
    sampling:
      probability: 0.1  # 生产环境 10% 采样率

# OpenTelemetry 配置 - 生产环境
otel:
  resource:
    attributes:
      deployment.environment: production
```

### 服务特定配置

每个服务的 application.yml 中配置服务名称：

```yaml
# OpenTelemetry 服务名（覆盖公共配置）
otel:
  service:
    name: ${spring.application.name}
```

## 使用方式

### 1. 启动 Jaeger

确保 Jaeger 已部署到 Kubernetes：

```bash
cd cuckoo-microservices/k8s/jaeger
./deploy.sh
./verify.sh
```

### 2. 配置环境变量

#### 开发环境

使用 Spring Profile 激活开发配置：

```bash
# 方式 1: 启动参数
java -jar service.jar --spring.profiles.active=dev

# 方式 2: 环境变量
export SPRING_PROFILES_ACTIVE=dev
java -jar service.jar

# 方式 3: application.yml
spring:
  profiles:
    active: dev
```

#### 生产环境

```bash
# Kubernetes Deployment
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: DEPLOYMENT_ENV
    value: "production"
```

### 3. 访问 Jaeger UI

#### 本地开发 (Port Forward)

```bash
kubectl port-forward -n observability svc/jaeger-query 16686:16686
```

然后访问: http://localhost:16686

#### Kubernetes 集群内

```bash
# 获取 NodePort
kubectl get svc jaeger-query -n observability

# 访问
http://<node-ip>:<node-port>
```

### 4. 查看追踪数据

1. 打开 Jaeger UI
2. 在 "Service" 下拉框选择服务名称（如 `order-service`）
3. 点击 "Find Traces" 查看追踪记录
4. 点击具体的 Trace 查看详细的调用链和时间分布

## 自动追踪的组件

OpenTelemetry Spring Boot Starter 自动为以下组件添加追踪：

### HTTP 请求

- Spring MVC Controllers
- RestTemplate 调用
- WebClient 调用
- OpenFeign 客户端调用

### 数据库

- JDBC 查询
- JPA/Hibernate 操作

### 消息队列

- Kafka Producer
- Kafka Consumer

### 其他

- Spring @Async 异步方法
- Spring @Scheduled 定时任务

## 手动添加 Span

如果需要手动添加自定义 Span：

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrderService {
    
    @Autowired
    private Tracer tracer;
    
    public void processOrder(Order order) {
        // 创建自定义 Span
        Span span = tracer.spanBuilder("processOrder")
            .setAttribute("order.id", order.getId())
            .setAttribute("order.amount", order.getAmount())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // 业务逻辑
            validateOrder(order);
            saveOrder(order);
            publishEvent(order);
        } catch (Exception e) {
            // 记录异常
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## 日志集成

TraceId 和 SpanId 会自动注入到日志的 MDC (Mapped Diagnostic Context) 中。

### Logback 配置示例

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId}, spanId=%X{spanId}] - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### JSON 格式日志 (推荐用于生产环境)

使用 Logstash Logback Encoder：

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
</dependency>
```

```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
    </encoder>
</appender>
```

## 采样策略

### 采样率说明

- **开发环境 (100%)**: 捕获所有请求，便于调试和问题排查
- **生产环境 (10%)**: 只采样 10% 的请求，减少性能开销和存储成本

### 自定义采样策略

如果需要更复杂的采样策略（如基于 URL 或用户的采样），可以自定义 Sampler：

```java
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {
    
    @Bean
    public Sampler sampler() {
        // 自定义采样逻辑
        return Sampler.parentBased(
            Sampler.traceIdRatioBased(0.1)  // 10% 采样率
        );
    }
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
4. **监控 Collector**: 确保 Jaeger Collector 有足够的资源处理追踪数据

## 故障排查

### 问题 1: 看不到追踪数据

**检查清单**:

1. 确认 Jaeger Collector 正在运行
   ```bash
   kubectl get pods -n observability | grep jaeger-collector
   ```

2. 检查服务配置
   ```bash
   # 查看配置是否正确
   kubectl exec -it <pod-name> -- env | grep OTEL
   ```

3. 检查网络连接
   ```bash
   # 从服务 Pod 测试连接
   kubectl exec -it <pod-name> -- curl -v http://jaeger-collector.observability.svc.cluster.local:4318/v1/traces
   ```

4. 查看服务日志
   ```bash
   kubectl logs <pod-name> | grep -i "opentelemetry\|tracing"
   ```

### 问题 2: 采样率不生效

**解决方案**:

1. 确认 Spring Profile 已激活
   ```bash
   kubectl exec -it <pod-name> -- env | grep SPRING_PROFILES_ACTIVE
   ```

2. 检查配置优先级
   - 环境变量 > application-{profile}.yml > application-common.yml

### 问题 3: TraceId 在日志中缺失

**解决方案**:

1. 确认 Logback 配置包含 MDC
2. 确认请求经过了 Spring MVC 拦截器
3. 检查异步线程是否正确传播上下文

## 最佳实践

### 1. Span 命名

使用清晰、一致的 Span 名称：

```java
// ✅ 好的命名
span.updateName("OrderService.createOrder");
span.updateName("InventoryService.deductStock");

// ❌ 避免的命名
span.updateName("process");
span.updateName("doSomething");
```

### 2. 添加有意义的属性

```java
span.setAttribute("order.id", orderId);
span.setAttribute("order.amount", amount);
span.setAttribute("user.id", userId);
span.setAttribute("http.status_code", 200);
```

### 3. 记录异常

```java
try {
    // 业务逻辑
} catch (Exception e) {
    span.recordException(e);
    span.setStatus(StatusCode.ERROR, e.getMessage());
    throw e;
}
```

### 4. 使用 Span Events

记录关键事件：

```java
span.addEvent("Order validated");
span.addEvent("Inventory reserved");
span.addEvent("Payment processed");
```

## 相关文档

- [OpenTelemetry 官方文档](https://opentelemetry.io/docs/)
- [Jaeger 部署指南](../k8s/jaeger/README.md)
- [Jaeger 快速开始](../k8s/jaeger/QUICK_START.md)
- [Requirements 5.5, 5.7](../../.kiro/specs/microservice-evolution/requirements.md)

## 验证清单

- [ ] 所有服务已添加 OpenTelemetry 依赖
- [ ] 配置文件已更新（开发和生产环境）
- [ ] Jaeger 已部署并运行正常
- [ ] 可以在 Jaeger UI 中看到追踪数据
- [ ] TraceId 和 SpanId 出现在日志中
- [ ] 开发环境使用 100% 采样率
- [ ] 生产环境使用 10% 采样率
- [ ] 服务间调用可以看到完整的调用链
- [ ] 异常被正确标记和记录

## 下一步

- [ ] 集成 ELK 日志收集系统 (Task 5)
- [ ] 配置 Prometheus 监控 (Task 6)
- [ ] 实现 TraceId 传播到 Kafka 消息 (Task 4.3)
