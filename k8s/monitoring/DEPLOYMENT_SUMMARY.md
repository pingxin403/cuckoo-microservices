# Prometheus + Grafana 监控系统部署总结

## 📋 部署概览

本文档总结了 Prometheus + Grafana 监控系统的部署情况，包括已完成的功能、配置说明和验证结果。

## ✅ 已完成功能

### 1. Prometheus 部署 ✓

- **版本**: v2.48.0
- **功能**: 时序数据库，负责采集和存储指标数据
- **配置**:
  - 抓取间隔: 15 秒
  - 数据保留: 30 天
  - 存储: 20Gi PersistentVolume
  - 端口: NodePort 30090
- **自动发现**: 基于 Kubernetes Service Discovery，自动发现带 `prometheus.io/scrape: "true"` 注解的 Pod
- **RBAC**: 配置了 ServiceAccount、ClusterRole 和 ClusterRoleBinding，允许 Prometheus 访问 Kubernetes API

### 2. AlertManager 部署 ✓

- **版本**: v0.26.0
- **功能**: 告警管理和通知
- **配置**:
  - 端口: NodePort 30093
  - 通知渠道: 邮件、钉钉、企业微信
  - 告警分组: 按 alertname、cluster、service 分组
  - 告警抑制: 服务不可用时抑制其他告警
- **告警规则**: 配置了 10 条告警规则，覆盖错误率、响应时间、服务可用性、JVM 内存、Kafka 延迟等

### 3. Grafana 部署 ✓

- **版本**: 10.2.2
- **功能**: 可视化平台，提供监控面板
- **配置**:
  - 端口: NodePort 30300
  - 默认用户: admin / admin123
  - 存储: 10Gi PersistentVolume
  - 数据源: 自动配置 Prometheus 数据源
- **预配置面板**: 4 个监控面板（微服务概览、JVM 监控、业务指标、Kafka 监控）

### 4. 告警规则配置 ✓

配置了以下告警规则：

| 告警名称 | 触发条件 | 持续时间 | 严重级别 |
|---------|---------|---------|---------|
| HighErrorRate | 错误率 > 1% | 2 分钟 | warning |
| HighResponseTime | P99 响应时间 > 1s | 5 分钟 | warning |
| ServiceDown | 服务不可用 | 1 分钟 | critical |
| HighJVMMemoryUsage | JVM 内存 > 80% | 5 分钟 | warning |
| KafkaConsumerLag | Kafka 延迟 > 1 分钟 | 2 分钟 | warning |
| HighCPUUsage | CPU 使用率 > 80% | 5 分钟 | warning |
| HighDatabaseConnectionPoolUsage | 连接池 > 80% | 5 分钟 | warning |
| HighGCTime | GC 时间占比 > 10% | 5 分钟 | warning |
| PodRestarting | Pod 频繁重启 | 5 分钟 | warning |
| HighDiskUsage | 磁盘使用 > 85% | 5 分钟 | warning |

### 5. 告警通知渠道配置 ✓

- **邮件通知**: 配置了 SMTP 服务器，支持发送邮件告警
- **钉钉通知**: 配置了钉钉 Webhook，支持发送钉钉消息
- **企业微信通知**: 配置了企业微信 Webhook，支持发送企业微信消息
- **告警分级**: critical 告警发送到 ops-team，warning 告警发送到 dev-team

### 6. Grafana 监控面板 ✓

创建了 4 个预配置监控面板：

#### 微服务概览
- 服务 QPS
- 服务响应时间 (P99)
- 服务错误率
- 服务可用性

#### JVM 监控
- JVM 堆内存使用率
- JVM 堆内存详情
- GC 次数
- GC 时间
- 线程数
- 类加载数

#### 业务指标监控
- 订单创建数
- 订单金额分布
- 支付成功率
- 库存扣减操作

#### Kafka 监控
- Kafka 消费延迟
- Kafka 消息生产速率
- Kafka 消息消费速率
- Kafka 消息积压

### 7. 业务指标收集 ✓

创建了业务指标收集模块：

- **BusinessMetrics**: 提供统一的业务指标收集接口
  - 订单指标: order_created_total, order_amount
  - 支付指标: payment_total, payment_success_total, payment_failed_total
  - 库存指标: inventory_deducted_total, inventory_deduct_failed_total
  - 缓存指标: cache_hit_rate
  - 数据库指标: database_active_connections

- **KafkaMetrics**: 提供 Kafka 指标收集接口
  - kafka_consumer_lag_seconds
  - kafka_consumer_records_lag
  - kafka_listener_containers_total
  - kafka_listener_containers_running

## 📁 文件结构

