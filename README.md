# Cuckoo Microservices - 微服务学习项目

## 项目简介

Cuckoo Microservices 是一个基于 **Spring Boot 3.x + Spring Cloud Alibaba** 的微服务学习项目，以简化版电商订单系统为业务场景。项目涵盖微服务架构的核心概念：服务拆分、服务注册发现、配置中心、API 网关、分布式事务、事件驱动、熔断降级、可观测性等。

本项目采用 **Docker Compose** 一键启动所有基础设施依赖，支持本地快速搭建完整的微服务开发环境，适合学习和实验微服务架构。

### 核心特性

- ✅ **服务拆分**：6 个业务微服务 + 1 个 API 网关
- ✅ **服务注册与发现**：基于 Nacos 实现服务自动注册和动态发现
- ✅ **配置中心**：Nacos Config 支持配置集中管理和动态刷新
- ✅ **API 网关**：Spring Cloud Gateway 统一路由和负载均衡
- ✅ **分布式事务**：Seata AT 模式保障跨服务数据一致性
- ✅ **事件驱动架构**：Kafka 实现服务间异步解耦和消息可靠性
- ✅ **熔断降级**：Sentinel 提供流量控制和容错机制
- ✅ **链路追踪**：OpenTelemetry + Jaeger 可视化请求调用链
- ✅ **指标监控**：Prometheus + Grafana 实时监控服务运行状态
- ✅ **结构化日志**：JSON 格式日志 + traceId 关联

## 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          客户端 (浏览器/Postman)                      │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │   API Gateway :8080    │
                    │  (Spring Cloud Gateway)│
                    └────────────┬───────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
        ▼                        ▼                        ▼
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│ User Service  │      │Product Service│      │Inventory Svc  │
│    :8081      │      │    :8082      │      │    :8083      │
└───────┬───────┘      └───────┬───────┘      └───────┬───────┘
        │                      │                      │
        │              ┌───────┴───────┐              │
        │              ▼               ▼              │
        │      ┌───────────────┐ ┌───────────────┐   │
        │      │ Order Service │ │Payment Service│   │
        │      │    :8084      │ │    :8085      │   │
        │      └───────┬───────┘ └───────┬───────┘   │
        │              │                 │            │
        │              └────────┬────────┘            │
        │                       ▼                     │
        │              ┌───────────────┐              │
        │              │Notification   │              │
        │              │Service :8086  │              │
        │              └───────────────┘              │
        │                                             │
        └─────────────────┬───────────────────────────┘
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
        ▼                                   ▼
┌──────────────────┐              ┌──────────────────┐
│  基础设施层       │              │  可观测性层       │
│                  │              │                  │
│ • Nacos :8848    │              │ • Jaeger :16686  │
│ • MySQL :3306    │              │ • Prometheus     │
│ • Redis :6379    │              │   :9090          │
│ • Kafka :9092    │              │ • Grafana :3000  │
│ • Seata :8091    │              │                  │
└──────────────────┘              └──────────────────┘
```

## 技术栈

### 核心框架
- **Java 17**
- **Spring Boot 3.2.x**
- **Spring Cloud Alibaba 2023.0.x**
- **Maven 3.8+**

### 微服务组件
- **Nacos 2.3.x** - 服务注册中心 + 配置中心
- **Spring Cloud Gateway** - API 网关
- **OpenFeign** - 声明式 HTTP 客户端
- **Seata 2.0.x** - 分布式事务
- **Sentinel 1.8.x** - 流量控制和熔断降级

### 消息队列
- **Kafka 3.6.x** - 分布式消息队列

### 数据存储
- **MySQL 8.0** - 关系型数据库
- **Redis 7.x** - 缓存 + 分布式锁

### 可观测性
- **OpenTelemetry** - 链路追踪数据采集
- **Jaeger 1.53** - 分布式链路追踪
- **Prometheus 2.49** - 指标监控
- **Grafana 10.3** - 可视化监控面板

## 环境要求

在开始之前，请确保您的开发环境满足以下要求：

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| **JDK** | 17+ | 推荐使用 OpenJDK 17 或 Oracle JDK 17 |
| **Maven** | 3.8+ | 用于项目构建和依赖管理 |
| **Docker** | 20.10+ | 用于运行基础设施容器 |
| **Docker Compose** | 2.0+ | 用于容器编排 |
| **Git** | 2.0+ | 版本控制 |

### 验证环境

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker-compose --version
```

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd cuckoo-microservices
```

### 2. 启动基础设施

使用 Docker Compose 一键启动所有基础设施依赖（MySQL、Redis、Nacos、Kafka、Seata、Jaeger、Prometheus、Grafana）：

```bash
docker-compose up -d
```

等待所有容器启动完成（约 1-2 分钟），可以通过以下命令查看容器状态：

```bash
docker-compose ps
```

所有容器的 `STATUS` 应显示为 `Up` 或 `healthy`。

### 3. 验证基础设施

访问以下地址验证基础设施是否正常运行：

| 服务 | 地址 | 用户名/密码 | 说明 |
|------|------|------------|------|
| **Nacos 控制台** | http://localhost:8848/nacos | nacos/nacos | 服务注册与配置中心 |
| **Jaeger UI** | http://localhost:16686 | - | 链路追踪可视化 |
| **Prometheus** | http://localhost:9090 | - | 指标监控 |
| **Grafana** | http://localhost:3000 | admin/admin | 监控面板 |

### 4. 编译项目

在项目根目录执行 Maven 编译：

```bash
mvn clean install -DskipTests
```

### 5. 启动微服务

按照以下顺序启动各微服务（建议使用多个终端窗口）：

```bash
# 终端 1: 启动用户服务
cd cuckoo-user
mvn spring-boot:run

