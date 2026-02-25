# Jaeger 链路追踪系统部署

本目录包含在 Kubernetes 集群中部署 Jaeger 分布式链路追踪系统的所有配置文件和脚本。

## 架构概述

Jaeger 部署采用生产级配置，包含以下组件：

- **Elasticsearch**: 3 节点集群，作为追踪数据的存储后端
- **Jaeger Operator**: 管理 Jaeger 组件的生命周期
- **Jaeger Collector**: 2 个副本，接收追踪数据
- **Jaeger Query**: 2 个副本，提供 UI 和查询 API
- **Jaeger Agent**: DaemonSet 部署，每个节点一个实例

## 文件说明

| 文件 | 说明 |
|------|------|
| `namespace.yaml` | 创建 observability namespace |
| `elasticsearch-statefulset.yaml` | Elasticsearch 集群部署配置 |
| `jaeger-operator.yaml` | Jaeger Operator 部署配置 |
| `jaeger-instance.yaml` | Jaeger 实例定义 (CRD) |
| `jaeger-service.yaml` | Jaeger 服务定义 |
| `deploy.sh` | 一键部署脚本 |
| `verify.sh` | 验证部署状态脚本 |
| `cleanup.sh` | 清理脚本 |

## 快速开始

### 前置要求

- Kubernetes 集群 (v1.20+)
- kubectl 已配置并可访问集群
- 至少 6GB 可用内存
- 至少 30GB 可用存储空间

### 部署步骤

1. **一键部署**

```bash
chmod +x deploy.sh
./deploy.sh
```

2. **验证部署**

```bash
chmod +x verify.sh
./verify.sh
```

3. **访问 Jaeger UI**

通过 port-forward:
```bash
kubectl port-forward -n observability svc/jaeger-query 16686:16686
```

然后在浏览器访问: http://localhost:16686

或者通过 NodePort (生产环境建议使用 Ingress):
```bash
# 获取 NodePort
kubectl get svc jaeger-query -n observability

# 访问 http://<node-ip>:<node-port>
```

## 配置说明

### Elasticsearch 配置

- **副本数**: 3 个节点
- **存储**: 每个节点 10GB PVC
- **内存**: 每个节点 2GB
- **集群名称**: jaeger-cluster

### Jaeger Collector 配置

- **副本数**: 2 (可自动扩展到 5)
- **端口**:
  - 4317: OTLP gRPC
  - 4318: OTLP HTTP
  - 14250: Jaeger Thrift
  - 9411: Zipkin
- **资源限制**: 
  - CPU: 200m-500m
  - Memory: 256Mi-512Mi

### Jaeger Query 配置

- **副本数**: 2
- **端口**: 16686 (HTTP)
- **NodePort**: 30686
- **资源限制**:
  - CPU: 100m-300m
  - Memory: 128Mi-256Mi

### 数据保留策略

- **索引清理**: 每天 23:55 执行
- **保留天数**: 7 天
- **索引前缀**: jaeger
- **分片数**: 3
- **副本数**: 1

## 应用集成

### Spring Boot 应用配置

在 `application.yml` 中添加:

```yaml
spring:
  application:
    name: your-service-name

management:
  tracing:
    sampling:
      probability: 0.1  # 生产环境 10% 采样
  otlp:
    tracing:
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318/v1/traces
```

### 添加依赖

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

### 验证追踪

1. 发送请求到你的应用
2. 访问 Jaeger UI
3. 在 Service 下拉框选择你的服务
4. 点击 "Find Traces" 查看追踪数据

## 监控和维护

### 查看日志

```bash
# Collector 日志
kubectl logs -n observability -l app.kubernetes.io/component=collector --tail=100

# Query 日志
kubectl logs -n observability -l app.kubernetes.io/component=query --tail=100

# Elasticsearch 日志
kubectl logs -n observability -l app=elasticsearch --tail=100
```

