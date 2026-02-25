# Jaeger 链路追踪系统部署总结

## 部署概述

本次部署实现了完整的 Jaeger 分布式链路追踪系统，满足 Requirements 5.5 和 5.6 的要求。

## 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                        │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Observability Namespace                    │ │
│  │                                                          │ │
│  │  ┌──────────────┐      ┌──────────────┐               │ │
│  │  │   Jaeger     │      │   Jaeger     │               │ │
│  │  │  Collector   │◄─────┤    Agent     │               │ │
│  │  │  (x2 pods)   │      │ (DaemonSet)  │               │ │
│  │  └──────┬───────┘      └──────────────┘               │ │
│  │         │                                               │ │
│  │         │ Write Traces                                  │ │
│  │         ▼                                               │ │
│  │  ┌──────────────────────────────────┐                 │ │
│  │  │      Elasticsearch Cluster       │                 │ │
│  │  │  ┌────────┐ ┌────────┐ ┌────────┐│                 │ │
│  │  │  │  ES-0  │ │  ES-1  │ │  ES-2  ││                 │ │
│  │  │  └────────┘ └────────┘ └────────┘│                 │ │
│  │  └──────────────┬───────────────────┘                 │ │
│  │                 │ Read Traces                          │ │
│  │                 ▼                                       │ │
│  │  ┌──────────────────────────────────┐                 │ │
│  │  │       Jaeger Query (x2)          │                 │ │
│  │  │         (UI + API)               │                 │ │
│  │  └──────────────────────────────────┘                 │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```


### 数据流

1. **应用发送追踪数据** → Jaeger Agent (可选) 或直接到 Collector
2. **Collector 接收数据** → 验证、批处理、写入 Elasticsearch
3. **Elasticsearch 存储** → 索引追踪数据，保留 7 天
4. **Query 服务** → 从 Elasticsearch 读取数据，提供 UI 和 API

## 部署的组件

### 1. Elasticsearch 集群 (存储后端)

- **副本数**: 3 个 StatefulSet Pod
- **存储**: 每个节点 10GB PVC
- **内存**: 每个节点 2GB (JVM heap 1GB)
- **CPU**: 500m-1000m
- **集群名称**: jaeger-cluster
- **端口**: 9200 (HTTP), 9300 (Transport)

**特性**:
- 3 节点集群保证高可用
- 自动发现和集群形成
- 禁用安全特性 (开发环境)
- 健康检查和就绪探针

### 2. Jaeger Operator

- **副本数**: 1
- **作用**: 管理 Jaeger 组件的生命周期
- **CRD**: 定义 Jaeger 自定义资源
- **版本**: 1.51.0

**管理的资源**:
- Jaeger Collector Deployment
- Jaeger Query Deployment
- Jaeger Agent DaemonSet
- 相关的 Service 和 ConfigMap

### 3. Jaeger Collector

- **副本数**: 2 (可自动扩展到 5)
- **资源**: CPU 200m-500m, Memory 256Mi-512Mi
- **工作线程**: 50
- **队列大小**: 2000

**支持的协议**:
- OTLP gRPC (4317)
- OTLP HTTP (4318)
- Jaeger Thrift (14250)
- Zipkin (9411)

### 4. Jaeger Query

- **副本数**: 2
- **资源**: CPU 100m-300m, Memory 128Mi-256Mi
- **端口**: 16686 (HTTP)
- **NodePort**: 30686

**功能**:
- Web UI 界面
- REST API 查询
- 服务依赖图
- 追踪对比

### 5. Jaeger Agent (可选)

- **部署方式**: DaemonSet (每个节点一个)
- **资源**: CPU 100m-200m, Memory 128Mi-256Mi
- **作用**: 本地代理，减少应用直连 Collector 的网络开销

**支持的协议**:
- Compact Thrift (6831 UDP)
- Binary Thrift (6832 UDP)
- HTTP (5778)
- gRPC (14250)

## 配置详情

### 存储配置

```yaml
storage:
  type: elasticsearch
  options:
    es:
      server-urls: http://elasticsearch:9200
      index-prefix: jaeger
      num-shards: 3
      num-replicas: 1
  esIndexCleaner:
    enabled: true
    numberOfDays: 7
    schedule: "55 23 * * *"
