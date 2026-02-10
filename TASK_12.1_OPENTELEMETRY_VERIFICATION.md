# Task 12.1: OpenTelemetry 链路追踪集成验证

## 实现内容

### 1. 父 POM 依赖配置

在 `cuckoo-microservices/pom.xml` 中添加了以下依赖：

- `opentelemetry-exporter-otlp`: OTLP 导出器，用于将追踪数据发送到 Jaeger
- `opentelemetry-spring-boot-starter`: Spring Boot 自动配置启动器

### 2. 各服务 POM 配置

为所有服务（user, product, inventory, order, payment, notification, gateway）添加了：

- `spring-boot-starter-actuator`: Spring Boot Actuator
- `micrometer-registry-prometheus`: Prometheus 指标注册表
- `opentelemetry-spring-boot-starter`: OpenTelemetry Spring Boot 启动器

### 3. 各服务 application.yml 配置

为所有服务添加了以下配置：

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
      probability: 1.0  # 100% 采样率

# OpenTelemetry 配置
otel:
  service:
    name: ${spring.application.name}  # 服务名称自动从 spring.application.name 获取
  exporter:
    otlp:
      endpoint: http://localhost:4317  # Jaeger OTLP gRPC 端点
  traces:
    exporter: otlp  # 使用 OTLP 导出器
  metrics:
    exporter: none  # 暂不导出指标到 OpenTelemetry
  logs:
    exporter: none  # 暂不导出日志到 OpenTelemetry
```

## 验证步骤

### 1. 启动基础设施

```bash
cd cuckoo-microservices
docker-compose up -d
```

确保 Jaeger 已启动并可访问：http://localhost:16686

### 2. 启动服务

启动任意一个或多个服务，例如：

```bash
# 启动用户服务
cd cuckoo-user
mvn spring-boot:run

# 启动订单服务
cd cuckoo-order
mvn spring-boot:run
```

### 3. 发送测试请求

```bash
# 测试用户服务
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 测试订单服务（需要先启动相关服务）
curl -X POST http://localhost:8084/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"skuId":1,"quantity":2}'
```

### 4. 查看 Jaeger UI

1. 访问 http://localhost:16686
2. 在 Service 下拉菜单中选择服务（如 `user-service`, `order-service`）
3. 点击 "Find Traces" 查看追踪数据
4. 点击具体的 trace 查看详细的调用链

### 5. 验证 TraceId 传播

对于跨服务调用（如订单服务调用商品服务和库存服务），应该能看到：

- 同一个请求的所有服务调用共享相同的 traceId
- 调用链清晰展示服务间的依赖关系
- 每个 span 包含服务名称、操作名称、时间戳等信息

### 6. 验证 Kafka 消息追踪

发送支付成功事件后，应该能看到：

- 支付服务发布事件的 span
- 订单服务、库存服务、通知服务消费事件的 span
- 这些 span 通过 traceId 关联在一起

## 预期结果

1. ✅ 所有服务成功启动，无 OpenTelemetry 相关错误
2. ✅ Jaeger UI 能够显示各服务的追踪数据
3. ✅ HTTP 请求的 traceId 自动传播
4. ✅ Kafka 消息的 traceId 自动传播
5. ✅ 跨服务调用链完整展示

## 技术说明

### OpenTelemetry 自动注入

OpenTelemetry Spring Boot Starter 会自动：

1. 为所有 HTTP 请求创建 span
2. 为所有 HTTP 客户端调用（包括 OpenFeign）传播 traceId
3. 为 Kafka 生产者和消费者传播 traceId
4. 为 JDBC 操作创建 span（如果启用）

### TraceId 传播机制

- **HTTP**: 通过 `traceparent` 或 `X-B3-TraceId` 等 HTTP 头传播
- **Kafka**: 通过 Kafka 消息头传播 traceId

### 采样率配置

当前配置为 100% 采样（`probability: 1.0`），适合开发和测试环境。生产环境建议降低采样率（如 0.1 表示 10%）以减少性能开销。

## 故障排查

### 问题 1: 服务启动失败

检查 OpenTelemetry 依赖是否正确添加：

```bash
mvn dependency:tree | grep opentelemetry
```

### 问题 2: Jaeger 无追踪数据

1. 检查 Jaeger 是否正常运行：`docker ps | grep jaeger`
2. 检查服务日志是否有 OpenTelemetry 错误
3. 确认 OTLP 端点配置正确：`http://localhost:4317`

### 问题 3: TraceId 未传播

1. 检查 OpenTelemetry 自动注入是否生效
2. 查看服务日志中的 traceId 字段
3. 确认 Kafka 消息头包含追踪信息

## 参考资料

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Spring Boot OpenTelemetry](https://opentelemetry.io/docs/instrumentation/java/automatic/spring-boot/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