### 查看资源使用

```bash
kubectl top pods -n observability
```

### 扩容 Collector

```bash
kubectl scale deployment -n observability -l app.kubernetes.io/component=collector --replicas=3
```

### 检查 Elasticsearch 健康

```bash
kubectl exec -n observability elasticsearch-0 -- curl -s http://localhost:9200/_cluster/health?pretty
```

### 查看索引

```bash
kubectl exec -n observability elasticsearch-0 -- curl -s http://localhost:9200/_cat/indices?v
```

## 故障排查

### Elasticsearch 无法启动

**症状**: Elasticsearch Pod 一直处于 Pending 或 CrashLoopBackOff

**可能原因**:
1. 存储空间不足
2. vm.max_map_count 设置不正确
3. 内存不足

**解决方案**:
```bash
# 检查 Pod 状态
kubectl describe pod -n observability elasticsearch-0

# 检查日志
kubectl logs -n observability elasticsearch-0

# 在宿主机上设置 vm.max_map_count
sudo sysctl -w vm.max_map_count=262144
```

### Jaeger Collector 无法连接 Elasticsearch

**症状**: Collector 日志显示连接错误

**解决方案**:
```bash
# 检查 Elasticsearch 服务
kubectl get svc -n observability elasticsearch

# 测试连接
kubectl run test --image=curlimages/curl --rm -i --restart=Never -n observability -- \
  curl -s http://elasticsearch:9200/_cluster/health
```

### 追踪数据未显示

**症状**: Jaeger UI 中看不到追踪数据

**检查清单**:
1. 应用是否正确配置了 OTLP 端点
2. Collector 是否正常运行
3. 采样率是否设置正确
4. 网络连接是否正常

```bash
# 检查 Collector 是否接收到数据
kubectl logs -n observability -l app.kubernetes.io/component=collector | grep "span"

# 测试发送追踪数据
kubectl run test-trace --image=curlimages/curl --rm -i --restart=Never -n observability -- \
  curl -X POST http://jaeger-collector:14268/api/traces \
  -H "Content-Type: application/json" \
  -d '{"data":[{"traceID":"test","spans":[{"traceID":"test","spanID":"test","operationName":"test"}]}]}'
```

## 性能优化

### 调整采样率

根据流量大小调整采样率:
- 开发环境: 100% (probability: 1.0)
- 测试环境: 50% (probability: 0.5)
- 生产环境: 10% (probability: 0.1)
- 高流量生产: 1% (probability: 0.01)

### 调整 Collector 资源

高流量场景下增加 Collector 资源:

```yaml
resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

### 调整 Elasticsearch 存储

根据追踪数据量调整存储和保留策略:

```yaml
storage:
  esIndexCleaner:
    numberOfDays: 7  # 根据需求调整保留天数
```

## 安全建议

1. **启用 Elasticsearch 认证**
   - 在生产环境中启用 xpack.security
   - 配置用户名和密码

2. **使用 Ingress 而非 NodePort**
   - 配置 TLS 证书
   - 添加认证中间件

3. **限制网络访问**
   - 使用 NetworkPolicy 限制访问
   - 只允许应用 namespace 访问 Collector

4. **定期备份**
   - 备份 Elasticsearch 数据
   - 备份 Jaeger 配置

## 清理

删除所有 Jaeger 组件:

```bash
chmod +x cleanup.sh
./cleanup.sh
```

**警告**: 这将删除所有追踪数据！

## 参考资料

- [Jaeger 官方文档](https://www.jaegertracing.io/docs/)
- [Jaeger Operator 文档](https://github.com/jaegertracing/jaeger-operator)
- [OpenTelemetry 文档](https://opentelemetry.io/docs/)
- [Elasticsearch 文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)

## 支持

如有问题，请查看:
1. Jaeger 日志
2. Elasticsearch 日志
3. Kubernetes Events
4. 本文档的故障排查部分
