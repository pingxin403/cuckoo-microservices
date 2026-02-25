# 业务指标收集模块

本模块提供统一的业务指标收集接口，用于 Prometheus 监控。

## 功能特性

- **业务指标收集**: 订单、支付、库存等业务指标
- **Kafka 指标收集**: 消费延迟、消息积压等 Kafka 指标
- **缓存指标收集**: 缓存命中率、缓存级别等指标
- **自定义指标**: 支持创建自定义计数器和计时器

## 使用方法

### 1. 业务指标收集

```java
@Service
public class OrderService {
    
    @Autowired
    private BusinessMetrics businessMetrics;
    
    public Order createOrder(OrderRequest request) {
        try {
            // 创建订单
            Order order = orderRepository.save(buildOrder(request));
            
            // 记录订单创建指标
            businessMetrics.recordOrderCreated("SUCCESS");
            businessMetrics.recordOrderAmount(order.getTotalAmount());
            
            return order;
        } catch (Exception e) {
            businessMetrics.recordOrderCreated("FAILED");
            throw e;
        }
    }
}
```

### 2. 支付指标收集

```java
@Service
public class PaymentService {
    
    @Autowired
    private BusinessMetrics businessMetrics;
    
    public Payment processPayment(PaymentRequest request) {
        try {
            // 处理支付
            Payment payment = paymentGateway.pay(request);
            
            // 记录支付指标
            businessMetrics.recordPayment(payment.isSuccess());
            
            return payment;
        } catch (Exception e) {
            businessMetrics.recordPayment(false);
            throw e;
        }
    }
}
```

### 3. 库存指标收集

```java
@Service
public class InventoryService {
    
    @Autowired
    private BusinessMetrics businessMetrics;
    
    public void deductInventory(Long productId, Integer quantity) {
        try {
            // 扣减库存
            inventoryRepository.deduct(productId, quantity);
            
            // 记录库存扣减指标
            businessMetrics.recordInventoryDeduction(true);
        } catch (InsufficientInventoryException e) {
            businessMetrics.recordInventoryDeduction(false);
            throw e;
        }
    }
}
```

### 4. 缓存指标收集

```java
@Service
public class ProductService {
    
    @Autowired
    private BusinessMetrics businessMetrics;
    
    public Product getProductById(Long productId) {
        // 查询本地缓存
        Product product = localCache.get(productId);
        if (product != null) {
            businessMetrics.recordCacheHit("local");
            return product;
        }
        
        // 查询 Redis 缓存
        product = redisCache.get(productId);
        if (product != null) {
            businessMetrics.recordCacheHit("redis");
            return product;
        }
        
        // 缓存未命中
        businessMetrics.recordCacheMiss("redis");
        
        // 查询数据库
        product = productRepository.findById(productId).orElseThrow();
        
        // 写入缓存
        localCache.put(productId, product);
        redisCache.put(productId, product);
        
        return product;
    }
}
```

### 5. Kafka 指标收集

```java
@Service
public class KafkaConsumerService {
    
    @Autowired
    private KafkaMetrics kafkaMetrics;
    
    @KafkaListener(topics = "order-events", groupId = "order-consumer-group")
    public void consumeOrderEvent(ConsumerRecord<String, String> record) {
        // 处理消息
        processMessage(record.value());
        
        // 记录消费延迟（可选，通常由监控系统自动计算）
        long lagSeconds = calculateLag(record);
        kafkaMetrics.recordConsumerLag("order-consumer-group", "order-events", lagSeconds);
    }
}
```

### 6. 自定义指标

```java
@Service
public class CustomMetricsService {
    
    @Autowired
    private BusinessMetrics businessMetrics;
    
    public void recordCustomMetric() {
        // 创建自定义计数器
        Counter customCounter = businessMetrics.createCounter(
            "custom_operation_total",
            "自定义操作总数",
            "operation", "custom",
            "status", "success"
        );
        customCounter.increment();
        
        // 创建自定义计时器
        Timer customTimer = businessMetrics.createTimer(
            "custom_operation_duration",
            "自定义操作耗时"
        );
        customTimer.record(() -> {
            // 执行操作
            performCustomOperation();
        });
    }
}
```

## 指标说明

### 业务指标

| 指标名称 | 类型 | 描述 | 标签 |
|---------|------|------|------|
| order_created_total | Counter | 订单创建总数 | application=order-service |
| order_amount | DistributionSummary | 订单金额分布 | - |
| payment_total | Counter | 支付总次数 | application=payment-service |
| payment_success_total | Counter | 支付成功次数 | application=payment-service |
| payment_failed_total | Counter | 支付失败次数 | application=payment-service |
| inventory_deducted_total | Counter | 库存扣减成功次数 | application=inventory-service |
| inventory_deduct_failed_total | Counter | 库存扣减失败次数 | application=inventory-service |
| cache_hit_rate | Gauge | 缓存命中率 | cache_level=local/redis |
| database_active_connections | Gauge | 数据库活跃连接数 | - |

### Kafka 指标

| 指标名称 | 类型 | 描述 | 标签 |
|---------|------|------|------|
| kafka_consumer_lag_seconds | Gauge | Kafka 消费者延迟时间（秒） | consumer_group, topic |
| kafka_consumer_records_lag | Gauge | Kafka 消费者消息积压数量 | consumer_group, topic, partition |
| kafka_listener_containers_total | Gauge | Kafka 监听器容器总数 | - |
| kafka_listener_containers_running | Gauge | 运行中的 Kafka 监听器容器数量 | - |

## Prometheus 查询示例

### 订单创建速率

```promql
rate(order_created_total[5m])
```

### 订单金额 P99

```promql
histogram_quantile(0.99, rate(order_amount_bucket[5m]))
```

### 支付成功率

```promql
(sum(rate(payment_success_total[5m])) / sum(rate(payment_total[5m]))) * 100
```

### 缓存命中率

```promql
cache_hit_rate{cache_level="local"}
```

### Kafka 消费延迟

```promql
kafka_consumer_lag_seconds{consumer_group="order-consumer-group"}
```

## 注意事项

1. **性能影响**: 指标收集对性能影响很小，但避免在高频操作中创建新的指标实例
2. **标签使用**: 合理使用标签，避免标签值过多导致时序数据爆炸
3. **指标命名**: 遵循 Prometheus 命名规范，使用下划线分隔单词
4. **单位**: 在指标描述中明确单位（秒、字节、百分比等）
5. **清理**: 不再使用的指标应该及时清理，避免内存泄漏

## 最佳实践

1. **使用 try-catch**: 在业务代码中使用 try-catch 确保指标记录不影响业务逻辑
2. **异步记录**: 对于非关键指标，可以考虑异步记录以减少延迟
3. **批量记录**: 对于高频操作，可以考虑批量记录指标
4. **监控告警**: 为关键指标配置告警规则，及时发现问题
5. **定期审查**: 定期审查指标使用情况，删除不再使用的指标

## 相关文档

- [Micrometer 官方文档](https://micrometer.io/docs)
- [Prometheus 指标类型](https://prometheus.io/docs/concepts/metric_types/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
