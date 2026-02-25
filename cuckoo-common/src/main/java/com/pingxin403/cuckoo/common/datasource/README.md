# 数据库读写分离组件

本组件实现了 MySQL 主从复制的读写分离功能，支持动态数据源路由、从库故障切换和写后读一致性。

## 功能特性

### 1. 动态数据源路由

- **DynamicDataSource**: 动态数据源，根据上下文自动选择主库或从库
- **DataSourceContextHolder**: 线程本地数据源上下文持有者
- **DataSourceType**: 数据源类型枚举（MASTER/SLAVE）

### 2. 读写分离 AOP

- **DataSourceAspect**: 自动切换数据源的切面
- **@ReadOnly**: 标记只读方法的注解

路由规则：
1. `@Transactional` 注解的方法 → 主库
2. `@ReadOnly` 注解的方法 → 从库（除非 `forceMaster=true`）
3. 写后读场景 → 主库（确保读取到最新数据）
4. 方法名以 `get/find/query/select/count/list` 开头 → 从库
5. 其他方法 → 主库

### 3. 从库故障切换

- **LoadBalancedDataSource**: 负载均衡数据源，支持多从库轮询
- **DataSourceHealthChecker**: 数据源健康检查器
- 自动检测从库故障并切换到其他从库或主库
- 支持从库恢复后自动重新加入

### 4. 写后读一致性

- **WriteAfterReadDetector**: 写后读检测器
- 检测同一请求上下文中的写后读场景
- 自动强制路由到主库，确保读取到最新数据
- 支持 10 秒时间窗口内的写后读检测

## 配置说明

### 1. 数据源配置

在 `application.yml` 中配置主从数据源：

```yaml
spring:
  datasource:
    master:
      jdbc-url: jdbc:mysql://mysql-master:3306/order_db?useSSL=false&serverTimezone=Asia/Shanghai
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
    
    slaves:
      - jdbc-url: jdbc:mysql://mysql-slave1:3306/order_db?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: root
        driver-class-name: com.mysql.cj.jdbc.Driver
        maximum-pool-size: 20
        minimum-idle: 5
      
      - jdbc-url: jdbc:mysql://mysql-slave2:3306/order_db?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: root
        driver-class-name: com.mysql.cj.jdbc.Driver
        maximum-pool-size: 20
        minimum-idle: 5
```

### 2. 启用读写分离

在 Spring Boot 应用中，组件会自动配置。确保：

1. 主库配置了 `spring.datasource.master.jdbc-url`
2. 至少配置了一个从库（可选，未配置时读操作也会路由到主库）

## 使用示例

### 1. 使用 @ReadOnly 注解

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    // 写操作，自动路由到主库
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        // ... 设置订单信息
        return orderRepository.save(order);
    }
    
    // 读操作，自动路由到从库
    @ReadOnly
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    // 强制使用主库的读操作（用于对一致性要求高的场景）
    @ReadOnly(forceMaster = true)
    public Order getOrderByIdFromMaster(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    // 根据方法名自动路由到从库
    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}
```

### 2. 类级别的 @ReadOnly 注解

```java
@Service
@ReadOnly  // 类中所有方法默认使用从库
public class OrderQueryService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    // 自动路由到从库
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
    
    // 自动路由到从库
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    // 强制使用主库
    @ReadOnly(forceMaster = true)
    public Order getLatestOrder(Long userId) {
        return orderRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
    }
}
```

### 3. 手动控制数据源

```java
@Service
public class CustomService {
    
    public void customLogic() {
        try {
            // 强制使用主库
            DataSourceContextHolder.forceMaster();
            // ... 执行需要主库的操作
            
            // 切换到从库
            DataSourceContextHolder.useSlave();
            // ... 执行只读操作
            
        } finally {
            // 清除上下文
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

### 4. 写后读场景

写后读场景会自动检测并路由到主库：

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    // 创建订单（写操作）
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        // ... 设置其他字段
        return orderRepository.save(order);
    }
    
    // 立即查询刚创建的订单
    // 系统会检测到这是写后读场景，自动路由到主库
    @ReadOnly
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
}
```

在同一请求中：
```java
// 1. 创建订单
Order order = orderService.createOrder(request);

// 2. 立即查询（10秒内）
// 自动检测到写后读场景，路由到主库
Order queriedOrder = orderService.getOrderById(order.getId());
```

## 监控指标

### 1. 数据源健康状态

```java
@Autowired
private DataSourceHealthChecker healthChecker;

// 检查从库健康状态
boolean isHealthy = healthChecker.isHealthy("slave-1");

// 获取失败次数
int failureCount = healthChecker.getFailureCount("slave-1");
```

### 2. 写后读缓存大小

```java
@Autowired
private WriteAfterReadDetector detector;

// 获取全局写缓存大小
int cacheSize = detector.getGlobalWriteCacheSize();
```

### 3. 负载均衡状态

```java
@Autowired
@Qualifier("slaveDataSource")
private LoadBalancedDataSource slaveDataSource;

// 获取从库总数
int totalCount = slaveDataSource.getDataSourceCount();

// 获取健康的从库数量
int healthyCount = slaveDataSource.getHealthyDataSourceCount();
```

## 故障处理

### 1. 从库不可用

当从库不可用时：
1. 系统会自动尝试其他从库
2. 如果所有从库都不可用，自动回退到主库
3. 从库恢复后会自动重新加入负载均衡

### 2. 主库不可用

主库不可用时：
- 写操作会失败（符合预期）
- 读操作会尝试从库
- 建议配置数据库高可用方案（如 MHA、MGR）

### 3. 主从延迟

对于主从延迟敏感的场景：
1. 使用 `@ReadOnly(forceMaster = true)` 强制从主库读取
2. 系统会自动检测写后读场景并路由到主库
3. 监控主从延迟，超过阈值时告警

## 性能优化

### 1. 连接池配置

根据实际负载调整连接池参数：

```yaml
spring:
  datasource:
    master:
      maximum-pool-size: 50  # 主库连接池大小
      minimum-idle: 10
    slaves:
      - maximum-pool-size: 30  # 从库连接池大小
        minimum-idle: 5
```

### 2. 从库数量

- 根据读写比例配置从库数量
- 读多写少的场景建议配置 2-3 个从库
- 每个从库建议配置独立的物理机器

### 3. 写后读时间窗口

默认时间窗口为 10 秒，可根据实际主从延迟调整：

```java
// 在 WriteAfterReadDetector 中修改
private static final long WRITE_AFTER_READ_WINDOW = 10000; // 毫秒
```

## 注意事项

1. **事务中的操作**：所有在 `@Transactional` 中的操作都会路由到主库
2. **跨服务调用**：数据源上下文不会跨服务传递，每个服务独立判断
3. **异步操作**：异步方法中需要重新设置数据源上下文
4. **批量操作**：批量读取建议使用从库，批量写入必须使用主库
5. **定时任务**：定时任务中的数据源需要显式指定

## 测试

### 1. 单元测试

```java
@SpringBootTest
class DataSourceRoutingTest {
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void testReadFromSlave() {
        // 验证读操作路由到从库
        Order order = orderService.getOrderById(1L);
        assertNotNull(order);
    }
    
    @Test
    void testWriteToMaster() {
        // 验证写操作路由到主库
        Order order = orderService.createOrder(new OrderRequest());
        assertNotNull(order.getId());
    }
}
```

### 2. 集成测试

使用 Docker Compose 启动 MySQL 主从集群进行集成测试。

## 参考资料

- [MySQL 主从复制](../../../../../docker/mysql/README.md)
- [Spring AbstractRoutingDataSource](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html)
- [HikariCP 配置](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
