# Prometheus + Grafana 监控系统

本目录包含 cuckoo-microservices 项目的完整监控系统部署配置，基于 Prometheus + Grafana + AlertManager 构建。

## 📋 目录

- [架构概览](#架构概览)
- [组件说明](#组件说明)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [监控面板](#监控面板)
- [告警规则](#告警规则)
- [故障排查](#故障排查)

## 🏗️ 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                        │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Microservice │    │ Microservice │    │ Microservice │ │
│  │   Pod 1      │    │   Pod 2      │    │   Pod 3      │ │
│  │              │    │              │    │              │ │
│  │ /actuator/   │    │ /actuator/   │    │ /actuator/   │ │
│  │ prometheus   │    │ prometheus   │    │ prometheus   │ │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘ │
│         │                   │                   │          │
│         └───────────────────┼───────────────────┘          │
│                             │                               │
│                    ┌────────▼────────┐                     │
│                    │   Prometheus    │                     │
│                    │  (Metrics DB)   │                     │
│                    │                 │                     │
│                    │  - 15s scrape   │                     │
│                    │  - 30d retention│                     │
│                    │  - Alert rules  │                     │
│                    └────┬───────┬────┘                     │
│                         │       │                           │
│              ┌──────────┘       └──────────┐               │
│              │                              │               │
│     ┌────────▼────────┐          ┌─────────▼────────┐     │
│     │  AlertManager   │          │     Grafana      │     │
│     │                 │          │                  │     │
│     │  - Email        │          │  - Dashboards    │     │
│     │  - DingTalk     │          │  - Visualization │     │
│     │  - WeChat       │          │  - Alerting      │     │
│     └─────────────────┘          └──────────────────┘     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 📦 组件说明

### Prometheus
- **版本**: v2.48.0
- **功能**: 时序数据库，负责采集和存储指标数据
- **抓取间隔**: 15 秒
- **数据保留**: 30 天
- **存储**: 20Gi PersistentVolume
- **端口**: NodePort 30090

### Grafana
- **版本**: 10.2.2
- **功能**: 可视化平台，提供监控面板
- **默认用户**: admin / admin123
- **存储**: 10Gi PersistentVolume
- **端口**: NodePort 30300

### AlertManager
- **版本**: v0.26.0
- **功能**: 告警管理和通知
- **通知渠道**: 邮件、钉钉、企业微信
- **端口**: NodePort 30093

## 🚀 快速开始

### 前置条件

1. Kubernetes 集群（v1.20+）
2. kubectl 已配置并可访问集群
3. 集群支持 PersistentVolume（StorageClass: standard）
4. 微服务已部署并暴露 `/actuator/prometheus` 端点

### 一键部署

```bash
# 进入监控目录
cd k8s/monitoring

# 执行部署脚本
./deploy.sh
```

部署脚本会自动完成以下操作：
1. 创建 monitoring 命名空间
2. 部署 Prometheus（包括配置和告警规则）
3. 部署 AlertManager
4. 部署 Grafana（包括数据源和预配置面板）
5. 等待所有组件就绪
6. 显示访问信息

### 验证部署

```bash
# 执行验证脚本
./verify.sh
```

验证脚本会检查：
- 命名空间和 Pod 状态
- 服务健康检查
- Prometheus 抓取目标
- Grafana 数据源配置
- 告警规则加载

### 访问监控系统

部署完成后，通过以下地址访问：

```
Prometheus UI:   http://<NODE_IP>:30090
AlertManager UI: http://<NODE_IP>:30093
Grafana UI:      http://<NODE_IP>:30300
```

## ⚙️ 配置说明

### 微服务配置

确保微服务的 Deployment 添加了 Prometheus 注解：

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
    spec:
      containers:
        - name: order-service
          image: order-service:latest
          ports:
            - containerPort: 8080
```

### Prometheus 配置

Prometheus 配置文件位于 `prometheus-config.yaml`，主要配置项：

- **抓取间隔**: 15 秒
- **评估间隔**: 15 秒
- **自动发现**: 基于 Kubernetes Service Discovery
- **抓取目标**: 所有带 `prometheus.io/scrape: "true"` 注解的 Pod

### 告警规则配置

告警规则位于 `prometheus-rules.yaml`，包含以下规则：

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

### AlertManager 配置

AlertManager 配置位于 `alertmanager-config.yaml`，需要配置实际的通知渠道：

#### 邮件通知

```yaml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@example.com'
  smtp_auth_username: 'alertmanager@example.com'
  smtp_auth_password: 'your-password'
```

#### 钉钉通知

```yaml
webhook_configs:
  - url: 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_DINGTALK_TOKEN'
```

获取钉钉 Token：
1. 在钉钉群中添加自定义机器人
2. 复制 Webhook 地址中的 access_token
3. 替换配置中的 YOUR_DINGTALK_TOKEN

#### 企业微信通知

```yaml
webhook_configs:
  - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_WECHAT_KEY'
```

获取企业微信 Key：
1. 在企业微信群中添加群机器人
2. 复制 Webhook 地址中的 key
3. 替换配置中的 YOUR_WECHAT_KEY

## 📊 监控面板

Grafana 预配置了 4 个监控面板：

### 1. 微服务概览 (service-overview)

监控所有微服务的核心指标：
- **服务 QPS**: 每秒请求数
- **服务响应时间 (P99)**: 99 分位响应时间
- **服务错误率**: HTTP 5xx 错误占比
- **服务可用性**: 服务健康状态

### 2. JVM 监控 (jvm-monitoring)

监控 Java 应用的 JVM 指标：
- **JVM 堆内存使用率**: 堆内存使用百分比
- **JVM 堆内存详情**: Used/Committed/Max 内存
- **GC 次数**: 垃圾回收频率
- **GC 时间**: 垃圾回收耗时
- **线程数**: 活跃线程和峰值线程
- **类加载数**: 已加载的类数量

### 3. 业务指标监控 (business-metrics)

监控业务相关指标：
- **订单创建数**: 订单创建速率
- **订单金额分布**: P50/P90/P99 订单金额
- **支付成功率**: 支付成功百分比
- **库存扣减操作**: 扣减成功和失败次数

### 4. Kafka 监控 (kafka-monitoring)

监控 Kafka 消息队列：
- **Kafka 消费延迟**: 消费者延迟时间
- **Kafka 消息生产速率**: 每秒生产消息数
- **Kafka 消息消费速率**: 每秒消费消息数
- **Kafka 消息积压**: 未消费的消息数量

## 🔔 告警规则

### 告警级别

- **critical**: 严重告警，需要立即处理
- **warning**: 警告告警，需要关注

### 告警通知

- **critical 告警**: 发送到 ops-team@example.com，同时通知钉钉和企业微信
- **warning 告警**: 发送到 dev-team@example.com，通知钉钉

### 告警抑制

- 服务不可用时，抑制该服务的其他告警
- 节点不可用时，抑制该节点上的所有告警

## 🔧 故障排查

### Prometheus 无法抓取指标

**问题**: Prometheus Targets 页面显示目标为 DOWN

**排查步骤**:
1. 检查微服务 Pod 是否添加了 `prometheus.io/scrape: "true"` 注解
2. 检查微服务是否暴露了 `/actuator/prometheus` 端点
3. 检查微服务端口是否正确（通过 `prometheus.io/port` 注解指定）
4. 在 Prometheus Pod 中测试连接：
   ```bash
   kubectl exec -n monitoring <prometheus-pod> -- wget -O- http://<service-ip>:8080/actuator/prometheus
   ```

### Grafana 无法连接 Prometheus

**问题**: Grafana 面板显示 "No data"

**排查步骤**:
1. 检查 Grafana 数据源配置：Settings -> Data Sources -> Prometheus
2. 测试数据源连接：点击 "Save & Test"
3. 检查 Prometheus Service 是否正常：
   ```bash
   kubectl get svc prometheus -n monitoring
   ```
4. 在 Grafana Pod 中测试连接：
   ```bash
   kubectl exec -n monitoring <grafana-pod> -- wget -O- http://prometheus:9090/api/v1/query?query=up
   ```

### AlertManager 未发送告警

**问题**: 告警触发但未收到通知

**排查步骤**:
1. 检查 AlertManager 配置是否正确：
   ```bash
   kubectl logs -n monitoring <alertmanager-pod>
   ```
2. 检查告警是否到达 AlertManager：访问 http://<NODE_IP>:30093
3. 检查通知渠道配置（邮件、钉钉、企业微信 Token）
4. 测试 Webhook 连接：
   ```bash
   curl -X POST -H 'Content-Type: application/json' \
     -d '{"msgtype":"text","text":{"content":"测试消息"}}' \
     'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN'
   ```

### Pod 无法启动

**问题**: Pod 状态为 Pending 或 CrashLoopBackOff

**排查步骤**:
1. 查看 Pod 事件：
   ```bash
   kubectl describe pod <pod-name> -n monitoring
   ```
2. 查看 Pod 日志：
   ```bash
   kubectl logs <pod-name> -n monitoring
   ```
3. 检查 PersistentVolumeClaim 是否绑定：
   ```bash
   kubectl get pvc -n monitoring
   ```
4. 检查资源限制是否合理

### 存储空间不足

**问题**: Prometheus 或 Grafana 存储空间不足

**解决方案**:
1. 增加 PVC 大小（需要 StorageClass 支持动态扩容）
2. 减少 Prometheus 数据保留时间（修改 `--storage.tsdb.retention.time`）
3. 清理旧数据：
   ```bash
   kubectl exec -n monitoring <prometheus-pod> -- rm -rf /prometheus/data/*
   ```

## 📝 维护建议

### 定期检查

- **每周**: 检查存储空间使用情况
- **每月**: 审查告警规则和阈值
- **每季度**: 优化监控面板和指标

### 备份

定期备份 Grafana 配置和面板：
```bash
# 导出 Grafana 面板
kubectl exec -n monitoring <grafana-pod> -- \
  grafana-cli admin export-dashboard <dashboard-id> > dashboard-backup.json
```

### 性能优化

- 调整 Prometheus 抓取间隔（默认 15 秒）
- 优化告警规则查询（避免复杂的 PromQL）
- 使用 Recording Rules 预计算常用指标

## 🔗 相关文档

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [AlertManager 官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## 📄 许可证

本项目采用 MIT 许可证。
