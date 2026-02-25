# Logging Configuration with Distributed Tracing

## Overview

This document describes the logging configuration for the Cuckoo microservices system, which integrates with OpenTelemetry for distributed tracing.

## Configuration

The logging configuration is defined in `logback-spring.xml` and provides multiple output formats and destinations:

### 1. JSON Format (Production)

Used in production environments (`prod`, `production` profiles):

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

**Features:**
- Structured JSON format for easy parsing by log aggregation systems (ELK Stack)
- Includes `traceId` and `spanId` from OpenTelemetry MDC
- Includes service name from `spring.application.name`
- UTC timezone for consistency across services

### 2. Text Format (Development)

Used in development environments (`dev`, `default` profiles):

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7] INFO  c.p.c.order.service.OrderService - Order created successfully
```

**Features:**
- Human-readable format for local development
- Includes `traceId` and `spanId` in brackets for easy identification
- Color-coded log levels (if terminal supports it)

### 3. Logstash TCP Output (Production)

Used in production environments to send logs to ELK Stack:

**Configuration:**
- **Destination**: `logstash.logging.svc.cluster.local:5000`
- **Format**: JSON (same as console JSON format)
- **Async**: Uses AsyncAppender to avoid blocking business threads
- **Queue Size**: 512 messages
- **Never Block**: Drops logs if queue is full (business continuity over log completeness)
- **Connection Timeout**: 5 seconds
- **Write Timeout**: 5 seconds
- **Reconnection Delay**: 10 seconds

**Features:**
- Asynchronous sending to avoid performance impact
- Automatic reconnection on connection failure
- Graceful degradation (logs to console even if Logstash is unavailable)
- Non-blocking (never blocks business operations)

## OpenTelemetry Integration

### Automatic MDC Population

OpenTelemetry Spring Boot Starter automatically injects `traceId` and `spanId` into SLF4J MDC (Mapped Diagnostic Context) when:

1. A request enters the service (via HTTP, gRPC, etc.)
2. A span is active in the current thread context
3. Asynchronous operations are properly instrumented

### MDC Keys

The following MDC keys are automatically populated by OpenTelemetry:

- `traceId`: The trace ID in hexadecimal format (32 characters)
- `spanId`: The span ID in hexadecimal format (16 characters)

### Example Usage

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(OrderRequest request) {
        // traceId and spanId are automatically included in logs
        log.info("Creating order for user: {}", request.getUserId());
        
        // Business logic...
        
        log.info("Order created successfully: {}", order.getId());
        return order;
    }
}
```

## Correlation with Distributed Tracing

### Jaeger Integration

The `traceId` in logs can be used to correlate log entries with traces in Jaeger:

1. **From Logs to Traces**: Copy the `traceId` from a log entry and search for it in Jaeger UI
2. **From Traces to Logs**: Copy the `traceId` from a Jaeger trace and search for it in Kibana

### Example Workflow

1. User reports an issue with order creation
2. Search logs in Kibana for the user ID or order ID
3. Extract the `traceId` from the log entry
4. Open Jaeger UI and search for the `traceId`
5. View the complete distributed trace showing all service calls
6. Identify the bottleneck or error in the trace

## Log Levels

The configuration sets appropriate log levels for different components:

- **Application Code** (`com.pingxin403.cuckoo`): `DEBUG` in dev, `INFO` in prod
- **Spring Framework** (`org.springframework`): `INFO`
- **Hibernate** (`org.hibernate`): `WARN`
- **Kafka** (`org.apache.kafka`): `WARN`

## Environment-Specific Configuration

### Development Environment

```yaml
spring:
  profiles:
    active: dev
```

**Log Destinations:**
- Console (text format)

**Behavior:**
- Human-readable format for local debugging
- No Logstash integration (not needed for local development)

### Production Environment

```yaml
spring:
  profiles:
    active: prod
```

**Log Destinations:**
- Console (JSON format)
- Logstash TCP (JSON format, async)

**Behavior:**
- Structured JSON format for parsing
- Logs sent to both console and Logstash
- Async sending to avoid performance impact
- Graceful degradation if Logstash is unavailable

## Testing

Run the integration test to verify logging and tracing integration:

```bash
mvn test -Dtest=LogbackTracingIntegrationTest
```

## ELK Stack Integration

### Architecture

```
Microservice (Logback)
         ↓ TCP:5000 (async)
    Logstash (2 replicas)
         ↓
  Elasticsearch (3 nodes)
         ↓
      Kibana (UI)
```

### Configuration

The Logstash TCP appender is configured in `logback-spring.xml`:

```xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash.logging.svc.cluster.local:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <customFields>{"service":"${APP_NAME}"}</customFields>
        <timeZone>UTC</timeZone>
    </encoder>
    <connectionTimeout>5000</connectionTimeout>
    <writeTimeout>5000</writeTimeout>
    <reconnectionDelay>10000</reconnectionDelay>
    <queueSize>512</queueSize>
</appender>
```

### Async Wrapper

To avoid blocking business threads, the Logstash appender is wrapped in an AsyncAppender:

```xml
<appender name="ASYNC_LOGSTASH" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="LOGSTASH"/>
    <neverBlock>true</neverBlock>
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
</appender>
```

**Key Settings:**
- `neverBlock=true`: Never blocks business threads, drops logs if queue is full
- `queueSize=512`: Buffer size for async sending
- `discardingThreshold=0`: Don't discard any log levels when queue is filling up

### Failure Handling

**Scenario 1: Logstash is unavailable**
- Logs continue to be written to console
- AsyncAppender queues logs and attempts to reconnect
- After reconnection delay (10s), attempts to reconnect
- Business operations are never blocked

**Scenario 2: Network issues**
- Connection timeout: 5 seconds
- Write timeout: 5 seconds
- Automatic reconnection with 10-second delay
- Logs continue to console

**Scenario 3: Queue full**
- `neverBlock=true` ensures business threads are never blocked
- Oldest logs in queue are dropped
- Error logged to console
- Business operations continue normally

## Requirements Validation

This configuration satisfies the following requirements:

- **Requirement 5.4**: Service logs include traceId and spanId ✓
- **Requirement 6.1**: Logs use JSON format (in production) ✓
- **Requirement 6.2**: Logs include timestamp, level, service, traceId, spanId, and message ✓
- **Requirement 6.3**: Logs sent to Logstash via TCP ✓
- **Requirement 6.8**: Log output failures don't affect business (async, neverBlock) ✓

## Troubleshooting

### TraceId Not Appearing in Logs

**Possible Causes:**
1. OpenTelemetry is not properly configured
2. No active span in the current thread context
3. Async operations not properly instrumented

**Solutions:**
1. Verify OpenTelemetry Spring Boot Starter is in dependencies
2. Check that `otel.sdk.disabled=false` in application properties
3. Use `@WithSpan` annotation for async methods

### Logs Not in JSON Format

**Possible Causes:**
1. Wrong Spring profile is active
2. Logback configuration not loaded

**Solutions:**
1. Check `spring.profiles.active` property
2. Verify `logback-spring.xml` is in classpath

## References

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Logback Documentation](https://logback.qos.ch/documentation.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
