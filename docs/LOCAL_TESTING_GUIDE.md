# 本地测试指南

## 概述

本文档提供 Cuckoo 微服务项目的本地测试指南，包括测试运行方法、测试覆盖范围和测试结果。

---

## 快速开始

### 1. 编译检查

```bash
./run-local-tests.sh compile
```

### 2. 运行所有测试

```bash
./run-local-tests.sh all
```

### 3. 运行特定类型测试

```bash
# 单元测试
./run-local-tests.sh unit

# 集成测试
./run-local-tests.sh integration

# 属性测试
./run-local-tests.sh property
```

### 4. 运行特定模块测试

```bash
# cuckoo-common 模块
./run-local-tests.sh common

# cuckoo-order 模块
./run-local-tests.sh order
```

---

## 现有测试覆盖

### cuckoo-common 模块

#### 单元测试
- `DomainEventTest` - 领域事件基类测试
- `EventPublisherTest` - 事件发布器测试
- `KafkaEventPublisherTest` - Kafka 事件发布器测试
- `LocalMessageServiceTest` - 本地消息表服务测试
- `MessageRetrySchedulerTest` - 消息重试调度器测试
- `IdempotencyServiceTest` - 幂等性服务测试
- `TracingUtilsTest` - 追踪工具测试
- `ExceptionHandlerTest` - 异常处理器测试
- `LoggingTest` - 日志测试

#### 集成测试
- 暂无

---

## 可选测试任务

以下测试任务标记为可选（`[ ]*`），可根据需求选择性实施：

### Phase 1: 核心功能测试
- [ ] 2.4 事件发布和消费的属性测试
- [ ] 3.4 本地消息表的单元测试
- [ ] 8.5 Saga 的属性测试

### Phase 2: 可观测性测试
- [ ] 4.5 链路追踪的属性测试
- [ ] 5.5 日志收集的集成测试
- [ ] 6.6 监控告警的单元测试

### Phase 3: 性能和高可用测试
- [ ] 9.4 健康检查的属性测试
- [ ] 10.4 优雅下线的属性测试
- [ ] 11.5 多级缓存的属性测试
- [ ] 12.6 读写分离的属性测试
- [ ] 13.6 CQRS 的属性测试
- [ ] 14.6 BFF 的属性测试

### Phase 4: 安全和高级测试
- [ ] 16.4 mTLS 的集成测试
- [ ] 17.5 RBAC 的属性测试
- [ ] 18.5 审计日志的属性测试
- [ ] 19.5 API 文档的单元测试
- [ ] 22.6 契约测试的属性测试
- [ ] 23.6 端到端测试的属性测试
- [ ] 24.7 性能测试的属性测试
- [ ] 25.7 混沌工程的属性测试

详细实施指南请参考: `OPTIONAL_TESTS_IMPLEMENTATION_GUIDE.md`

---

## 测试环境要求

### 必需
- Java 17+
- Maven 3.8+

### 可选（用于集成测试）
- Docker（用于 Testcontainers）
- Kafka
- MySQL
- Redis

---

## 测试最佳实践

### 1. 单元测试
- 使用 JUnit 5 + Mockito
- 测试单个类或方法
- Mock 外部依赖
- 快速执行

### 2. 集成测试
- 使用 Spring Boot Test
- 测试多个组件集成
- 使用 Testcontainers 管理依赖
- 可能较慢

### 3. 属性测试
- 使用 jqwik
- 测试通用属性和不变量
- 生成随机测试数据
- 发现边界情况

---

## 持续集成

项目配置了 GitHub Actions CI/CD 流程：

- 每次 push 和 PR 自动运行测试
- 编译检查
- 单元测试
- 代码覆盖率报告

---

## 故障排查

### 编译失败
```bash
# 清理并重新编译
mvn clean compile -DskipTests
```

### 测试失败
```bash
# 查看详细错误信息
mvn test -X
```

### 依赖问题
```bash
# 更新依赖
mvn clean install -U
```

---

## 测试报告

测试报告生成位置：
- 单元测试报告: `target/surefire-reports/`
- 集成测试报告: `target/failsafe-reports/`
- 代码覆盖率报告: `target/site/jacoco/`

---

## 下一步

1. **实施高优先级可选测试** - 从核心功能测试开始
2. **提高测试覆盖率** - 目标 80%+
3. **添加集成测试** - 测试服务间集成
4. **性能测试** - 建立性能基准

---

**最后更新**: 2026-02-25
