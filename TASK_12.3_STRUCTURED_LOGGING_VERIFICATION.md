# Task 12.3 结构化日志配置验证报告

## 任务概述

配置所有微服务使用统一的结构化日志格式（JSON + traceId），通过 cuckoo-common 模块提供的通用 logback-spring.xml 配置。

## 验收标准

- [x] 各服务引用 cuckoo-common 中的通用 logback-spring.xml（已在 Task 1.2 中创建）
- [x] 如有服务需要自定义日志配置，通过 `<include>` 继承通用配置后扩展
- [x] 日志输出为 JSON 格式，包含 traceId 字段以便与链路追踪关联

## 实现细节

### 1. 通用日志配置（cuckoo-common）

**位置**: `cuckoo-common/src/main/resources/logback-spring.xml`

**配置特性**:
- ✅ JSON 格式输出（使用 LogstashEncoder）
- ✅ 包含 traceId 和 spanId 字段（从 MDC 读取）
- ✅ 包含服务名称（从 spring.application.name 读取）
- ✅ 支持多环境配置（dev 使用文本格式，prod 使用 JSON 格式）
- ✅ UTC 时区统一
- ✅ 合理的日志级别配置

**关键配置片段**:
```xml
<!-- 控制台输出 - JSON 格式 -->
<appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <customFields>{"service":"${APP_NAME}"}</customFields>
        <timeZone>UTC</timeZone>
    </encoder>
</appender>
```

### 2. 依赖配置

**cuckoo-common/pom.xml** 包含 logstash-logback-encoder 依赖:
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
</dependency>
```

### 3. 各服务配置验证

所有微服务都通过依赖 cuckoo-common 自动继承结构化日志配置：

| 服务 | cuckoo-common 依赖 | 自定义 logback 配置 | 状态 |
|------|-------------------|-------------------|------|
| cuckoo-user | ✅ | ❌ | 使用通用配置 |
| cuckoo-product | ✅ | ❌ | 使用通用配置 |
| cuckoo-inventory | ✅ | ❌ | 使用通用配置 |
| cuckoo-order | ✅ | ❌ | 使用通用配置 |
| cuckoo-payment | ✅ | ❌ | 使用通用配置 |
| cuckoo-notification | ✅ | ❌ | 使用通用配置 |
| cuckoo-gateway | ✅ | ❌ | 使用通用配置 |

**说明**: 
- 所有服务都依赖 cuckoo-common，因此自动继承 logback-spring.xml 配置
- 没有服务创建自定义的 logback 配置文件，确保配置统一
- Gateway 已添加 cuckoo-common 依赖以支持结构化日志

### 4. 日志输出示例

#### 开发环境（dev profile）- 文本格式
```
2024-01-15 10:30:45.123 [http-nio-8081-exec-1] [abc123def456] INFO  c.p.c.user.controller.UserController - 用户注册请求: username=testuser
```

#### 生产环境（prod profile）- JSON 格式
```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "service": "user-service",
  "level": "INFO",
  "thread": "http-nio-8081-exec-1",
  "logger": "com.pingxin403.cuckoo.user.controller.UserController",
  "message": "用户注册请求: username=testuser",
  "traceId": "abc123def456",
  "spanId": "789ghi012jkl"
}
```

### 5. 与 OpenTelemetry 集成

结构化日志配置与 OpenTelemetry 链路追踪无缝集成：

1. **traceId 自动注入**: OpenTelemetry SDK 自动将 traceId 和 spanId 写入 MDC（Mapped Diagnostic Context）
2. **日志关联**: logback 配置从 MDC 读取 traceId 和 spanId，包含在日志输出中
3. **链路追踪关联**: 通过 traceId 可以在 Jaeger 中查找对应的链路追踪信息

### 6. 自定义日志配置扩展示例

如果某个服务需要自定义日志配置（例如添加文件输出），可以通过 `<include>` 继承通用配置：

**示例**: `cuckoo-order/src/main/resources/logback-spring.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 引入通用配置 -->
    <include resource="logback-spring.xml"/>
    
    <!-- 添加文件输出 appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/order-service.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/order-service.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- 添加文件输出到 root logger -->
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**注意**: 当前所有服务都使用通用配置，没有创建自定义配置文件。

## 验证方法

### 方法 1: 启动服务并查看日志输出

1. 启动任意微服务（例如 user-service）
2. 观察控制台日志输出格式
3. 验证日志包含 traceId 字段（如果有请求经过）

### 方法 2: 发送请求并验证 traceId

1. 启动 user-service 和 gateway
2. 通过 gateway 发送请求：
   ```bash
   curl -X POST http://localhost:8080/api/users/register \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
   ```
3. 查看 user-service 日志，验证包含 traceId
4. 在 Jaeger UI (http://localhost:16686) 中搜索相同的 traceId

### 方法 3: 验证 JSON 格式（生产环境）

1. 使用 prod profile 启动服务：
   ```bash
   java -jar cuckoo-user/target/cuckoo-user-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
   ```
2. 发送请求并查看日志输出
3. 验证日志为有效的 JSON 格式
4. 验证 JSON 包含必需字段：@timestamp, service, level, logger, message, traceId

## 配置优势

1. **统一管理**: 所有服务的日志配置集中在 cuckoo-common 中，便于维护
2. **自动继承**: 新增服务只需依赖 cuckoo-common，无需额外配置
3. **可扩展**: 支持通过 `<include>` 继承后扩展自定义配置
4. **多环境支持**: 开发环境使用易读的文本格式，生产环境使用 JSON 格式
5. **链路追踪集成**: 自动包含 traceId，便于日志与链路追踪关联
6. **标准化**: 使用 Logstash JSON 格式，便于日志收集系统（如 ELK）处理

## 符合需求

✅ **Requirements 13.6**: THE 每个微服务 SHALL 使用结构化日志格式（JSON），日志中包含 traceId 字段以便与链路追踪关联

- 所有服务通过 cuckoo-common 继承统一的 logback-spring.xml 配置
- 日志输出为 JSON 格式（生产环境）
- 日志包含 traceId 和 spanId 字段
- 与 OpenTelemetry 链路追踪无缝集成

## 总结

Task 12.3 已成功完成：

1. ✅ 通用 logback-spring.xml 配置已在 Task 1.2 中创建
2. ✅ 所有微服务（包括 gateway）都依赖 cuckoo-common，自动继承日志配置
3. ✅ 日志配置支持 JSON 格式输出，包含 traceId 字段
4. ✅ 支持多环境配置（dev/prod）
5. ✅ 支持通过 `<include>` 扩展自定义配置（虽然当前未使用）
6. ✅ 与 OpenTelemetry 链路追踪集成

所有服务现在都使用统一的结构化日志配置，满足可观测性要求。