# 终端 2: 启动商品服务
cd cuckoo-product
mvn spring-boot:run

# 终端 3: 启动库存服务
cd cuckoo-inventory
mvn spring-boot:run

# 终端 4: 启动订单服务
cd cuckoo-order
mvn spring-boot:run

# 终端 5: 启动支付服务
cd cuckoo-payment
mvn spring-boot:run

# 终端 6: 启动通知服务
cd cuckoo-notification
mvn spring-boot:run

# 终端 7: 启动 API 网关
cd cuckoo-gateway
mvn spring-boot:run
```

**提示**：也可以使用 IDE（如 IntelliJ IDEA）直接运行各服务的主类。

### 6. 验证服务注册

访问 Nacos 控制台 http://localhost:8848/nacos，在 **服务管理 > 服务列表** 中应该能看到以下 7 个服务：

- `api-gateway`
- `user-service`
- `product-service`
- `inventory-service`
- `order-service`
- `payment-service`
- `notification-service`

### 7. 测试 API

所有 API 请求通过 API 网关访问（端口 8080）。以下是一些测试示例：

#### 7.1 用户注册

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

#### 7.2 用户登录

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

#### 7.3 创建商品

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "price": 7999.00,
    "description": "Apple iPhone 15 Pro 256GB"
  }'
```

#### 7.4 初始化库存

```bash
curl -X POST http://localhost:8080/api/inventory/init \
  -H "Content-Type: application/json" \
  -d '{
    "skuId": 1,
    "totalStock": 100
  }'
```

#### 7.5 创建订单

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "skuId": 1,
    "quantity": 2
  }'
```

#### 7.6 确认支付

```bash
# 假设支付单 ID 为 1
curl -X POST http://localhost:8080/api/payments/1/confirm
```

#### 7.7 查询用户通知

```bash
# 假设用户 ID 为 1
curl http://localhost:8080/api/notifications/user/1
```

## 微服务端口和 API 列表

### API 网关 (cuckoo-gateway)

- **端口**: 8080
- **说明**: 统一入口，所有业务 API 通过网关访问

### 用户服务 (cuckoo-user)

- **端口**: 8081
- **数据库**: user_db

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/users/register` | 用户注册 |
| POST | `/api/users/login` | 用户登录 |
| GET | `/api/users/{id}` | 查询用户信息 |

### 商品服务 (cuckoo-product)

- **端口**: 8082
- **数据库**: product_db

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/products` | 创建商品 |
| GET | `/api/products/{id}` | 查询商品详情 |
| GET | `/api/products` | 查询商品列表 |

### 库存服务 (cuckoo-inventory)

- **端口**: 8083
- **数据库**: inventory_db

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/inventory/init` | 初始化库存 |
| POST | `/api/inventory/reserve` | 预占库存 |
| POST | `/api/inventory/deduct` | 扣减库存 |
| POST | `/api/inventory/release` | 释放库存 |
| GET | `/api/inventory/{skuId}` | 查询库存信息 |

