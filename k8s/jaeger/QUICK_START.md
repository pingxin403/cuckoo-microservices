# Jaeger 快速开始指南

5 分钟快速部署 Jaeger 链路追踪系统。

## 一键部署

```bash
cd cuckoo-microservices/k8s/jaeger
chmod +x deploy.sh verify.sh
./deploy.sh
```

等待 2-3 分钟，直到所有组件启动完成。

## 验证部署

```bash
./verify.sh
```

你应该看到所有组件都显示 ✓ 标记。

## 访问 Jaeger UI

### 方法 1: Port Forward (推荐用于开发)

```bash
kubectl port-forward -n observability svc/jaeger-query 16686:16686
```

在浏览器打开: http://localhost:16686

### 方法 2: NodePort (用于测试)

```bash
# 获取访问地址
kubectl get svc jaeger-query -n observability
```

访问 `http://<node-ip>:30686`

## 配置应用发送追踪数据

### Spring Boot 应用

1. **添加依赖** (`pom.xml`):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

2. **配置** (`application.yml`):

```yaml
spring:
  application:
    name: order-service  # 你的服务名

management:
  tracing:
    sampling:
      probability: 1.0  # 开发环境 100% 采样
  otlp:
    tracing:
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318/v1/traces
```

3. **重启应用**

4. **发送请求并查看追踪**
   - 访问你的应用 API
   - 打开 Jaeger UI
   - 选择你的服务名
   - 点击 "Find Traces"

## 常用命令

```bash
# 查看所有组件
kubectl get all -n observability

# 查看 Collector 日志
kubectl logs -n observability -l app.kubernetes.io/component=collector --tail=50

# 查看 Query 日志
kubectl logs -n observability -l app.kubernetes.io/component=query --tail=50

# 查看 Elasticsearch 健康
kubectl exec -n observability elasticsearch-0 -- curl -s http://localhost:9200/_cluster/health?pretty

# 重启 Collector
kubectl rollout restart deployment -n observability -l app.kubernetes.io/component=collector
```

## 故障排查

### 问题: Elasticsearch Pod 无法启动

```bash
# 检查日志
kubectl logs -n observability elasticsearch-0

# 常见原因: vm.max_map_count 太小
# 在宿主机上执行:
sudo sysctl -w vm.max_map_count=262144
```

### 问题: 看不到追踪数据

1. 检查应用配置是否正确
2. 检查 Collector 是否运行:
   ```bash
   kubectl get pods -n observability -l app.kubernetes.io/component=collector
   ```
3. 检查 Collector 日志是否有错误:
   ```bash
   kubectl logs -n observability -l app.kubernetes.io/component=collector
   ```

### 问题: Jaeger UI 无法访问

```bash
# 检查 Query 服务
kubectl get svc jaeger-query -n observability

# 检查 Query Pod
kubectl get pods -n observability -l app.kubernetes.io/component=query
```

## 清理

```bash
chmod +x cleanup.sh
./cleanup.sh
```

## 下一步

- 阅读 [README.md](README.md) 了解详细配置
- 阅读 [DEPLOYMENT_SUMMARY.md](DEPLOYMENT_SUMMARY.md) 了解架构设计
- 配置生产环境的采样率和资源限制
- 集成到 CI/CD 流程
