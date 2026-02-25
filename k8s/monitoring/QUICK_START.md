# Prometheus + Grafana 监控系统快速开始指南

本指南帮助您快速部署和使用 Prometheus + Grafana 监控系统。

## 📋 前置条件

- Kubernetes 集群（v1.20+）
- kubectl 已配置并可访问集群
- 至少 4GB 可用内存
- 至少 30GB 可用存储空间

## 🚀 5 分钟快速部署

### 步骤 1: 克隆项目

```bash
git clone <repository-url>
cd cuckoo-microservices/k8s/monitoring
```

### 步骤 2: 一键部署

```bash
./deploy.sh
```

部署脚本会自动完成：
- ✅ 创建 monitoring 命名空间
- ✅ 部署 Prometheus（包括配置和告警规则）
- ✅ 部署 AlertManager
- ✅ 部署 Grafana（包括数据源和预配置面板）
- ✅ 等待所有组件就绪
- ✅ 显示访问信息

### 步骤 3: 验证部署

```bash
./verify.sh
```

验证脚本会检查所有组件的健康状态。

### 步骤 4: 访问监控系统

部署完成后，您会看到类似以下的访问信息：

```
Prometheus UI:   http://192.168.1.100:30090
AlertManager UI: http://192.168.1.100:30093
Grafana UI:      http://192.168.1.100:30300
  默认用户名: admin
  默认密码: admin123
```

## 📊 查看监控面板

### 1. 登录 Grafana

1. 打开浏览器访问 Grafana UI
2. 使用默认用户名 `admin` 和密码 `admin123` 登录
3. 首次登录后建议修改密码

### 2. 查看预配置面板

Grafana 已预配置了 4 个监控面板：

#### 微服务概览
- 路径: Dashboards -> 微服务概览
- 内容: 服务 QPS、响应时间、错误率、可用性

#### JVM 监控
- 路径: Dashboards -> JVM 监控
- 内容: 堆内存、GC、线程、类加载

#### 业务指标监控
- 路径: Dashboards -> 业务指标监控
- 内容: 订单、支付、库存等业务指标

#### Kafka 监控
- 路径: Dashboards -> Kafka 监控
- 内容: 消费延迟、消息积压、生产消费速率

## 🔔 配置告警通知

### 邮件通知

编辑 `alertmanager-config.yaml`：

```yaml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'your-email@gmail.com'
  smtp_auth_username: 'your-email@gmail.com'
  smtp_auth_password: 'your-app-password'
```

### 钉钉通知

1. 在钉钉群中添加自定义机器人
2. 复制 Webhook 地址中的 access_token
3. 编辑 `alertmanager-config.yaml`：

```yaml
webhook_configs:
  - url: 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN'
```

### 企业微信通知

1. 在企业微信群中添加群机器人
2. 复制 Webhook 地址中的 key
3. 编辑 `alertmanager-config.yaml`：

```yaml
webhook_configs:
  - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
```

### 应用配置更改

```bash
kubectl apply -f alertmanager-config.yaml
kubectl rollout restart deployment/alertmanager -n monitoring
```

## 🔧 配置微服务监控

### 为微服务添加 Prometheus 注解

编辑微服务的 Deployment YAML：

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

### 验证指标暴露

```bash
# 获取 Pod 名称
kubectl get pods -n default -l app=order-service

# 测试指标端点
kubectl exec -n default <pod-name> -- wget -O- http://localhost:8080/actuator/prometheus
```

## 📈 常用 Prometheus 查询

### 服务 QPS

```promql
sum(rate(http_server_requests_seconds_count[5m])) by (application)
```

### 服务响应时间 P99

```promql
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (application, le))
```

### 服务错误率

```promql
(sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application) / sum(rate(http_server_requests_seconds_count[5m])) by (application)) * 100
```

### JVM 内存使用率

```promql
(sum(jvm_memory_used_bytes{area="heap"}) by (application) / sum(jvm_memory_max_bytes{area="heap"}) by (application)) * 100
```

## 🐛 常见问题

### Q: Prometheus 无法抓取指标？

**A**: 检查以下几点：
1. 微服务 Pod 是否添加了 `prometheus.io/scrape: "true"` 注解
2. 微服务是否暴露了 `/actuator/prometheus` 端点
3. 端口是否正确（通过 `prometheus.io/port` 注解指定）

### Q: Grafana 面板显示 "No data"？

**A**: 检查以下几点：
1. Prometheus 是否正在抓取指标（访问 Prometheus UI -> Status -> Targets）
2. Grafana 数据源是否配置正确（Settings -> Data Sources -> Prometheus）
3. 时间范围是否正确（右上角时间选择器）

### Q: 告警未发送？

**A**: 检查以下几点：
1. 告警规则是否触发（访问 Prometheus UI -> Alerts）
2. AlertManager 配置是否正确（访问 AlertManager UI）
3. 通知渠道配置是否正确（邮件、钉钉、企业微信 Token）

### Q: 存储空间不足？

**A**: 解决方案：
1. 增加 PVC 大小（需要 StorageClass 支持动态扩容）
2. 减少 Prometheus 数据保留时间（修改 `--storage.tsdb.retention.time`）
3. 清理旧数据

## 📚 下一步

- [完整文档](README.md)
- [故障排查指南](README.md#故障排查)
- [业务指标收集](../../cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/metrics/README.md)
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)

## 💡 提示

- 首次登录 Grafana 后，建议修改默认密码
- 定期检查存储空间使用情况
- 根据实际情况调整告警阈值
- 定期备份 Grafana 配置和面板

## 🆘 获取帮助

如果遇到问题，请：
1. 查看 [完整文档](README.md)
2. 运行 `./verify.sh` 检查系统状态
3. 查看 Pod 日志：`kubectl logs <pod-name> -n monitoring`
4. 提交 Issue 到项目仓库
