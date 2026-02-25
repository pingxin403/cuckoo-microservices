# 消息重试调度器 (MessageRetryScheduler)

## 概述

MessageRetryScheduler 是本地消息表模式的核心组件，负责定时扫描待发送的消息并重试发送到 Kafka，确保事件发布的可靠性。

## 功能特性

### 1. 定时重试机制
- **扫描频率**: 每 30 秒扫描一次待发送消息
- **批量处理**: 每次最多处理 100 条消息
- **初始延迟**: 应用启动后 10 秒开始执行

### 2. 重试策略
- **最大重试次数**: 5 次
- **发送超时**: 5 秒
- **失败处理**: 
  - 发送失败时自动增加重试次数
  - 超过最大重试次数后标记为 FAILED
  - 发送告警通知（当前记录错误日志）

### 3. 消息清理
- **清理频率**: 每天凌晨 2 点执行
- **保留期限**: 7 天
- **清理范围**: 仅清理已发送成功的消息

### 4. 监控支持
- 提供消息统计信息查询
- 详细的日志记录（DEBUG、INFO、WARN、ERROR 级别）
- 支持通过配置开关启用/禁用

## 配置说明

### 启用/禁用调度器

在 `application.yml` 中配置：

```yaml
cuckoo:
  message:
    retry:
      enabled: true  # 默认为 true，设置为 false 可禁用调度器
```

### 自定义配置（可选）

如需自定义配置，可以修改 `MessageRetryScheduler` 类中的常量：

```java
// 最大重试次数
private static final int MAX_RETRY_COUNT = 5;

// 每次扫描的消息数量限制
private static final int BATCH_SIZE = 100;

// 消息保留天数
private static final int MESSAGE_RETENTION_DAYS = 7;

// 发送超时时间（秒）
private static final int SEND_TIMEOUT_SECONDS = 5;
```

## 工作流程

### 消息重试流程

```
1. 扫描本地消息表中 PENDING 状态的消息
   ↓
2. 检查重试次数
   ├─ 重试次数 < 5: 继续处理
   └─ 重试次数 >= 5: 标记为 FAILED 并告警
   ↓
3. 反序列化事件
   ↓
4. 发送到 Kafka
   ├─ 成功: 标记为 SENT
   └─ 失败: 增加重试次数
```

### 消息清理流程

```
1. 查询 7 天前已发送的消息
   ↓
2. 批量删除
   ↓
3. 记录清理数量
```

## 使用示例

### 业务代码中保存消息

```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private LocalMessageService localMessageService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    public Order createOrder(OrderRequest request) {
        // 1. 保存订单
        Order order = orderRepository.save(buildOrder(request));
        
        // 2. 创建事件
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        
        // 3. 在同一事务中保存消息
        localMessageService.saveMessage(event);
        
        // 4. 异步发布事件（失败不影响事务）
        CompletableFuture.runAsync(() -> {
            try {
                eventPublisher.publish(event).get(5, TimeUnit.SECONDS);
                localMessageService.markAsSent(event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish event, will retry later", e);
                // 消息保持 PENDING 状态，等待调度器重试
            }
        });
        
        return order;
    }
}
```

### 查询消息统计

```java
@RestController
@RequestMapping("/api/admin/messages")
public class MessageAdminController {
    
    @Autowired
    private MessageRetryScheduler scheduler;
    
    @GetMapping("/stats")
    public MessageStats getStats() {
        return scheduler.getMessageStats();
    }
}
```

## 监控指标

### 日志级别

- **DEBUG**: 扫描开始、消息重试详情
- **INFO**: 批量处理结果、成功发送、清理完成
- **WARN**: 发送失败（会重试）
- **ERROR**: 超过最大重试次数、反序列化失败、调度器执行失败

### 关键日志示例

```
# 扫描开始
DEBUG - 开始扫描待发送消息...

# 发现待发送消息
INFO - 发现 5 条待发送消息，开始重试

# 重试成功
INFO - 消息重试发送成功: messageId=xxx, eventType=ORDER_CREATED, retryCount=2

# 重试失败
WARN - 消息重试发送失败: messageId=xxx, eventType=ORDER_CREATED, retryCount=3, error=Kafka unavailable

# 超过最大重试次数
ERROR - 【告警】消息发送失败: messageId=xxx, eventType=ORDER_CREATED, retryCount=5, error=消息重试次数超过最大限制: maxRetryCount=5

# 批量处理完成
INFO - 消息重试完成: total=5, success=3, failed=1, maxRetryExceeded=1

# 清理旧消息
INFO - 旧消息清理完成: deletedCount=100, retentionDays=7
```

