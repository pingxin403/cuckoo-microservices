# Kafka Event Consumer 实现指南

## 概述

本模块提供了完整的 Kafka 事件消费基础设施，包括：
- 幂等性检查（基于 eventId）
- 自动重试机制（最多 3 次）
- 死信队列支持
- 统一的错误处理

## 架构组件

### 1. AbstractEventConsumer<T>
抽象基类，提供模板方法模式的事件消费流程。

**功能特性：**
- 自动幂等性检查
- 统一的错误处理
- 事件处理状态记录

**使用方式：**
```java
@Component
public class MyEventConsumer extends AbstractEventConsumer<MyEvent> {
    
    @KafkaListener(
        topics = "my-topic",
        groupId = "my-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvent(MyEvent event) {
        consume(event);
    }
    
    @Override
    protected void handleEvent(MyEvent event) {
        // 实现具体的业务逻辑
    }
}
```

### 2. KafkaConsumerConfig
提供统一的 Kafka 消费者配置，包括错误处理和重试策略。

**重试策略：**
- 最大重试次数：3 次
- 初始间隔：1 秒
- 退避倍数：2.0（指数退避）
- 最大间隔：10 秒

**重试时间线示例：**
```
第 1 次失败 -> 等待 1 秒 -> 第 1 次重试
第 2 次失败 -> 等待 2 秒 -> 第 2 次重试
第 3 次失败 -> 等待 4 秒 -> 第 3 次重试
第 4 次失败 -> 发送到死信队列
```

### 3. 幂等性检查
使用 `event_idempotency` 表（在设计文档中称为 `processed_events`）记录已处理的事件。

**表结构：**
```sql
CREATE TABLE processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(50) UNIQUE NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
```

**工作流程：**
1. 消费事件前检查 `event_id` 是否存在
2. 如果存在，跳过处理（幂等性保证）
3. 如果不存在，处理事件并记录 `event_id`

### 4. 死信队列
重试失败的消息自动发送到 `dead-letter-queue` 主题。

**死信队列消息包含：**
- 原始消息内容
- 原始主题和分区信息
- 失败原因和异常堆栈

## 配置指南

### 步骤 1: 在服务中创建 KafkaConfig

```java
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Autowired
    private KafkaConsumerConfig kafkaConsumerConfig;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pingxin403.cuckoo.*");

        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(Object.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            CommonErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 应用通用的错误处理器
        kafkaConsumerConfig.configureContainerFactory(factory, errorHandler);
        
        return factory;
    }
}
```

### 步骤 2: 在 application.yml 中配置 Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: my-service-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.pingxin403.cuckoo.*
```

### 步骤 3: 创建数据库表

在服务的数据库中创建幂等性检查表：

```sql
CREATE TABLE processed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(50) UNIQUE NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_id (event_id)
);
```

### 步骤 4: 实现事件消费者

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer extends AbstractEventConsumer<OrderCreatedEvent> {

    private final OrderService orderService;

    @KafkaListener(
        topics = "order-events",
        groupId = "inventory-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("收到 OrderCreatedEvent: eventId={}, orderId={}",
                event.getEventId(), event.getOrderId());
        consume(event);
    }

    @Override
    protected void handleEvent(OrderCreatedEvent event) {
        // 实现业务逻辑
        orderService.processOrder(event);
    }
}
```

## 错误处理流程

```
消息到达
    ↓
幂等性检查
    ↓
已处理? → 是 → 跳过（记录日志）
    ↓ 否
处理事件
    ↓
成功? → 是 → 标记为已处理 → 完成
    ↓ 否
记录错误日志
    ↓
抛出异常
    ↓
Kafka 重试机制
    ↓
重试 1 (等待 1 秒)
    ↓
成功? → 是 → 标记为已处理 → 完成
    ↓ 否
重试 2 (等待 2 秒)
    ↓
成功? → 是 → 标记为已处理 → 完成
    ↓ 否
重试 3 (等待 4 秒)
    ↓
成功? → 是 → 标记为已处理 → 完成
    ↓ 否
发送到死信队列
    ↓
记录告警日志
```