### 订单服务 (cuckoo-order)

- **端口**: 8084
- **数据库**: order_db

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/orders` | 创建订单 |
| GET | `/api/orders/{id}` | 查询订单详情 |
| GET | `/api/orders/user/{userId}` | 查询用户订单列表 |
| PUT | `/api/orders/{id}/cancel` | 取消订单 |

### 支付服务 (cuckoo-payment)

- **端口**: 8085
- **数据库**: payment_db

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/payments` | 创建支付单 |
| POST | `/api/payments/{id}/confirm` | 确认支付（模拟支付成功）|
| POST | `/api/payments/{id}/fail` | 支付失败（模拟支付失败）|
| GET | `/api/payments/{id}` | 查询支付单详情 |

### 通知服务 (cuckoo-notification)

- **端口**: 8086
- **数据库**: notification_db

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/notifications/user/{userId}` | 查询用户通知列表 |

## 核心业务流程

### 下单流程

```
1. 用户通过 API 网关创建订单
   ↓
2. 订单服务开启 Seata 全局事务
   ↓
3. 调用商品服务查询商品信息（价格、名称）
   ↓
4. 调用库存服务预占库存（使用 Redis 分布式锁）
   ↓
5. 创建订单记录（状态：待支付）
   ↓
6. Seata 全局事务提交
   ↓
7. 调用支付服务创建支付单
   ↓
8. 返回订单详情 + 支付单 ID
```

### 支付流程

```
1. 用户确认支付（调用支付服务 confirm 接口）
   ↓
2. 支付服务更新支付单状态为"支付成功"
   ↓
3. 发布 PaymentSuccessEvent 到 Kafka
   ↓
4. 订单服务消费事件 → 更新订单状态为"已支付"
   ↓
5. 库存服务消费事件 → 扣减库存（预占 → 已扣减）
   ↓
6. 通知服务消费事件 → 创建支付成功通知
```

### 订单超时取消

```
1. 订单服务定时任务（每 5 分钟）扫描超时订单
   ↓
2. 发现状态为"待支付"且超过 30 分钟的订单
   ↓
3. 更新订单状态为"已取消"，设置取消原因为"支付超时"
   ↓
4. 发布 OrderCancelledEvent 到 Kafka
   ↓
5. 库存服务消费事件 → 释放预占库存
   ↓
