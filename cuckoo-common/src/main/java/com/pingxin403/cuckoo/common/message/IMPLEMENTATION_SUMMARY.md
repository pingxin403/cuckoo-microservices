# Task 3.3 实现消息重试调度器 - 实施总结

## 任务概述

实现了 MessageRetryScheduler 定时任务，用于扫描本地消息表中的待发送消息并重试发送到 Kafka，确保事件发布的可靠性。

## 实施内容

### 1. 核心组件

#### MessageRetryScheduler.java
- **位置**: `cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/message/MessageRetryScheduler.java`
- **功能**:
  - 每 30 秒扫描一次 PENDING 状态的消息
  - 重试发送失败的消息（最多 5 次）
  - 超过最大重试次数后标记为 FAILED 并告警
  - 每天凌晨 2 点清理 7 天前的旧消息
  - 提供消息统计信息查询

#### SchedulingConfig.java
- **位置**: `cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/config/SchedulingConfig.java`
- **功能**: 启用 Spring 定时任务支持

### 2. 测试

#### MessageRetrySchedulerTest.java
- **位置**: `cuckoo-common/src/test/java/com/pingxin403/cuckoo/common/message/MessageRetrySchedulerTest.java`
- **测试覆盖**:
  - ✅ 成功重试发送待发送的消息
  - ✅ 发送失败时增加重试次数
  - ✅ 超过最大重试次数标记为失败并告警
  - ✅ 批量处理多条待发送消息
  - ✅ 没有待发送消息时直接返回
  - ✅ 清理 7 天前的旧消息
  - ✅ 反序列化失败时增加重试次数
  - ✅ 获取消息统计信息
  - ✅ 发送超时时增加重试次数
  - ✅ 处理混合成功和失败的消息

**测试结果**: 10/10 通过 ✅

### 3. 文档

#### SCHEDULER_README.md
- **位置**: `cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/message/SCHEDULER_README.md`
- **内容**:
  - 功能特性说明
  - 配置说明
  - 工作流程图
  - 使用示例
  - 监控指标
  - 告警集成方案
  - 性能考虑
  - 故障排查指南
  - 最佳实践

## 技术实现细节

### 1. 定时任务配置

```java
@Scheduled(fixedDelay = 30000, initialDelay = 10000)
public void retryPendingMessages() {
    // 每 30 秒执行一次，初始延迟 10 秒
}

@Scheduled(cron = "0 0 2 * * ?")
public void cleanupOldMessages() {
    // 每天凌晨 2 点执行
}
```

### 2. 重试策略

- **最大重试次数**: 5 次
- **发送超时**: 5 秒
- **批量处理**: 每次最多 100 条消息
- **失败处理**: 自动增加重试次数，超过限制后标记为 FAILED

### 3. 事件反序列化

使用 Jackson 的多态反序列化功能，通过 `@JsonTypeInfo` 注解自动处理类型信息：

```java
private DomainEvent deserializeEvent(LocalMessage message) throws Exception {
    return objectMapper.readValue(message.getPayload(), DomainEvent.class);
}
```

### 4. 告警机制

当前版本记录错误日志，预留了告警接口：

```java
private void sendAlert(LocalMessage message, String errorMessage) {
    // TODO: 集成告警系统（钉钉、企业微信、邮件等）
    log.error("【告警】消息发送失败: ...");
}
```

### 5. 配置开关

支持通过配置启用/禁用调度器：

```java
@ConditionalOnProperty(
    prefix = "cuckoo.message.retry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
```

## 满足的需求

### Requirement 1.5: 本地消息表保证消息可靠发送
- ✅ 定时扫描待发送消息
- ✅ 自动重试发送失败的消息
- ✅ 消息状态管理（PENDING → SENT/FAILED）

### Requirement 1.6: 消息重试和失败处理
- ✅ 最多重试 5 次
- ✅ 超过重试次数标记为 FAILED
- ✅ 发送告警通知（当前为日志，预留接口）
- ✅ 清理 7 天前的旧消息

## 性能优化

1. **批量处理**: 每次最多处理 100 条消息，避免长时间占用线程
2. **超时控制**: 每条消息发送超时 5 秒，避免阻塞
3. **数据库索引**: 使用 `idx_status_created` 索引优化查询
4. **异步发送**: 业务代码中异步发送，不阻塞主流程

## 监控和日志

### 日志级别
- **DEBUG**: 扫描开始、消息重试详情
- **INFO**: 批量处理结果、成功发送、清理完成
- **WARN**: 发送失败（会重试）
- **ERROR**: 超过最大重试次数、反序列化失败

### 关键指标
- 待发送消息数量 (pending)
- 已发送消息数量 (sent)
- 失败消息数量 (failed)
- 重试成功率
- 清理的旧消息数量

## 使用示例

### 业务代码集成

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
        
        // 4. 异步发布事件
        CompletableFuture.runAsync(() -> {
            try {
                eventPublisher.publish(event).get(5, TimeUnit.SECONDS);
                localMessageService.markAsSent(event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish event, will retry later", e);
                // 消息保持 PENDING 状态，调度器会自动重试
            }
        });
        
        return order;
    }
}
```

## 后续改进建议

### 1. 告警集成
- 集成钉钉 Webhook
- 集成企业微信 Webhook
- 集成邮件通知
- 支持告警规则配置

### 2. 监控增强
- 暴露 Prometheus 指标
- 创建 Grafana 监控面板
- 添加消息重试成功率指标
- 添加消息处理延迟指标

### 3. 配置优化
- 支持动态配置重试次数
- 支持动态配置扫描频率
- 支持动态配置批量大小
- 支持动态配置消息保留天数

### 4. 性能优化
- 支持分布式调度（避免多实例重复处理）
- 支持消息优先级
- 支持按事件类型分组处理
- 支持限流控制

## 验证清单

- [x] 代码实现完成
- [x] 单元测试通过（10/10）
- [x] 代码无编译错误
- [x] 文档完整
- [x] 满足 Requirements 1.5
- [x] 满足 Requirements 1.6
- [x] 日志记录完善
- [x] 错误处理完善
- [x] 配置开关可用

## 总结

Task 3.3 已成功完成，实现了完整的消息重试调度器功能。该组件是本地消息表模式的核心，确保了事件发布的可靠性。所有单元测试通过，代码质量良好，文档完善。

下一步可以继续执行 Task 3.4（编写本地消息表的单元测试）或根据用户需求进行其他任务。