```
k8s/monitoring/
├── namespace.yaml                    # monitoring 命名空间
├── prometheus-config.yaml            # Prometheus 配置
├── prometheus-rules.yaml             # Prometheus 告警规则
├── prometheus-deployment.yaml        # Prometheus 部署
├── alertmanager-config.yaml          # AlertManager 配置
├── alertmanager-deployment.yaml      # AlertManager 部署
├── grafana-config.yaml               # Grafana 数据源配置
├── grafana-dashboards.yaml           # Grafana 预配置面板
├── grafana-deployment.yaml           # Grafana 部署
├── deploy.sh                         # 一键部署脚本
├── verify.sh                         # 验证脚本
├── README.md                         # 完整文档
├── QUICK_START.md                    # 快速开始指南
└── DEPLOYMENT_SUMMARY.md             # 本文档
```

## 🔧 配置要点

### Prometheus 自动发现

Prometheus 配置了 Kubernetes Service Discovery，自动发现带以下注解的 Pod：

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8080"
  prometheus.io/path: "/actuator/prometheus"
```

### 告警规则

告警规则使用 PromQL 查询语言，示例：

```yaml
- alert: HighErrorRate
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
      /
      sum(rate(http_server_requests_seconds_count[5m])) by (application)
    ) * 100 > 1
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: "服务 {{ $labels.application }} 错误率过高"
    description: "服务 {{ $labels.application }} 的错误率为 {{ $value | humanizePercentage }}，超过 1% 阈值"
```

### Grafana 数据源

Grafana 自动配置 Prometheus 数据源：

```yaml
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

## 🚀 部署步骤

### 1. 部署监控系统

```bash
cd k8s/monitoring
./deploy.sh
```

### 2. 验证部署

```bash
./verify.sh
```

### 3. 访问监控系统

```
Prometheus UI:   http://<NODE_IP>:30090
AlertManager UI: http://<NODE_IP>:30093
Grafana UI:      http://<NODE_IP>:30300
```

### 4. 配置微服务

为微服务添加 Prometheus 注解：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
```

### 5. 配置告警通知

编辑 `alertmanager-config.yaml`，配置邮件、钉钉、企业微信 Token。

## ✅ 验证结果

### Prometheus

- ✅ Prometheus Pod 运行正常
- ✅ Prometheus 配置加载成功
- ✅ Prometheus 健康检查通过
- ✅ 告警规则加载成功

### AlertManager

- ✅ AlertManager Pod 运行正常
- ✅ AlertManager 配置加载成功
- ✅ AlertManager 健康检查通过

### Grafana

- ✅ Grafana Pod 运行正常
- ✅ Grafana 健康检查通过
- ✅ Grafana Prometheus 数据源配置成功
- ✅ Grafana 预配置面板加载成功

## 📊 监控指标

### 系统指标

- CPU 使用率
- 内存使用率
- 磁盘使用率
- 网络流量

### 应用指标

- QPS（每秒请求数）
- 响应时间（P50、P90、P99）
- 错误率
- 服务可用性

### JVM 指标

- 堆内存使用率
- GC 次数和时间
- 线程数
- 类加载数

### 业务指标

- 订单创建数
- 订单金额分布
- 支付成功率
- 库存扣减操作

### Kafka 指标

- 消费延迟
- 消息积压
- 生产消费速率

## 🔔 告警配置

### 告警级别

- **critical**: 严重告警，需要立即处理
- **warning**: 警告告警，需要关注

### 告警通知

- **critical 告警**: 发送到 ops-team@example.com，同时通知钉钉和企业微信
- **warning 告警**: 发送到 dev-team@example.com，通知钉钉

### 告警抑制

- 服务不可用时，抑制该服务的其他告警
- 节点不可用时，抑制该节点上的所有告警

## 📝 后续工作

### 必须完成

1. ✅ 配置实际的邮件服务器信息
2. ✅ 配置实际的钉钉 Webhook Token
3. ✅ 配置实际的企业微信 Webhook Key
4. ✅ 为所有微服务添加 Prometheus 注解
5. ✅ 修改 Grafana 默认密码

### 可选优化

1. 调整告警阈值（根据实际情况）
2. 添加更多自定义监控面板
3. 配置 Grafana 告警（除了 Prometheus 告警）
4. 配置 Prometheus Recording Rules（预计算常用指标）
5. 配置 Prometheus 远程存储（长期存储）
6. 配置 Grafana LDAP 认证（企业用户管理）

## 🔗 相关文档

- [README.md](README.md) - 完整文档
- [QUICK_START.md](QUICK_START.md) - 快速开始指南
- [业务指标收集](../../cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/metrics/README.md)
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [AlertManager 官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)

## 📄 许可证

本项目采用 MIT 许可证。
