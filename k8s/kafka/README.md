# Kafka 事件总线基础设施部署指南

本目录包含在 Kubernetes 上部署 Kafka 集群（3 个 broker）的所有配置文件。

## 架构概述

- **Zookeeper**: 1 个实例（用于 Kafka 协调）
- **Kafka Brokers**: 3 个实例（StatefulSet）
- **Topics**: 4 个核心 topic + 1 个死信队列
  - `order-events`: 订单事件（3 分区，2 副本）
  - `payment-events`: 支付事件（3 分区，2 副本）
  - `inventory-events`: 库存事件（3 分区，2 副本）
  - `notification-events`: 通知事件（3 分区，2 副本）
  - `dead-letter-queue`: 死信队列（3 分区，2 副本）

## 前置要求

1. Kubernetes 集群（版本 >= 1.20）
2. kubectl 已配置并连接到集群
3. 集群有足够的资源：
   - CPU: 至少 2.5 核心
   - 内存: 至少 5GB
   - 存储: 至少 180GB（Zookeeper 20GB + Kafka 3x50GB + 余量）

## 部署步骤

### 1. 创建命名空间

```bash
kubectl apply -f namespace.yaml
```

验证命名空间创建成功：
```bash
kubectl get namespace kafka
```

### 2. 部署 Zookeeper

```bash
kubectl apply -f zookeeper-statefulset.yaml
```

等待 Zookeeper 就绪：
```bash
kubectl wait --for=condition=ready pod/zookeeper-0 -n kafka --timeout=300s
```

验证 Zookeeper 状态：
```bash
kubectl exec -it zookeeper-0 -n kafka -- bash -c "echo ruok | nc localhost 2181"
# 应该返回: imok
```

### 3. 部署 Kafka 集群

```bash
kubectl apply -f kafka-statefulset.yaml
```

等待所有 Kafka broker 就绪：
```bash
kubectl wait --for=condition=ready pod/kafka-0 -n kafka --timeout=300s
kubectl wait --for=condition=ready pod/kafka-1 -n kafka --timeout=300s
kubectl wait --for=condition=ready pod/kafka-2 -n kafka --timeout=300s
```

验证 Kafka 集群状态：
```bash
# 查看所有 pods
kubectl get pods -n kafka

# 查看 StatefulSet
kubectl get statefulset -n kafka

# 查看服务
kubectl get svc -n kafka
```

### 4. 创建 Topics

```bash
kubectl apply -f topic-creation-job.yaml
```

查看 topic 创建日志：
```bash
kubectl logs -n kafka job/kafka-topic-creation -f
```

验证 topics 创建成功：
```bash
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --list
```

应该看到以下 topics：
- order-events
- payment-events
- inventory-events
- notification-events
- dead-letter-queue

查看 topic 详细信息：
```bash
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe --topic order-events
```

### 5. 部署健康检查 CronJob（可选）

```bash
kubectl apply -f health-check-cronjob.yaml
```

手动触发健康检查：
```bash
kubectl create job --from=cronjob/kafka-health-check kafka-health-check-manual -n kafka
kubectl logs -n kafka job/kafka-health-check-manual -f
```

## 验证部署

### 1. 检查所有资源状态

```bash
kubectl get all -n kafka
```

期望输出：
```
NAME              READY   STATUS    RESTARTS   AGE
pod/kafka-0       1/1     Running   0          5m
pod/kafka-1       1/1     Running   0          5m
pod/kafka-2       1/1     Running   0          5m
pod/zookeeper-0   1/1     Running   0          10m

NAME                     TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)             AGE
service/kafka            ClusterIP   10.96.xxx.xxx   <none>        9092/TCP            5m
service/kafka-headless   ClusterIP   None            <none>        9092/TCP,9093/TCP   5m
service/zookeeper        ClusterIP   None            <none>        2181/TCP,2888/TCP   10m

NAME                         READY   AGE
statefulset.apps/kafka       3/3     5m
statefulset.apps/zookeeper   1/1     10m
```

### 2. 测试消息发送和接收

进入 Kafka pod：
```bash
kubectl exec -it kafka-0 -n kafka -- bash
```

发送测试消息：
```bash
echo "test message" | kafka-console-producer --bootstrap-server localhost:9092 --topic order-events
```

