# 优雅下线组件

## 概述

优雅下线组件实现了微服务的优雅关闭流程，确保在服务停止时不丢失请求，保证用户体验。

## 功能特性

### 1. 完整的下线流程

1. **从 Nacos 注销服务** - 停止接收新的服务发现请求
2. **等待负载均衡器更新** - 给负载均衡器时间更新服务列表（默认 30 秒）
3. **停止接收新请求** - Tomcat 停止接受新的 HTTP 连接
4. **等待现有请求完成** - 等待正在处理的请求完成（最多 60 秒）
5. **关闭资源连接** - 依次关闭 Kafka、Redis、数据库连接

### 2. 资源清理

- **Kafka 生产者关闭** - 确保所有待发送消息都已发送
- **Redis 连接池关闭** - 释放 Redis 连接资源
- **数据库连接池关闭** - 释放数据库连接资源

### 3. 可配置参数

```yaml
graceful:
  shutdown:
    grace-period: 30  # 等待负载均衡器更新的时间（秒）
    max-wait: 60      # 等待现有请求完成的最大时间（秒）
```

## 使用方法

### 1. 启用优雅下线

在 `application.yml` 中配置：

```yaml
server:
  shutdown: graceful  # 启用优雅关闭

spring:
  lifecycle:
    timeout-per-shutdown-phase: 60s  # 每个关闭阶段的超时时间
```

### 2. 配置参数（可选）

```yaml
graceful:
  shutdown:
    grace-period: 30  # 自定义等待时间
    max-wait: 60      # 自定义最大等待时间
```

### 3. Kubernetes 配置

在 Deployment 中配置 preStop 钩子和终止宽限期：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: order-service
        image: order-service:latest
        
        # 优雅下线钩子
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 30"]
        
      # 终止宽限期（必须大于 preStop sleep 时间）
      terminationGracePeriodSeconds: 90
      
  # 滚动更新策略
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1  # 最多 1 个 Pod 不可用
      maxSurge: 1        # 最多多 1 个 Pod
```

## 工作原理

### 下线流程时序图

```
应用关闭信号
    ↓
从 Nacos 注销
    ↓
等待 30 秒（负载均衡器更新）
    ↓
停止接收新请求
    ↓
等待现有请求完成（最多 60 秒）
    ↓
关闭 Kafka 生产者
    ↓
关闭 Redis 连接池
    ↓
关闭数据库连接池
    ↓
应用退出
```

### Kubernetes 集成

1. **preStop 钩子** - 在容器停止前执行 `sleep 30`，给应用时间注销服务
2. **terminationGracePeriodSeconds** - Kubernetes 等待容器优雅关闭的时间
3. **滚动更新策略** - 确保至少 2 个 Pod 可用

## 验证需求

- **9.1** - 从 Nacos 注销服务
- **9.2** - 等待负载均衡器更新（30 秒）
- **9.3** - 停止接收新请求
- **9.4** - 等待现有请求完成
- **9.5** - 关闭数据库连接池
- **9.6** - 关闭 Redis 连接池
- **9.7** - 关闭 Kafka 生产者
- **9.8** - 配置 Kubernetes preStop 钩子
- **9.9** - 配置 terminationGracePeriodSeconds
- **9.10** - 滚动更新策略保证最少 2 个 Pod

## 日志示例

```
2024-01-15 10:30:00.000 INFO  --- [main] GracefulShutdownHandler : === Starting graceful shutdown ===
2024-01-15 10:30:00.100 INFO  --- [main] GracefulShutdownHandler : Deregistering from Nacos...
2024-01-15 10:30:00.200 INFO  --- [main] GracefulShutdownHandler : Successfully deregistered from Nacos
2024-01-15 10:30:00.300 INFO  --- [main] GracefulShutdownHandler : Waiting 30 seconds for load balancer to update...
2024-01-15 10:30:30.300 INFO  --- [main] GracefulShutdownHandler : Load balancer update grace period completed
2024-01-15 10:30:30.400 INFO  --- [main] GracefulShutdownHandler : Waiting for existing requests to complete (max 60 seconds)...
2024-01-15 10:30:35.400 INFO  --- [main] GracefulShutdownHandler : All requests completed successfully
2024-01-15 10:30:35.500 INFO  --- [main] GracefulShutdownHandler : Closing resource connections...
2024-01-15 10:30:35.600 INFO  --- [main] GracefulShutdownHandler : Closing Kafka producer...
2024-01-15 10:30:35.700 INFO  --- [main] GracefulShutdownHandler : Kafka producer closed successfully
2024-01-15 10:30:35.800 INFO  --- [main] GracefulShutdownHandler : Closing Redis connection pool...
2024-01-15 10:30:35.900 INFO  --- [main] GracefulShutdownHandler : Redis connection pool will be closed by Spring
2024-01-15 10:30:36.000 INFO  --- [main] GracefulShutdownHandler : Closing database connection pool...
2024-01-15 10:30:36.100 INFO  --- [main] GracefulShutdownHandler : HikariCP connection pool will be closed by Spring
2024-01-15 10:30:36.200 INFO  --- [main] GracefulShutdownHandler : All resource connections closed
2024-01-15 10:30:36.300 INFO  --- [main] GracefulShutdownHandler : === Graceful shutdown completed successfully ===
```

## 注意事项

1. **配置 Spring Boot 优雅关闭** - 必须设置 `server.shutdown=graceful`
2. **Kubernetes 终止宽限期** - `terminationGracePeriodSeconds` 应大于 `grace-period + max-wait`
3. **滚动更新策略** - 确保至少 2 个 Pod 可用，避免服务中断
4. **监控告警** - 监控优雅下线过程，如果经常超时需要调整参数

## 故障排查

### 问题：服务关闭时仍有请求失败

**原因**：负载均衡器更新时间不足

**解决**：增加 `grace-period` 参数，例如改为 45 秒

### 问题：服务关闭时间过长

**原因**：等待请求完成时间过长

**解决**：
1. 检查是否有长时间运行的请求
2. 减少 `max-wait` 参数
3. 优化业务逻辑，减少请求处理时间

### 问题：Kubernetes 强制杀死 Pod

**原因**：`terminationGracePeriodSeconds` 设置过小

**解决**：增加 `terminationGracePeriodSeconds`，建议设置为 90 秒或更长

## 相关文档

- [健康检查组件](../health/README.md)
- [服务预热](../health/README.md#服务预热)
- [Kubernetes 生命周期钩子](https://kubernetes.io/docs/concepts/containers/container-lifecycle-hooks/)
