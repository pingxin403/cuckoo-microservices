# 性能测试计划

## 概述

本文档描述了微服务系统的性能测试计划，用于验证系统优化后的性能指标。

## 测试目标

- 验证系统 QPS 提升至少 50%
- 验证 P99 响应时间降低至 200ms 以内
- 验证缓存命中率达到 80% 以上

## 测试环境要求

### 基础设施
- MySQL 8.0
- Redis 7.0
- Kafka 3.x
- Nacos 2.x
- Sentinel Dashboard

### 服务部署
- 所有微服务正常运行
- 所有服务已注册到 Nacos
- Redis 缓存已启用
- Sentinel 限流规则已配置

## 测试工具

推荐使用 Apache JMeter 5.x 进行性能测试。

### JMeter 安装

```bash
# macOS
brew install jmeter

# 或下载二进制包
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
```

## 测试场景

### 场景 1：用户服务性能测试

**测试接口**: `GET /api/users/{id}`

**测试配置**:
- 线程数: 100
- Ramp-up 时间: 10 秒
- 循环次数: 1000
- 持续时间: 5 分钟

**预期结果**:
- QPS: > 500
- P99 响应时间: < 200ms
- 错误率: < 1%

**JMeter 配置示例**:
```xml
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">10</stringProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>
```

### 场景 2：商品查询性能测试

**测试接口**: `GET /api/products/{id}`

**测试配置**:
- 线程数: 200
- Ramp-up 时间: 20 秒
- 循环次数: 2000
- 持续时间: 5 分钟

**预期结果**:
- QPS: > 1000 (由于 Sentinel 限流配置为 QPS 50，实际会被限流)
- P99 响应时间: < 200ms
- 缓存命中率: > 80%

### 场景 3：订单创建性能测试

**测试接口**: `POST /api/orders`

**测试配置**:
- 线程数: 50
- Ramp-up 时间: 10 秒
- 循环次数: 500
- 持续时间: 5 分钟

**预期结果**:
- QPS: > 100 (受限于 Sentinel 配置 QPS 20)
- P99 响应时间: < 500ms (订单创建涉及多个服务调用)
- 错误率: < 1%

### 场景 4：库存操作性能测试

**测试接口**: `POST /api/inventory/reserve`

**测试配置**:
- 线程数: 100
- Ramp-up 时间: 10 秒
- 循环次数: 1000
- 持续时间: 5 分钟

**预期结果**:
- QPS: > 200
- P99 响应时间: < 200ms
- 分布式锁正常工作，无库存超卖

## 缓存性能测试

### 测试步骤

1. **预热缓存**
   ```bash
   # 调用 100 次商品查询接口，预热缓存
   for i in {1..100}; do
     curl http://localhost:8082/api/products/1
   done
   ```

2. **监控缓存命中率**
   ```bash
   # 连接 Redis 查看统计信息
   redis-cli INFO stats | grep keyspace_hits
   redis-cli INFO stats | grep keyspace_misses
   
   # 计算命中率
   # 命中率 = hits / (hits + misses)
   ```

3. **验证缓存 TTL**
   ```bash
   # 查看缓存键的 TTL
   redis-cli TTL "product:1"
   redis-cli TTL "user:1"
   redis-cli TTL "inventory:100"
   ```

### 预期结果

- 商品服务缓存命中率: > 80%
- 用户服务缓存命中率: > 80%
- 库存服务缓存命中率: > 70%

## Sentinel 限流测试

### 测试步骤

1. **快速发送请求超过限流阈值**
   ```bash
   # 使用 ab (Apache Bench) 快速发送请求
   ab -n 100 -c 10 http://localhost:8081/api/users/register
   ```

2. **验证限流响应**
   - 预期返回 HTTP 429 (Too Many Requests)
   - 预期返回 Sentinel 限流提示信息

3. **查看 Sentinel Dashboard**
   - 访问 http://localhost:8858
   - 查看实时 QPS 监控
   - 查看限流规则生效情况

## 性能对比测试

### 测试方法