6. 通知服务消费事件 → 创建订单取消通知
```

## 可观测性

### 链路追踪 (Jaeger)

访问 http://localhost:16686 查看分布式链路追踪：

- 选择服务名称（如 `order-service`）
- 点击 **Find Traces** 查看请求调用链
- 可以看到请求在各微服务间的传播路径和耗时

### 指标监控 (Prometheus + Grafana)

1. **Prometheus**: http://localhost:9090
   - 查询指标：`http_server_requests_seconds_count`
   - 查看各服务的请求量、响应时间等

2. **Grafana**: http://localhost:3000 (admin/admin)
   - 预配置了微服务监控面板
   - 展示各服务的请求量、响应时间 P99、错误率、JVM 内存使用等

### 结构化日志

所有微服务输出 JSON 格式日志，包含 `traceId` 字段，可以通过 traceId 关联链路追踪：

```json
{
  "timestamp": "2024-01-15T10:30:45.123+08:00",
  "level": "INFO",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "1234567890abcdef",
  "service": "order-service",
  "message": "订单创建成功",
  "orderId": "ORD20240115001"
}
```

## 项目结构

```
cuckoo-microservices/
├── pom.xml                          # 父 POM（统一依赖版本管理）
├── docker-compose.yml               # 基础设施容器编排
├── docker/                          # Docker 相关配置
│   ├── mysql/init/                  # MySQL 初始化脚本
│   ├── prometheus/prometheus.yml    # Prometheus 抓取配置
│   └── grafana/dashboards/          # Grafana 预配置仪表盘
├── nacos-config/                    # Nacos 共享配置
│   └── shared-config.yml            # 公共配置（MySQL、Kafka、Redis）
├── cuckoo-common/                   # 公共模块
│   └── src/main/java/.../common/
│       ├── event/                   # 领域事件定义
│       ├── dto/                     # 公共 DTO
│       ├── exception/               # 公共异常 + 全局异常处理器
│       ├── idempotency/             # 幂等性检查组件
│       └── kafka/                   # Kafka 消费者抽象基类
├── cuckoo-gateway/                  # API 网关
├── cuckoo-user/                     # 用户服务
├── cuckoo-product/                  # 商品服务
├── cuckoo-inventory/                # 库存服务
├── cuckoo-order/                    # 订单服务
├── cuckoo-payment/                  # 支付服务
└── cuckoo-notification/             # 通知服务
```

## 常见问题

### 1. 容器启动失败

**问题**: Docker Compose 启动时某些容器无法启动

**解决方案**:
- 检查端口是否被占用：`lsof -i :3306` (macOS/Linux) 或 `netstat -ano | findstr :3306` (Windows)
- 查看容器日志：`docker-compose logs <service-name>`
- 重启 Docker 服务

### 2. 服务无法注册到 Nacos

**问题**: 微服务启动后在 Nacos 控制台看不到服务

**解决方案**:
- 确认 Nacos 容器已启动：`docker-compose ps nacos`
- 检查服务的 `application.yml` 中 Nacos 地址配置是否正确
- 查看服务启动日志，确认是否有连接 Nacos 的错误信息

### 3. Kafka 消息消费失败

**问题**: 事件发布后消费者没有收到消息

**解决方案**:
- 确认 Kafka 和 Zookeeper 容器已启动
- 检查 Topic 是否已创建：`docker exec -it cuckoo-kafka kafka-topics --bootstrap-server localhost:9092 --list`
- 查看消费者日志，确认是否有反序列化错误

### 4. 分布式事务回滚失败

**问题**: Seata 全局事务回滚不生效

**解决方案**:
- 确认 Seata Server 容器已启动
- 检查各服务的 `application.yml` 中 Seata 配置是否正确
- 确认数据库表中是否有 `undo_log` 表（Seata AT 模式需要）

### 5. 链路追踪数据看不到

**问题**: Jaeger UI 中看不到链路追踪数据

**解决方案**:
- 确认 Jaeger 容器已启动
- 检查各服务的 OpenTelemetry 配置是否正确
- 确认 `management.tracing.sampling.probability` 设置为 1.0（100% 采样）

## 停止服务

### 停止微服务

在各微服务的终端窗口按 `Ctrl+C` 停止服务。

### 停止基础设施

```bash
# 停止所有容器
docker-compose stop

# 停止并删除所有容器
docker-compose down

# 停止并删除所有容器和数据卷（会清空数据库数据）
docker-compose down -v
```

## 学习路径建议

1. **第一阶段：单体服务**
   - 理解 Spring Boot 基础
   - 学习 RESTful API 设计
   - 掌握 JPA 数据持久化

2. **第二阶段：服务拆分**
   - 理解微服务拆分原则
   - 学习服务注册与发现（Nacos）
   - 掌握服务间通信（OpenFeign）

3. **第三阶段：分布式能力**
   - 学习分布式事务（Seata）
   - 掌握分布式锁（Redis）
   - 理解事件驱动架构（Kafka）

4. **第四阶段：容错与治理**
   - 学习熔断降级（Sentinel）
   - 掌握 API 网关（Spring Cloud Gateway）
   - 理解配置中心（Nacos Config）

5. **第五阶段：可观测性**
   - 学习链路追踪（OpenTelemetry + Jaeger）
   - 掌握指标监控（Prometheus + Grafana）
   - 理解结构化日志

## 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Cloud Alibaba 官方文档](https://spring-cloud-alibaba-group.github.io/github-pages/2023/zh-cn/index.html)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Seata 官方文档](https://seata.io/zh-cn/docs/overview/what-is-seata.html)
- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/docs/introduction.html)
- [Kafka 官方文档](https://kafka.apache.org/documentation/)
- [OpenTelemetry 官方文档](https://opentelemetry.io/docs/)

## 许可证

本项目仅用于学习和研究目的。

## 联系方式

如有问题或建议，欢迎提交 Issue 或 Pull Request。

---

**Happy Learning! 🚀**
