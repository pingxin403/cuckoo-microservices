# Task 4.4: 集成日志和追踪 - Implementation Summary

## Overview

Task 4.4 integrates logging with distributed tracing by configuring Logback to output `traceId` and `spanId` in log messages. This enables correlation between logs and distributed traces in Jaeger.

## Implementation Details

### 1. Logback Configuration

**File**: `cuckoo-common/src/main/resources/logback-spring.xml`

The configuration provides two output formats:

#### Production Format (JSON)

```xml
<appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <customFields>{"service":"${APP_NAME}"}</customFields>
        <timeZone>UTC</timeZone>
    </encoder>
</appender>
```

**Output Example**:
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.pingxin403.cuckoo.order.service.OrderService",
  "message": "Order created successfully",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "service": "cuckoo-order"
}
```

#### Development Format (Text)

```xml
<appender name="CONSOLE_TEXT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [traceId=%X{traceId:-} spanId=%X{spanId:-}] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

**Output Example**:
```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7] INFO  c.p.c.order.service.OrderService - Order created successfully
```

### 2. OpenTelemetry Integration

OpenTelemetry Spring Boot Starter automatically injects `traceId` and `spanId` into SLF4J MDC:

- **Automatic Injection**: No manual code required
- **MDC Keys**: `traceId` and `spanId`
- **Format**: Hexadecimal strings (traceId: 32 chars, spanId: 16 chars)

### 3. Profile-Based Configuration

- **Production** (`prod`, `production`): JSON format for log aggregation
- **Development** (`dev`, `default`): Text format for human readability

### 4. Testing

**Test File**: `cuckoo-common/src/test/java/com/pingxin403/cuckoo/common/logging/LogbackConfigurationTest.java`

Tests verify:
- ✅ Logback configuration loads correctly
- ✅ MDC support for traceId and spanId
- ✅ Logs work with and without tracing context
- ✅ Different log levels work correctly

**Test Results**:
```bash
$ mvn test -Dtest=LogbackConfigurationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

## How It Works

### Request Flow with Tracing

```
1. Request enters Gateway
   ↓
2. OpenTelemetry creates Span
   ↓
3. Span context injected into MDC
   - MDC.put("traceId", "...")
   - MDC.put("spanId", "...")
   ↓
4. Application logs message
   ↓
5. Logback reads MDC values
   ↓
6. Log output includes traceId and spanId
```

### Correlation Workflow

```
User Issue Report
   ↓
Search logs in Kibana (by user ID, order ID, etc.)
   ↓
Extract traceId from log entry
   ↓
Search traceId in Jaeger UI
   ↓
View complete distributed trace
   ↓
Identify bottleneck or error
```

## Configuration Files

### Modified Files

1. **cuckoo-common/src/main/resources/logback-spring.xml**
   - Updated text format to include both traceId and spanId
   - Format: `[traceId=%X{traceId:-} spanId=%X{spanId:-}]`

### New Files

1. **cuckoo-common/src/test/java/com/pingxin403/cuckoo/common/logging/LogbackConfigurationTest.java**
   - Unit tests for Logback configuration
   - Tests MDC support and log output

2. **cuckoo-common/src/main/resources/LOGGING_CONFIGURATION.md**
   - Comprehensive documentation
   - Usage examples and troubleshooting guide

## Requirements Validation

### Requirement 5.4: Service logs include traceId and spanId

✅ **Satisfied**:
- JSON format includes `traceId` and `spanId` fields
- Text format includes `[traceId=... spanId=...]` in log pattern
- OpenTelemetry automatically populates MDC values

### Requirement 6.1: Logs use JSON format

✅ **Satisfied**:
- Production profile uses LogstashEncoder for JSON output
- Structured format suitable for ELK Stack ingestion

### Requirement 6.2: Logs include required fields

✅ **Satisfied**:
- timestamp ✓
- level ✓
- service ✓ (from spring.application.name)
- traceId ✓
- spanId ✓
- message ✓

## Usage Examples

### Example 1: Service Code

```java
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(OrderRequest request) {
        // traceId and spanId automatically included
        log.info("Creating order for user: {}", request.getUserId());
        
        Order order = orderRepository.save(new Order(request));
        
        log.info("Order created: {}", order.getId());
        return order;
    }
}
```

### Example 2: Log Output (Development)

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7] INFO  c.p.c.order.service.OrderService - Creating order for user: user123
2024-01-15 10:30:45.456 [http-nio-8080-exec-1] [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7] INFO  c.p.c.order.service.OrderService - Order created: order456
```