1. **禁用缓存测试**
   - 临时注释掉 RedisConfig
   - 重启服务
   - 运行性能测试
   - 记录 QPS 和响应时间

2. **启用缓存测试**
   - 启用 RedisConfig
   - 重启服务
   - 运行性能测试
   - 记录 QPS 和响应时间

3. **计算性能提升**
   ```
   QPS 提升 = (启用缓存 QPS - 禁用缓存 QPS) / 禁用缓存 QPS * 100%
   响应时间降低 = (禁用缓存响应时间 - 启用缓存响应时间) / 禁用缓存响应时间 * 100%
   ```

### 预期结果

- QPS 提升: > 50%
- P99 响应时间降低: > 30%

## 测试报告模板

### 性能测试报告

**测试日期**: YYYY-MM-DD

**测试环境**:
- 服务器配置: [CPU/内存/磁盘]
- 数据库配置: [MySQL 配置]
- 缓存配置: [Redis 配置]

**测试结果**:

| 测试场景 | 线程数 | QPS | P50 响应时间 | P99 响应时间 | 错误率 | 是否达标 |
|---------|--------|-----|-------------|-------------|--------|---------|
| 用户查询 | 100 | 520 | 150ms | 180ms | 0.1% | ✓ |
| 商品查询 | 200 | 1050 | 80ms | 150ms | 0.2% | ✓ |
| 订单创建 | 50 | 120 | 300ms | 450ms | 0.5% | ✓ |
| 库存操作 | 100 | 250 | 120ms | 190ms | 0.1% | ✓ |

**缓存性能**:

| 服务 | 缓存命中率 | 平均响应时间(缓存命中) | 平均响应时间(缓存未命中) | 是否达标 |
|-----|-----------|---------------------|----------------------|---------|
| 用户服务 | 85% | 50ms | 200ms | ✓ |
| 商品服务 | 88% | 40ms | 180ms | ✓ |
| 库存服务 | 75% | 60ms | 220ms | ✓ |

**性能提升对比**:

| 指标 | 优化前 | 优化后 | 提升幅度 | 是否达标 |
|-----|--------|--------|---------|---------|
| 系统 QPS | 300 | 520 | 73% | ✓ |
| P99 响应时间 | 350ms | 180ms | 49% | ✓ |
| 缓存命中率 | N/A | 83% | N/A | ✓ |

**结论**:
- ✓ 系统 QPS 提升 73%，超过目标 50%
- ✓ P99 响应时间降低至 180ms，达到目标 200ms 以内
- ✓ 缓存命中率达到 83%，超过目标 80%

## 故障排查

### 常见问题

1. **QPS 未达标**
   - 检查数据库连接池配置
   - 检查 Redis 连接是否正常
   - 检查 Sentinel 限流规则是否过于严格
   - 检查服务器资源使用情况

2. **响应时间过长**
   - 检查数据库查询是否有慢查询
   - 检查缓存是否正常工作
   - 检查网络延迟
   - 检查是否有死锁或资源竞争

3. **缓存命中率低**
   - 检查缓存 TTL 配置是否合理
   - 检查缓存键是否正确
   - 检查缓存更新策略是否正确
   - 检查 Redis 内存是否充足

## 持续监控

### 生产环境监控指标

- QPS 实时监控
- 响应时间分布 (P50, P90, P99)
- 错误率监控
- 缓存命中率监控
- 服务器资源使用率 (CPU, 内存, 磁盘, 网络)
- 数据库连接池使用情况
- Redis 内存使用情况

### 告警规则

- QPS 下降超过 30%
- P99 响应时间超过 500ms
- 错误率超过 5%
- 缓存命中率低于 60%
- CPU 使用率超过 80%
- 内存使用率超过 85%

## 参考资料

- [Apache JMeter 官方文档](https://jmeter.apache.org/usermanual/index.html)
- [Redis 性能优化指南](https://redis.io/docs/management/optimization/)
- [Sentinel 性能测试最佳实践](https://sentinelguard.io/zh-cn/docs/performance-test.html)
