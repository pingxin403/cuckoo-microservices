# Task 20: 创建开发者门户实施指南

## 概述

开发者门户是一个统一的文档和工具入口，聚合了架构文档、API 文档、监控面板、日志查询等资源。本文档提供基于 VuePress 的实施方案。

---

## 20.1 创建开发者门户项目

### 1. 初始化 VuePress 项目

```bash
# 创建项目目录
cd cuckoo-microservices
mkdir developer-portal
cd developer-portal

# 初始化 npm 项目
npm init -y

# 安装 VuePress
npm install -D vuepress@next @vuepress/client@next vue

# 创建目录结构
mkdir -p docs/.vuepress
mkdir -p docs/architecture
mkdir -p docs/getting-started
mkdir -p docs/api
mkdir -p docs/troubleshooting
mkdir -p docs/best-practices
```

### 2. 配置 package.json

```json
{
  "name": "cuckoo-developer-portal",
  "version": "1.0.0",
  "description": "Cuckoo Microservices Developer Portal",
  "scripts": {
    "docs:dev": "vuepress dev docs",
    "docs:build": "vuepress build docs"
  },
  "devDependencies": {
    "@vuepress/client": "^2.0.0-rc.0",
    "vuepress": "^2.0.0-rc.0",
    "vue": "^3.3.0"
  }
}
```


### 3. VuePress 配置文件

```javascript
// developer-portal/docs/.vuepress/config.js
import { defaultTheme } from '@vuepress/theme-default'
import { defineUserConfig } from 'vuepress'

export default defineUserConfig({
  lang: 'zh-CN',
  title: 'Cuckoo 微服务开发者门户',
  description: '统一的文档、工具和监控入口',
  
  theme: defaultTheme({
    logo: '/images/logo.png',
    navbar: [
      { text: '首页', link: '/' },
      { text: '架构', link: '/architecture/' },
      { text: '快速开始', link: '/getting-started/' },
      { text: 'API 文档', link: '/api/' },
      { text: '故障排查', link: '/troubleshooting/' },
      { text: '最佳实践', link: '/best-practices/' },
      {
        text: '工具',
        children: [
          { text: 'Grafana 监控', link: 'http://grafana.cuckoo.local' },
          { text: 'Kibana 日志', link: 'http://kibana.cuckoo.local' },
          { text: 'Jaeger 追踪', link: 'http://jaeger.cuckoo.local' }
        ]
      }
    ],
    sidebar: {
      '/architecture/': [
        {
          text: '系统架构',
          children: [
            '/architecture/overview.md',
            '/architecture/services.md',
            '/architecture/event-driven.md',
            '/architecture/observability.md'
          ]
        }
      ],
      '/getting-started/': [
        {
          text: '快速开始',
          children: [
            '/getting-started/prerequisites.md',
            '/getting-started/local-setup.md',
            '/getting-started/first-request.md'
          ]
        }
      ]
    }
  })
})
```

---

## 20.2 编写架构文档

### 1. 系统架构概览

```markdown
<!-- developer-portal/docs/architecture/overview.md -->
# 系统架构概览

## 整体架构

Cuckoo 微服务系统采用事件驱动架构，包含以下核心组件：

- **API 网关**：统一入口，路由转发
- **业务服务**：订单、商品、库存、支付等
- **BFF 层**：移动端和 Web 端聚合层
- **事件总线**：Kafka 消息队列
- **数据存储**：MySQL、Redis
- **可观测性**：Jaeger、ELK、Prometheus + Grafana

## 架构图

![系统架构图](../images/architecture-diagram.png)

## 技术栈

- **后端框架**：Spring Boot 3.2.5
- **服务治理**：Spring Cloud Alibaba
- **消息队列**：Kafka
- **数据库**：MySQL 8.0
- **缓存**：Redis 7.0
- **容器编排**：Kubernetes
- **监控**：Prometheus + Grafana
- **日志**：ELK Stack
- **链路追踪**：Jaeger + OpenTelemetry
```