### Example 3: Log Output (Production)

```json
{"timestamp":"2024-01-15T10:30:45.123Z","level":"INFO","thread":"http-nio-8080-exec-1","logger":"com.pingxin403.cuckoo.order.service.OrderService","message":"Creating order for user: user123","traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","service":"cuckoo-order"}
{"timestamp":"2024-01-15T10:30:45.456Z","level":"INFO","thread":"http-nio-8080-exec-1","logger":"com.pingxin403.cuckoo.order.service.OrderService","message":"Order created: order456","traceId":"4bf92f3577b34da6a3ce929d0e0e4736","spanId":"00f067aa0ba902b7","service":"cuckoo-order"}
```

## Verification Steps

### 1. Run Tests

```bash
cd cuckoo-microservices/cuckoo-common
mvn test -Dtest=LogbackConfigurationTest
```

### 2. Start a Service

```bash
cd cuckoo-microservices/cuckoo-order
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. Make a Request

```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","items":[{"productId":1,"quantity":2}]}'
```

### 4. Check Logs

Look for log entries with traceId and spanId:

```
[traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7]
```

### 5. Verify in Jaeger

1. Copy the traceId from logs
2. Open Jaeger UI: http://localhost:16686
3. Search for the traceId
4. Verify the trace shows the complete request flow

## Benefits

### 1. Observability

- **Log-Trace Correlation**: Easily jump from logs to traces and vice versa
- **Distributed Context**: Track requests across multiple services
- **Root Cause Analysis**: Quickly identify the source of errors

### 2. Debugging

- **Request Tracking**: Follow a single request through the entire system
- **Performance Analysis**: Identify slow operations in the trace
- **Error Investigation**: See the complete context when errors occur

### 3. Operations

- **Incident Response**: Faster troubleshooting with correlated data
- **Monitoring**: Better visibility into system behavior
- **Compliance**: Audit trail with complete request context

## Troubleshooting

### Issue: TraceId not appearing in logs

**Cause**: OpenTelemetry not properly configured

**Solution**:
1. Verify OpenTelemetry Spring Boot Starter is in dependencies
2. Check application.properties for OpenTelemetry configuration
3. Ensure `otel.sdk.disabled=false`

### Issue: Logs not in JSON format

**Cause**: Wrong Spring profile

**Solution**:
1. Check `spring.profiles.active` property
2. Use `prod` or `production` profile for JSON output
3. Use `dev` or `default` profile for text output

### Issue: Empty traceId in logs

**Cause**: No active span in current thread

**Solution**:
1. Verify request is instrumented by OpenTelemetry
2. For async operations, use `@WithSpan` annotation
3. Check that OpenTelemetry agent is running

## Next Steps

1. **Task 5.1-5.4**: Deploy ELK Stack for log aggregation
2. **Task 6.1-6.5**: Set up Prometheus and Grafana for metrics
3. **Integration**: Configure Logstash to parse JSON logs and send to Elasticsearch
4. **Dashboards**: Create Kibana dashboards for log analysis

## References

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Logback Documentation](https://logback.qos.ch/documentation.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [SLF4J MDC](http://www.slf4j.org/manual.html#mdc)
- Task 4.2: OpenTelemetry SDK Integration
- Task 4.3: TraceId Propagation Implementation