```

**说明**:
- 使用 Elasticsearch 作为存储后端
- 索引前缀: `jaeger`
- 每个索引 3 个分片，1 个副本
- 每天 23:55 清理 7 天前的数据

### 采样策略

推荐配置:
- **开发环境**: 100% (probability: 1.0)
- **测试环境**: 50% (probability: 0.5)
- **生产环境**: 10% (probability: 0.1)

### 资源配置

| 组件 | CPU Request | CPU Limit | Memory Request | Memory Limit |
|------|-------------|-----------|----------------|--------------|
| Elasticsearch | 500m | 1000m | 2Gi | 2Gi |
| Collector | 200m | 500m | 256Mi | 512Mi |
| Query | 100m | 300m | 128Mi | 256Mi |
| Agent | 100m | 200m | 128Mi | 256Mi |

## 网络配置

### 服务端点

| 服务 | 类型 | 端口 | 用途 |
|------|------|------|------|
| jaeger-collector | ClusterIP | 4317 | OTLP gRPC |
| jaeger-collector | ClusterIP | 4318 | OTLP HTTP |
| jaeger-collector | ClusterIP | 14250 | Jaeger Thrift |
| jaeger-query | NodePort | 16686 | Web UI |
| jaeger-agent | ClusterIP | 6831 | Compact Thrift UDP |
| elasticsearch | ClusterIP | 9200 | HTTP API |

### 应用配置示例

Spring Boot 应用配置:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1
  otlp:
    tracing:
      endpoint: http://jaeger-collector.observability.svc.cluster.local:4318/v1/traces
```

## 部署脚本

### deploy.sh

自动化部署流程:
1. 检查 kubectl 和集群连接
2. 创建 observability namespace
3. 部署 Elasticsearch 集群
4. 等待 Elasticsearch 健康
5. 部署 Jaeger Operator
6. 部署 Jaeger 实例
7. 创建 Jaeger 服务
8. 显示访问信息

### verify.sh

验证部署状态:
1. 检查 namespace
2. 检查 Elasticsearch 集群 (3/3 运行)
3. 检查 Elasticsearch 健康状态
4. 检查 Jaeger Operator
5. 检查 Jaeger Collector
6. 检查 Jaeger Query
7. 检查 Jaeger Agent
8. 测试服务连接
9. 显示访问信息

### cleanup.sh

清理所有资源:
1. 删除 Jaeger 服务
2. 删除 Jaeger 实例
3. 删除 Jaeger Operator
4. 删除 Elasticsearch
5. 删除 PVC
6. 可选删除 namespace

## 验证清单

部署完成后验证:

- [ ] Elasticsearch 3 个 Pod 都在运行
- [ ] Elasticsearch 集群健康状态为 green 或 yellow
- [ ] Jaeger Operator 运行正常
- [ ] Jaeger Collector 至少 1 个 Pod 运行
- [ ] Jaeger Query 至少 1 个 Pod 运行
- [ ] 可以访问 Jaeger UI (http://localhost:16686)
- [ ] 应用可以发送追踪数据到 Collector
- [ ] Jaeger UI 中可以看到追踪数据

## 监控指标

建议监控的指标:

### Elasticsearch
- 集群健康状态
- 节点状态
- 索引大小
- 查询延迟

### Jaeger Collector
- 接收的 span 数量
- 处理延迟
- 队列大小
- 错误率

### Jaeger Query
- 查询延迟
- 请求数
- 错误率

## 故障排查

### 常见问题

1. **Elasticsearch Pod 无法启动**
   - 检查 vm.max_map_count 设置
   - 检查存储空间
   - 查看 Pod 日志

2. **Collector 无法连接 Elasticsearch**
   - 检查 Elasticsearch 服务
   - 检查网络策略
   - 查看 Collector 日志

3. **看不到追踪数据**
   - 检查应用配置
   - 检查采样率
   - 检查 Collector 日志
   - 测试网络连接

## 性能优化建议

1. **高流量场景**
   - 增加 Collector 副本数
   - 增加 Collector 资源限制
   - 调整队列大小和工作线程数

2. **存储优化**
   - 根据数据量调整保留天数
   - 增加 Elasticsearch 节点数
   - 调整分片和副本数

3. **查询优化**
   - 增加 Query 副本数
   - 使用索引模式优化查询
   - 限制查询时间范围

## 安全建议

1. **生产环境**
   - 启用 Elasticsearch 认证
   - 使用 Ingress + TLS
   - 配置 NetworkPolicy
   - 定期备份数据

2. **访问控制**
   - 限制 Jaeger UI 访问
   - 使用 RBAC 控制权限
   - 审计日志

## 下一步

1. **集成应用**
   - 配置 Spring Boot 应用
   - 添加 OpenTelemetry 依赖
   - 测试追踪数据

2. **监控告警**
   - 集成 Prometheus
   - 配置告警规则
   - 创建 Grafana 面板

3. **优化配置**
   - 根据实际流量调整采样率
   - 优化资源配置
   - 配置数据保留策略

## 参考文档

- [Requirements 5.5, 5.6](.kiro/specs/microservice-evolution/requirements.md)
- [Design Document](.kiro/specs/microservice-evolution/design.md)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