### 2. 服务职责说明

```markdown
<!-- developer-portal/docs/architecture/services.md -->
# 服务职责

## 核心服务

### 订单服务 (Order Service)
- **端口**：8084
- **职责**：订单创建、查询、取消
- **数据库**：order_db
- **API 文档**：http://localhost:8084/swagger-ui.html

### 商品服务 (Product Service)
- **端口**：8082
- **职责**：商品管理、查询
- **数据库**：product_db
- **缓存**：多级缓存（Caffeine + Redis）

### 库存服务 (Inventory Service)
- **端口**：8083
- **职责**：库存扣减、释放
- **数据库**：inventory_db

### 支付服务 (Payment Service)
- **端口**：8085
- **职责**：支付处理、退款
- **数据库**：payment_db

## BFF 服务

### Mobile BFF
- **端口**：8090
- **职责**：移动端数据聚合
- **聚合接口**：/mobile/api/home

### Web BFF
- **端口**：8091
- **职责**：Web 端数据聚合
- **聚合接口**：/web/api/products/{id}

## 基础设施

### API 网关 (Gateway)
- **端口**：8080
- **职责**：路由转发、限流、认证

### 配置中心 (Nacos)
- **端口**：8848
- **职责**：配置管理、服务发现
```

---

## 20.3 编写快速开始指南

### 1. 环境准备

```markdown
<!-- developer-portal/docs/getting-started/prerequisites.md -->
# 环境准备

## 必需软件

### JDK 17+
```bash
java -version
# 预期输出：java version "17.0.x"
```

### Maven 3.8+
```bash
mvn -version
# 预期输出：Apache Maven 3.8.x
```

### Docker & Docker Compose
```bash
docker --version
docker-compose --version
```

### Kubernetes (可选)
```bash
kubectl version --client
```

## 推荐工具

- **IDE**：IntelliJ IDEA / VS Code
- **API 测试**：Postman / Insomnia
- **数据库客户端**：DBeaver / DataGrip
- **Redis 客户端**：RedisInsight
```


### 2. 本地开发环境搭建

```markdown
<!-- developer-portal/docs/getting-started/local-setup.md -->
# 本地开发环境搭建

## 1. 克隆代码

```bash
git clone https://github.com/your-org/cuckoo-microservices.git
cd cuckoo-microservices
```

## 2. 启动基础设施

```bash
# 启动 MySQL、Redis、Kafka、Nacos
docker-compose -f docker/docker-compose-dev.yml up -d

# 验证服务状态
docker-compose -f docker/docker-compose-dev.yml ps
```

## 3. 编译项目

```bash
mvn clean install -DskipTests
```

## 4. 启动服务

### 方式 A：使用 IDE
1. 导入项目到 IntelliJ IDEA
2. 依次启动各个服务的 Application 类

### 方式 B：使用 Maven
```bash
# 启动订单服务
cd cuckoo-order
mvn spring-boot:run

# 启动商品服务
cd cuckoo-product
mvn spring-boot:run

# 启动其他服务...
```

## 5. 验证服务

```bash
# 检查服务健康状态
curl http://localhost:8084/actuator/health

# 预期输出：{"status":"UP"}
```

## 6. 访问监控面板

- **Grafana**：http://localhost:3000 (admin/admin)
- **Kibana**：http://localhost:5601
- **Jaeger**：http://localhost:16686
- **Nacos**：http://localhost:8848/nacos (nacos/nacos)
```

---

## 20.4 编写故障排查指南

### 1. 常见问题 FAQ

```markdown
<!-- developer-portal/docs/troubleshooting/faq.md -->
# 常见问题 FAQ

## 服务启动失败

### Q: 服务启动时报 "Address already in use"
**A**: 端口被占用，检查并关闭占用端口的进程
```bash
# macOS/Linux
lsof -i :8084
kill -9 <PID>

