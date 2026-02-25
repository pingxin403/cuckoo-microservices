# Kafka 集群快速开始指南

## 一键部署

```bash
cd cuckoo-microservices/k8s/kafka
./deploy.sh
```

部署脚本会自动完成：
1. ✓ 创建 kafka 命名空间
2. ✓ 部署 Zookeeper (1 实例)
3. ✓ 部署 Kafka 集群 (3 brokers)
4. ✓ 创建 5 个 topics (3 分区, 2 副本)
5. ✓ 部署健康检查 CronJob
6. ✓ 验证部署状态

## 验证部署

```bash
./verify.sh
```

验证脚本会检查：
- ✓ 命名空间
- ✓ Zookeeper 状态
- ✓ Kafka brokers 状态
- ✓ Services
- ✓ PVCs
- ✓ Topics 配置
- ✓ 消息发送/接收

## 常用命令

### 查看集群状态

```bash
# 查看所有资源
kubectl get all -n kafka

# 查看 pods
kubectl get pods -n kafka

# 查看 services
kubectl get svc -n kafka

# 查看 PVCs
kubectl get pvc -n kafka
```

### 查看日志

```bash
# Kafka broker 日志
kubectl logs -n kafka kafka-0 -f

# Zookeeper 日志
kubectl logs -n kafka zookeeper-0 -f

# 所有 Kafka brokers 日志
kubectl logs -n kafka -l app=kafka --tail=100
```

### 进入容器

```bash
# 进入 Kafka broker
kubectl exec -it kafka-0 -n kafka -- bash

# 进入 Zookeeper
kubectl exec -it zookeeper-0 -n kafka -- bash
```

### Topic 操作

```bash
# 列出所有 topics
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --list

# 查看 topic 详情
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe --topic order-events

# 创建新 topic
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic --partitions 3 --replication-factor 2

# 删除 topic
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --delete --topic test-topic
```

### 消息测试

```bash
# 发送消息
echo "test message" | kubectl exec -i kafka-0 -n kafka -- kafka-console-producer --bootstrap-server localhost:9092 --topic order-events

# 接收消息（从头开始）
kubectl exec -it kafka-0 -n kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events --from-beginning

# 接收消息（只接收新消息）
kubectl exec -it kafka-0 -n kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events
```

### 消费者组管理

```bash
# 列出所有消费者组
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --list

# 查看消费者组详情
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group <group-name>

# 重置消费者组偏移量
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --group <group-name> --topic <topic-name> --reset-offsets --to-earliest --execute
```

### 健康检查

```bash
# 手动触发健康检查
kubectl create job --from=cronjob/kafka-health-check kafka-health-check-manual -n kafka

# 查看健康检查日志
kubectl logs -n kafka job/kafka-health-check-manual -f

# 删除手动触发的 job
kubectl delete job kafka-health-check-manual -n kafka
```

## 连接配置

### Spring Boot 应用配置

```yaml
spring:
  kafka:
    bootstrap-servers: kafka.kafka.svc.cluster.local:9092
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
```

### 环境变量配置

```bash
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka.kafka.svc.cluster.local:9092
```

## 故障排查

### Kafka broker 无法启动

```bash
# 1. 检查 Zookeeper
kubectl exec -it zookeeper-0 -n kafka -- bash -c "echo ruok | nc localhost 2181"

# 2. 查看 broker 日志
kubectl logs -n kafka kafka-0 --tail=100

# 3. 检查 PVC
kubectl get pvc -n kafka

# 4. 重启 broker
kubectl delete pod kafka-0 -n kafka
```

### Topic 创建失败

```bash
# 1. 检查 broker 数量
kubectl get pods -n kafka -l app=kafka

# 2. 手动创建 topic
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic --partitions 3 --replication-factor 2

# 3. 查看 topic 创建 job 日志
kubectl logs -n kafka job/kafka-topic-creation
```

### 消息消费延迟

```bash
# 1. 检查消费者组延迟
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group <group-name>

# 2. 查看 broker 性能
kubectl top pods -n kafka

# 3. 增加消费者实例数量
```

### Under-replicated 分区

```bash
# 1. 检查 under-replicated 分区
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe --under-replicated-partitions

# 2. 检查 broker 状态
kubectl get pods -n kafka

# 3. 查看 broker 日志
kubectl logs -n kafka kafka-0 --tail=100
```

## 清理

```bash
# 清理所有资源（保留命名空间）
./cleanup.sh
# 输入 "yes" 确认删除资源
# 输入 "no" 保留命名空间

# 完全清理（包括命名空间）
./cleanup.sh
# 输入 "yes" 确认删除资源
# 输入 "yes" 确认删除命名空间
```

## 性能调优

### 增加 broker 资源

编辑 `kafka-statefulset.yaml`:

```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

应用更改：
```bash
kubectl apply -f kafka-statefulset.yaml
```

### 扩容 Kafka 集群

```bash
# 增加到 5 个 brokers
kubectl scale statefulset kafka -n kafka --replicas=5

# 等待新 brokers 就绪
kubectl wait --for=condition=ready pod/kafka-3 -n kafka --timeout=300s
kubectl wait --for=condition=ready pod/kafka-4 -n kafka --timeout=300s
```

### 调整 topic 配置

```bash
# 增加分区数（只能增加，不能减少）
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --alter --topic order-events --partitions 6

# 修改保留时间
kubectl exec -it kafka-0 -n kafka -- kafka-configs --bootstrap-server localhost:9092 --entity-type topics --entity-name order-events --alter --add-config retention.ms=1209600000
```

## 监控

### Prometheus 指标

Kafka JMX 指标暴露在端口 9999：

```yaml
- job_name: 'kafka'
  static_configs:
  - targets:
    - kafka-0.kafka-headless.kafka.svc.cluster.local:9999
    - kafka-1.kafka-headless.kafka.svc.cluster.local:9999
    - kafka-2.kafka-headless.kafka.svc.cluster.local:9999
```

### 关键指标

- `kafka_server_brokertopicmetrics_messagesinpersec`: 消息流入速率
- `kafka_server_brokertopicmetrics_bytesoutpersec`: 消息流出速率
- `kafka_controller_kafkacontroller_activecontrollercount`: 活跃 controller 数量
- `kafka_server_replicamanager_underreplicatedpartitions`: Under-replicated 分区数

## 备份和恢复

### 导出配置

```bash
# 导出 topic 配置
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe > kafka-topics-backup.txt

# 导出消费者组偏移量
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --all-groups --describe > consumer-groups-backup.txt
```

### 数据备份

使用 Velero 或类似工具备份 PVCs：

```bash
# 安装 Velero
# 备份 kafka 命名空间
velero backup create kafka-backup --include-namespaces kafka

# 恢复
velero restore create --from-backup kafka-backup
```

## 下一步

1. 阅读完整文档: [README.md](README.md)
2. 查看设计文档: [../../.kiro/specs/microservice-evolution/design.md](../../.kiro/specs/microservice-evolution/design.md)
3. 实现事件发布和消费: 任务 2
4. 实现本地消息表: 任务 3

## 支持

如有问题，请查看：
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [故障排查指南](README.md#故障排查)