## 告警集成

当前版本的告警功能仅记录错误日志。未来可以集成以下告警渠道：

### 1. 钉钉告警

```java
private void sendAlert(LocalMessage message, String errorMessage) {
    DingTalkAlert alert = DingTalkAlert.builder()
        .title("消息发送失败告警")
        .content(String.format(
            "消息ID: %s\n事件类型: %s\n重试次数: %d\n错误信息: %s",
            message.getMessageId(),
            message.getEventType(),
            message.getRetryCount(),
            errorMessage
        ))
        .build();
    
    dingTalkClient.send(alert);
}
```

### 2. 企业微信告警

```java
private void sendAlert(LocalMessage message, String errorMessage) {
    WeChatWorkAlert alert = WeChatWorkAlert.builder()
        .msgtype("text")
        .text(String.format(
            "【消息发送失败】\n消息ID: %s\n事件类型: %s\n重试次数: %d\n错误信息: %s",
            message.getMessageId(),
            message.getEventType(),
            message.getRetryCount(),
            errorMessage
        ))
        .build();
    
    weChatWorkClient.send(alert);
}
```

### 3. 邮件告警

```java
private void sendAlert(LocalMessage message, String errorMessage) {
    EmailAlert alert = EmailAlert.builder()
        .to("ops@example.com")
        .subject("消息发送失败告警")
        .body(String.format(
            "消息ID: %s\n事件类型: %s\n重试次数: %d\n创建时间: %s\n错误信息: %s",
            message.getMessageId(),
            message.getEventType(),
            message.getRetryCount(),
            message.getCreatedAt(),
            errorMessage
        ))
        .build();
    
    emailClient.send(alert);
}
```

## 性能考虑

### 1. 批量处理
- 每次最多处理 100 条消息，避免长时间占用线程
- 如果待发送消息超过 100 条，下次调度会继续处理

### 2. 超时控制
- 每条消息发送超时时间为 5 秒
- 避免因 Kafka 响应慢导致调度器阻塞

### 3. 异步发送
- 业务代码中使用异步发送，不阻塞主流程
- 发送失败不影响业务事务提交

### 4. 数据库索引
- `local_message` 表已创建 `idx_status_created` 索引
- 优化 PENDING 状态消息的查询性能

## 故障排查

### 问题 1: 消息一直处于 PENDING 状态

**可能原因**:
1. Kafka 服务不可用
2. 网络连接问题
3. 事件反序列化失败

**排查步骤**:
1. 检查 Kafka 服务状态
2. 查看调度器日志，确认是否有错误信息
3. 检查消息的 payload 是否正确

### 问题 2: 消息重试次数快速增加

**可能原因**:
1. Kafka 持续不可用
2. 事件格式错误导致反序列化失败

**排查步骤**:
1. 检查 Kafka 集群健康状态
2. 查看错误日志中的具体错误信息
3. 验证事件的 JSON 格式是否正确

### 问题 3: 调度器未执行

**可能原因**:
1. 调度器被禁用
2. Spring 定时任务未启用

**排查步骤**:
1. 检查配置 `cuckoo.message.retry.enabled` 是否为 true
2. 确认 `@EnableScheduling` 注解已添加
3. 查看应用启动日志，确认调度器 Bean 已创建

## 最佳实践

### 1. 事务管理
- 业务操作和消息保存必须在同一事务中
- 确保业务失败时消息也不会保存

### 2. 幂等性
- 消费端必须实现幂等性检查
- 使用 eventId 去重，避免重复处理

### 3. 监控告警
- 定期检查 FAILED 状态的消息数量
- 设置告警阈值，及时发现问题

### 4. 消息清理
- 定期清理旧消息，避免表数据过大
- 可根据业务需求调整保留天数

## 相关文档

- [本地消息表设计](./README.md)
- [事件发布器](../event/README.md)
- [Kafka 配置](../kafka/README.md)

## Requirements

- Requirements 1.5: 本地消息表保证消息可靠发送
- Requirements 1.6: 消息重试和失败处理