# Windows
netstat -ano | findstr :8084
taskkill /PID <PID> /F
```

### Q: 连接 MySQL 失败
**A**: 检查 MySQL 是否启动，配置是否正确
```bash
# 检查 MySQL 容器
docker ps | grep mysql

# 查看日志
docker logs mysql-master

# 测试连接
mysql -h localhost -P 3306 -u root -p
```

### Q: Kafka 消息发送失败
**A**: 检查 Kafka 集群状态
```bash
# 检查 Kafka 容器
docker ps | grep kafka

# 查看 topic 列表
docker exec -it kafka-0 kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

## 性能问题

### Q: 接口响应慢
**A**: 使用 Jaeger 查看链路追踪，定位慢查询
1. 访问 http://localhost:16686
2. 搜索对应的 traceId
3. 分析各个 span 的耗时

### Q: 数据库连接池耗尽
**A**: 检查连接池配置和慢查询
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```
```

### 2. 故障排查流程

```markdown
<!-- developer-portal/docs/troubleshooting/debugging.md -->
# 故障排查流程

## 1. 查看日志

### 应用日志
```bash
# 查看实时日志
tail -f logs/application.log

# 搜索错误日志
grep ERROR logs/application.log

# 使用 Kibana 查询
# 访问 http://localhost:5601
# 搜索：level:ERROR AND service:order-service
```

### 容器日志
```bash
# 查看容器日志
docker logs <container-name>

# 实时跟踪
docker logs -f <container-name>

# Kubernetes
kubectl logs <pod-name> -f
```

## 2. 链路追踪

1. 从错误日志中提取 traceId
2. 访问 Jaeger UI：http://localhost:16686
3. 搜索 traceId
4. 分析调用链路和耗时

## 3. 监控指标

访问 Grafana：http://localhost:3000

查看关键指标：
- QPS（每秒请求数）
- 响应时间（P50、P95、P99）
- 错误率
- JVM 内存使用
- 数据库连接池状态

## 4. 数据库排查

```bash
# 查看慢查询
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;

# 查看当前连接
SHOW PROCESSLIST;

# 查看表锁
SHOW OPEN TABLES WHERE In_use > 0;
```
```


---

## 20.5 集成 API 文档和监控链接

### 1. 创建工具导航页面

```markdown
<!-- developer-portal/docs/tools/README.md -->
# 工具和资源

## 监控和可观测性

### Grafana 监控面板
- **地址**：http://grafana.cuckoo.local
- **用户名/密码**：admin/admin
- **功能**：
  - 服务概览面板
  - JVM 监控面板
  - 业务指标面板
  - Kafka 监控面板

### Kibana 日志查询
- **地址**：http://kibana.cuckoo.local
- **功能**：
  - 日志全文搜索
  - 日志聚合分析
  - 自定义查询和过滤

### Jaeger 链路追踪
- **地址**：http://jaeger.cuckoo.local
- **功能**：
  - 分布式追踪
  - 性能分析
  - 依赖关系图

## API 文档

### 订单服务 API
- **Swagger UI**：http://localhost:8084/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8084/v3/api-docs

### 商品服务 API
- **Swagger UI**：http://localhost:8082/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8082/v3/api-docs

### 库存服务 API
- **Swagger UI**：http://localhost:8083/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8083/v3/api-docs

## 配置和管理

### Nacos 控制台
- **地址**：http://localhost:8848/nacos
- **用户名/密码**：nacos/nacos
- **功能**：
  - 服务注册与发现
  - 配置管理
  - 命名空间管理

## 数据库工具

### MySQL 连接信息
- **主库**：localhost:3306
- **从库1**：localhost:3307
- **从库2**：localhost:3308
- **用户名/密码**：root/root123

### Redis 连接信息
- **地址**：localhost:6379
- **密码**：redis123
```

### 2. 服务健康状态页面