## 监控和告警

### 关键指标

1. **消费延迟**：监控 Kafka 消费者的 lag
2. **重试次数**：监控重试频率
3. **死信队列消息数**：监控失败消息数量
4. **幂等性命中率**：监控重复消息比例

### 日志示例

**正常消费：**
```
INFO  开始处理事件: eventId=abc123, eventType=OrderCreatedEvent
INFO  事件处理完成: eventId=abc123, eventType=OrderCreatedEvent
```

**重复事件：**
```
INFO  跳过重复事件: eventId=abc123, eventType=OrderCreatedEvent
```

**重试：**
```
ERROR 事件处理失败: eventId=abc123, eventType=OrderCreatedEvent
WARN  消息处理重试: topic=order-events, partition=0, offset=123, attempt=1/3, error=...
```

**发送到死信队列：**
```
ERROR 消息处理失败，发送到死信队列: topic=order-events, partition=0, offset=123, key=abc123, error=...
```

## 最佳实践

### 1. 幂等性设计
确保 `handleEvent` 方法的实现是幂等的，即使幂等性检查失败，多次执行也不会产生副作用。

### 2. 事务管理
如果业务逻辑涉及数据库操作，使用 `@Transactional` 确保原子性：

```java
@Override
@Transactional
protected void handleEvent(OrderCreatedEvent event) {
    // 数据库操作会在同一事务中执行
    orderService.processOrder(event);
}
```

### 3. 错误处理
在 `handleEvent` 中捕获业务异常并转换为运行时异常，让 Kafka 框架处理重试：

```java
@Override
protected void handleEvent(OrderCreatedEvent event) {
    try {
        orderService.processOrder(event);
    } catch (BusinessException e) {
        log.error("业务处理失败: {}", e.getMessage());
        throw new RuntimeException("Order processing failed", e);
    }
}
```

### 4. 死信队列处理
定期检查死信队列，分析失败原因：
- 修复 bug 后重新发送消息
- 对于无法恢复的消息，记录并归档

## 测试

### 单元测试示例

```java
@SpringBootTest
class OrderCreatedEventConsumerTest {

    @Autowired
    private OrderCreatedEventConsumer consumer;

    @MockBean
    private OrderService orderService;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    void shouldProcessEventSuccessfully() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId("test-123");
        event.setOrderId("order-456");
        
        when(idempotencyService.isDuplicate("test-123")).thenReturn(false);

        // When
        consumer.consume(event);

        // Then
        verify(orderService).processOrder(event);
        verify(idempotencyService).markProcessed("test-123");
    }

    @Test
    void shouldSkipDuplicateEvent() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId("test-123");
        
        when(idempotencyService.isDuplicate("test-123")).thenReturn(true);

        // When
        consumer.consume(event);

        // Then
        verify(orderService, never()).processOrder(any());
    }
}
```

## 故障排查

### 问题 1: 消息重复消费
**症状**：同一个 eventId 被处理多次
**原因**：幂等性检查失败或事务未提交
**解决**：检查数据库事务配置和 `processed_events` 表

### 问题 2: 消息一直重试
**症状**：同一个消息重试 3 次后仍然失败
**原因**：业务逻辑错误或依赖服务不可用
**解决**：检查死信队列中的错误信息，修复 bug 后重新发送

### 问题 3: 消费延迟过高
**症状**：Kafka lag 持续增长
**原因**：消费速度慢于生产速度
**解决**：增加消费者并发数或优化业务逻辑

## Requirements 映射

- **Requirement 1.6**: 消费失败重试最多 3 次，超过重试次数后发送到死信队列
- **Requirement 1.7**: 通过 eventId 实现幂等性检查

## 相关文档

- [Design Document](../../../../../../../../../.kiro/specs/microservice-evolution/design.md)
- [Requirements Document](../../../../../../../../../.kiro/specs/microservice-evolution/requirements.md)
- [Kafka Official Documentation](https://kafka.apache.org/documentation/)