接收测试消息：
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events --from-beginning --max-messages 1
```

### 3. 检查集群健康状态

```bash
# 检查 broker 列表
kubectl exec -it kafka-0 -n kafka -- kafka-broker-api-versions --bootstrap-server localhost:9092

# 检查 topic 状态
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe

# 检查消费者组
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## 连接配置

### 从集群内部连接

应用程序可以使用以下地址连接到 Kafka：

```yaml
spring:
  kafka:
    bootstrap-servers: kafka.kafka.svc.cluster.local:9092
```

或者使用单个 broker 地址（用于内部通信）：
```
kafka-0.kafka-headless.kafka.svc.cluster.local:9093
kafka-1.kafka-headless.kafka.svc.cluster.local:9093
kafka-2.kafka-headless.kafka.svc.cluster.local:9093
```

### 从集群外部连接（需要额外配置）

如果需要从集群外部访问 Kafka，需要：

1. 创建 NodePort 或 LoadBalancer 服务
2. 配置 KAFKA_ADVERTISED_LISTENERS 包含外部地址
3. 更新防火墙规则

## 监控和维护

### 查看日志

```bash
# Kafka broker 日志
kubectl logs -n kafka kafka-0 -f

# Zookeeper 日志
kubectl logs -n kafka zookeeper-0 -f

# 所有 Kafka brokers 日志
kubectl logs -n kafka -l app=kafka --tail=100
```

### 扩容 Kafka 集群

```bash
# 增加到 5 个 brokers
kubectl scale statefulset kafka -n kafka --replicas=5

# 等待新 brokers 就绪
kubectl wait --for=condition=ready pod/kafka-3 -n kafka --timeout=300s
kubectl wait --for=condition=ready pod/kafka-4 -n kafka --timeout=300s
```

注意：扩容后需要手动重新分配分区以平衡负载。

### 备份和恢复

```bash
# 导出 topic 配置
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe > kafka-topics-backup.txt

# 导出消费者组偏移量
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --all-groups --describe > consumer-groups-backup.txt
```

## 故障排查

### Kafka broker 无法启动

1. 检查 Zookeeper 是否正常运行：
```bash
kubectl exec -it zookeeper-0 -n kafka -- bash -c "echo ruok | nc localhost 2181"
```

2. 检查 PVC 是否正常绑定：
```bash
kubectl get pvc -n kafka
```

3. 查看 broker 日志：
```bash
kubectl logs -n kafka kafka-0 --tail=100
```

### Topic 创建失败

1. 检查 broker 数量是否足够（至少 2 个用于副本因子 2）
2. 检查 Kafka 配置中的 `auto.create.topics.enable` 设置
3. 手动创建 topic：
```bash
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic --partitions 3 --replication-factor 2
```

### 消息消费延迟

1. 检查消费者组延迟：
```bash
kubectl exec -it kafka-0 -n kafka -- kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group <group-name>
```

2. 增加消费者实例数量
3. 检查网络延迟和资源使用情况

### Under-replicated 分区

1. 检查哪些分区 under-replicated：
```bash
kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe --under-replicated-partitions
```

2. 检查 broker 健康状态
3. 触发分区重新分配（如果需要）

## 清理

删除所有 Kafka 资源：

```bash
# 删除 CronJob
kubectl delete -f health-check-cronjob.yaml

# 删除 Job
kubectl delete -f topic-creation-job.yaml

# 删除 Kafka
kubectl delete -f kafka-statefulset.yaml

# 删除 Zookeeper
kubectl delete -f zookeeper-statefulset.yaml

# 删除命名空间（会删除所有资源和 PVC）
kubectl delete -f namespace.yaml
```

**警告**: 删除命名空间会永久删除所有数据！

## 性能调优

### Kafka Broker 配置

关键配置参数（在 StatefulSet 中调整）：

- `num.network.threads`: 网络线程数（默认 3）
- `num.io.threads`: IO 线程数（默认 8）
- `socket.send.buffer.bytes`: 发送缓冲区大小
- `socket.receive.buffer.bytes`: 接收缓冲区大小
- `log.segment.bytes`: 日志段大小
- `log.retention.hours`: 日志保留时间

### 资源限制

根据实际负载调整资源请求和限制：

```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

## 参考文档

- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Platform Documentation](https://docs.confluent.io/)
- [Kubernetes StatefulSets](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/)
- [Requirements 1.1-1.4](../../.kiro/specs/microservice-evolution/requirements.md)