```vue
<!-- developer-portal/docs/.vuepress/components/ServiceStatus.vue -->
<template>
  <div class="service-status">
    <h2>服务健康状态</h2>
    <table>
      <thead>
        <tr>
          <th>服务名称</th>
          <th>状态</th>
          <th>端口</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="service in services" :key="service.name">
          <td>{{ service.name }}</td>
          <td>
            <span :class="['status', service.status]">
              {{ service.status === 'up' ? '✓ 运行中' : '✗ 停止' }}
            </span>
          </td>
          <td>{{ service.port }}</td>
          <td>
            <a :href="service.healthUrl" target="_blank">健康检查</a> |
            <a :href="service.swaggerUrl" target="_blank">API 文档</a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      services: [
        {
          name: '订单服务',
          status: 'up',
          port: 8084,
          healthUrl: 'http://localhost:8084/actuator/health',
          swaggerUrl: 'http://localhost:8084/swagger-ui.html'
        },
        {
          name: '商品服务',
          status: 'up',
          port: 8082,
          healthUrl: 'http://localhost:8082/actuator/health',
          swaggerUrl: 'http://localhost:8082/swagger-ui.html'
        },
        // 其他服务...
      ]
    }
  },
  mounted() {
    this.checkServicesHealth()
    setInterval(this.checkServicesHealth, 30000) // 每 30 秒刷新
  },
  methods: {
    async checkServicesHealth() {
      for (const service of this.services) {
        try {
          const response = await fetch(service.healthUrl)
          service.status = response.ok ? 'up' : 'down'
        } catch (error) {
          service.status = 'down'
        }
      }
    }
  }
}
</script>

<style scoped>
.service-status table {
  width: 100%;
  border-collapse: collapse;
}
.service-status th,
.service-status td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}
.status.up {
  color: #42b983;
}
.status.down {
  color: #ff4444;
}
</style>
```

---

## 20.6 实现文档全文搜索

### 1. 配置搜索插件

```bash
# 安装搜索插件
npm install -D @vuepress/plugin-search@next
```

### 2. 更新配置文件

```javascript
// developer-portal/docs/.vuepress/config.js
import { searchPlugin } from '@vuepress/plugin-search'

export default defineUserConfig({
  // ... 其他配置
  
  plugins: [
    searchPlugin({
      locales: {
        '/': {
          placeholder: '搜索文档',
        },
      },
      // 搜索热键
      hotKeys: ['s', '/'],
      // 最大搜索结果数
      maxSuggestions: 10,
      // 排除的页面
      isSearchable: (page) => page.path !== '/',
    }),
  ],
})
```

---

## 部署和发布

### 1. 构建静态网站

```bash
cd developer-portal
npm run docs:build
```

### 2. 部署到 Nginx

```nginx
# nginx.conf
server {
    listen 80;
    server_name docs.cuckoo.local;
    
    root /var/www/developer-portal;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 启用 gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
}
```

### 3. 部署到 Kubernetes

```yaml
# k8s/developer-portal-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: developer-portal
spec:
  replicas: 2
  selector:
    matchLabels:
      app: developer-portal
  template:
    metadata:
      labels:
        app: developer-portal
    spec:
      containers:
      - name: nginx
        image: nginx:alpine
        ports:
        - containerPort: 80
        volumeMounts:
        - name: docs
          mountPath: /usr/share/nginx/html
      volumes:
      - name: docs
        configMap:
          name: developer-portal-docs
---
apiVersion: v1
kind: Service
metadata:
  name: developer-portal
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 80
  selector:
    app: developer-portal
```

---

## 最佳实践

1. **保持文档更新**：代码变更时同步更新文档
2. **使用版本控制**：文档和代码一起管理
3. **添加示例代码**：提供可运行的示例
4. **定期审查**：定期检查文档的准确性
5. **收集反馈**：鼓励开发者提供反馈和改进建议

---

## 总结

开发者门户提供了统一的文档和工具入口，极大提升了开发效率和团队协作。通过 VuePress 构建的静态网站，易于维护和部署，支持全文搜索和实时服务状态监控。
