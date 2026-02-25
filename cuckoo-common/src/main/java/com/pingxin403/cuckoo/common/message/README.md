# 本地消息表 (Local Message Table)

## 概述

本地消息表模式用于保证事件发布的可靠性,避免消息丢失。通过在业务操作的同一事务中保存消息记录,确保消息最终能够发送到 Kafka。

## 核心组件

### 1. LocalMessage 实体

本地消息表实体,包含以下字段:
- `messageId`: 消息ID (UUID)
- `eventType`: 事件类型
- `payload`: 消息内容 (JSON格式)
- `status`: 消息状态 (PENDING/SENT/FAILED)
- `retryCount`: 重试次数
- `createdAt`: 创建时间
- `sentAt`: 发送成功时间
- `errorMessage`: 错误信息

### 2. LocalMessageRepository

提供消息的数据访问功能:
- 查询待发送的消息
- 查询旧消息用于清理
- 统计各状态消息数量

### 3. LocalMessageService

提供消息的业务逻辑:
- `saveMessage()`: 保存消息(与业务操作在同一事务中)
- `markAsSent()`: 标记消息为已发送
- `markAsFailed()`: 标记消息为失败
- `incrementRetryCount()`: 增加重试次数
- `getPendingMessages()`: 获取待发送的消息
- `cleanupOldMessages()`: 清理旧消息
- `getStats()`: 获取消息统计信息

## 使用方式

### 1. 配置 EntityScan

在服务的主应用类中添加 message 包到 EntityScan:

```java
@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "com.pingxin403.cuckoo.order.repository",
    "com.pingxin403.cuckoo.common.idempotency",
    "com.pingxin403.cuckoo.common.message"  // 添加这一行
})
@EntityScan(basePackages = {
    "com.pingxin403.cuckoo.order.entity",
    "com.pingxin403.cuckoo.common.idempotency",
    "com.pingxin403.cuckoo.common.message"  // 添加这一行
})
public class CuckooOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(CuckooOrderApplication.class, args);
    }
}
```

### 2. 在业务代码中使用

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final LocalMessageService localMessageService;
    private final EventPublisher eventPublisher;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. 保存订单
        Order order = orderRepository.save(buildOrder(request));
        
        // 2. 在同一事务中保存事件消息
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        localMessageService.saveMessage(event);
        
        // 3. 异步发布事件(失败不影响事务提交)
        CompletableFuture.runAsync(() -> {
            try {
                eventPublisher.publish(event).get(5, TimeUnit.SECONDS);
                localMessageService.markAsSent(event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish event, will retry later", e);
                // 消息保持 PENDING 状态,等待定时任务重试
            }
        });
        
        return order;
    }
}
```

### 3. 实现消息重试调度器

在下一个任务(3.3)中将实现 MessageRetryScheduler,用于定期扫描和重试待发送的消息。

## 数据库表结构

```sql
CREATE TABLE local_message (
    message_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    error_message TEXT NULL,
    INDEX idx_status_created (status, created_at)
);
```

## 工作流程

1. **业务操作成功**: 在同一事务中保存业务数据和消息记录
2. **异步发布事件**: 尝试发布事件到 Kafka
3. **发布成功**: 更新消息状态为 SENT
4. **发布失败**: 消息保持 PENDING 状态
5. **定时重试**: MessageRetryScheduler 定期扫描 PENDING 消息并重试
6. **超过重试次数**: 标记为 FAILED 并发送告警
7. **清理旧消息**: 定期清理已发送超过 7 天的消息

## 优势

1. **可靠性**: 通过本地事务保证消息不丢失
2. **最终一致性**: 通过重试机制保证消息最终发送
3. **可追溯性**: 记录消息状态和错误信息,便于问题排查
4. **性能**: 异步发布不阻塞业务操作

## 注意事项

1. 消息表需要定期清理,避免数据量过大
2. 重试次数有上限(默认5次),超过后需要人工介入
3. 消息序列化使用 JSON 格式,确保事件类可序列化
4. 需要在各服务中配置 EntityScan 和 EnableJpaRepositories

## 相关需求

- Requirements 1.5: 本地消息表实现
