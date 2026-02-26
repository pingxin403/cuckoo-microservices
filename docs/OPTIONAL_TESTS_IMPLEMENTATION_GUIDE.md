# 可选测试实施指南

## 概述

本文档提供所有可选测试任务的实施指南。这些测试任务标记为可选（`[ ]*`），可以根据项目需求选择性实施。

**测试框架**:
- **单元测试**: JUnit 5 + Mockito
- **属性测试**: jqwik (Property-Based Testing)
- **集成测试**: Spring Boot Test + Testcontainers

**总计**: 20 个可选测试任务

---

## 测试分类

### Phase 1: 核心功能测试 (3个)
- 2.4 事件发布和消费的属性测试
- 3.4 本地消息表的单元测试
- 8.5 Saga 的属性测试

### Phase 2: 可观测性测试 (3个)
- 4.5 链路追踪的属性测试
- 5.5 日志收集的集成测试
- 6.6 监控告警的单元测试

### Phase 3: 性能和高可用测试 (6个)
- 9.4 健康检查的属性测试
- 10.4 优雅下线的属性测试
- 11.5 多级缓存的属性测试
- 12.6 读写分离的属性测试
- 13.6 CQRS 的属性测试
- 14.6 BFF 的属性测试

### Phase 4: 安全和高级测试 (8个)
- 16.4 mTLS 的集成测试
- 17.5 RBAC 的属性测试
- 18.5 审计日志的属性测试
- 19.5 API 文档的单元测试
- 22.6 契约测试的属性测试
- 23.6 端到端测试的属性测试
- 24.7 性能测试的属性测试
- 25.7 混沌工程的属性测试

---

## 实施优先级建议

### 高优先级 (建议立即实施)
1. **2.4 事件发布和消费的属性测试** - 验证核心事件驱动架构
2. **3.4 本地消息表的单元测试** - 验证消息可靠性
3. **8.5 Saga 的属性测试** - 验证分布式事务一致性

### 中优先级 (建议近期实施)
4. **11.5 多级缓存的属性测试** - 验证缓存策略
5. **13.6 CQRS 的属性测试** - 验证读写分离
6. **14.6 BFF 的属性测试** - 验证聚合层

### 低优先级 (可延后实施)
7. 其他可观测性和安全测试

---


## Task 2.4: 事件发布和消费的属性测试

**验证需求**: Requirements 1.5, 1.7, 1.8

### 测试属性

**Property 1: 事件发布可靠性**
```java
@Property
void allSuccessfullyPublishedEventsShouldBeRecorded(
    @ForAll List<DomainEvent> events) {
    // 验证所有成功发布的事件都被记录
}
```

**Property 2: 事件幂等性**
```java
@Property
void duplicateEventsShouldBeProcessedOnlyOnce(
    @ForAll DomainEvent event,
    @ForAll @IntRange(min = 2, max = 10) int duplicateCount) {
    // 验证相同eventId的事件只处理一次
}
```

**Property 3: 事件结构完整性**
```java
@Property
void allEventsShouldHaveRequiredFields(@ForAll DomainEvent event) {
    // 验证事件包含: eventId, eventType, timestamp, version, payload
}
```

### 实施步骤

1. 创建测试类: `EventPublishingPropertiesTest.java`
2. 使用 jqwik 生成随机事件
3. 验证事件发布和消费的正确性
4. 测试幂等性检查机制

### 注意事项

- `DomainEvent.timestamp` 类型为 `Long` (毫秒时间戳)
- `DomainEvent.version` 类型为 `Integer`
- 需要 mock `KafkaTemplate<String, DomainEvent>`

---

## Task 3.4: 本地消息表的单元测试

**验证需求**: Requirements 1.5, 1.6

### 测试场景

1. **测试事务性保存**
```java
@Test
void shouldSaveMessageInSameTransaction() {
    // 验证消息和业务数据在同一事务中保存
}
```

2. **测试重试逻辑**
```java
@Test
void shouldRetryFailedMessages() {
    // 验证失败消息会被重试
}
```

3. **测试失败告警**
```java
@Test
void shouldAlertAfterMaxRetries() {
    // 验证超过最大重试次数后发送告警
}
```

### 实施步骤

1. 创建测试类: `LocalMessageServiceTest.java`
2. 使用 `@DataJpaTest` 测试数据库操作
3. Mock `EventPublisher` 验证重试逻辑
4. 验证消息状态转换

---

