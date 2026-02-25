# Logging Quick Reference

## Quick Start

### 1. Add Logger to Your Class

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    
    public void myMethod() {
        log.info("This is a log message");
    }
}
```

### 2. Log Levels

```java
log.trace("Detailed trace information");  // Most verbose
log.debug("Debug information");           // Development details
log.info("Informational messages");       // Normal operations
log.warn("Warning messages");             // Potential issues
log.error("Error messages");              // Errors and exceptions
```

### 3. Parameterized Logging (Recommended)

```java
// ✅ Good - Efficient, no string concatenation if log level disabled
log.info("User {} created order {}", userId, orderId);

// ❌ Bad - Always creates string, even if logging disabled
log.info("User " + userId + " created order " + orderId);
```

### 4. Logging Exceptions

```java
try {
    // code that might throw exception
} catch (Exception e) {
    log.error("Failed to process order: {}", orderId, e);
}
```

## Tracing Integration

### Automatic TraceId and SpanId

OpenTelemetry automatically adds `traceId` and `spanId` to your logs:

```java
// No special code needed!
log.info("Processing order: {}", orderId);

// Output includes traceId and spanId automatically:
// [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7] INFO ... - Processing order: 12345
```

### Manual Span Creation (Advanced)

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Service
public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    
    @Autowired
    private Tracer tracer;
    
    public void complexOperation() {
        Span span = tracer.spanBuilder("complex-operation").startSpan();
        try (Scope scope = span.makeCurrent()) {
            log.info("Starting complex operation");
            // Your code here
            log.info("Complex operation completed");
        } finally {
            span.end();
        }
    }
}
```

## Log Formats

### Development (Text Format)

```
2024-01-15 10:30:45.123 [http-nio-8080-exec-1] [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7] INFO  c.p.c.order.service.OrderService - Order created: order123
```

### Production (JSON Format)

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.pingxin403.cuckoo.order.service.OrderService",
  "message": "Order created: order123",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "service": "cuckoo-order"
}
```

## Best Practices

### ✅ Do

1. **Use appropriate log levels**
   ```java
   log.debug("Detailed calculation: {} + {} = {}", a, b, result);  // Debug details
   log.info("Order created: {}", orderId);                          // Important events
   log.warn("Inventory low: {} units remaining", count);            // Warnings
   log.error("Failed to process payment", exception);               // Errors
   ```

2. **Use parameterized logging**
   ```java
   log.info("User {} logged in from {}", username, ipAddress);
   ```

3. **Log business events**
   ```java
   log.info("Order {} created by user {}", orderId, userId);
   log.info("Payment {} processed successfully", paymentId);
   ```

4. **Include context in error logs**
   ```java
   log.error("Failed to create order for user {}: {}", userId, orderId, exception);
   ```

### ❌ Don't

1. **Don't use string concatenation**
   ```java
   // ❌ Bad
   log.info("User " + username + " logged in");
   
   // ✅ Good
   log.info("User {} logged in", username);
   ```

2. **Don't log sensitive data**
   ```java
   // ❌ Bad - Logs password
   log.info("User login: username={}, password={}", username, password);
   
   // ✅ Good - No sensitive data
   log.info("User login: username={}", username);
   ```

3. **Don't log in loops without throttling**
   ```java
   // ❌ Bad - Can generate millions of log entries
   for (int i = 0; i < 1000000; i++) {
       log.debug("Processing item {}", i);
   }
   
   // ✅ Good - Log summary
   log.info("Processing {} items", items.size());
   // ... process items ...
   log.info("Processed {} items successfully", successCount);
   ```

4. **Don't swallow exceptions**
   ```java
   // ❌ Bad - Exception details lost
   try {
       // code
   } catch (Exception e) {
       log.error("Error occurred");
   }
   
   // ✅ Good - Exception details preserved
   try {
       // code
   } catch (Exception e) {
       log.error("Error processing order {}", orderId, e);
   }
   ```

## Correlation with Jaeger

### From Logs to Traces

1. Find a log entry in Kibana
2. Copy the `traceId` value
3. Open Jaeger UI: http://localhost:16686
4. Paste the `traceId` in the search box
5. View the complete distributed trace

### From Traces to Logs

1. Find a trace in Jaeger
2. Copy the `traceId` from the trace
3. Open Kibana
4. Search for `traceId: "4bf92f3577b34da6a3ce929d0e0e4736"`
5. View all logs for that request

## Configuration

### Switch Between Text and JSON

**Development (Text)**:
```yaml
spring:
  profiles:
    active: dev
```

**Production (JSON)**:
```yaml
spring:
  profiles:
    active: prod
```

### Adjust Log Levels

In `application.yml`:
```yaml
logging:
  level:
    com.pingxin403.cuckoo: DEBUG
    org.springframework: INFO
    org.hibernate: WARN
```

## Troubleshooting

### No TraceId in Logs

**Problem**: Logs don't show traceId

**Solutions**:
1. Check OpenTelemetry is enabled: `otel.sdk.disabled=false`
2. Verify OpenTelemetry Spring Boot Starter is in dependencies
3. Ensure request is instrumented (HTTP requests are auto-instrumented)

### Logs Not in JSON Format

**Problem**: Logs are in text format in production

**Solutions**:
1. Check Spring profile: `spring.profiles.active=prod`
2. Verify logback-spring.xml is in classpath
3. Check LogstashEncoder dependency is present

### Too Many Logs

**Problem**: Log volume is too high

**Solutions**:
1. Increase log level: `INFO` or `WARN` instead of `DEBUG`
2. Reduce third-party library logging
3. Use sampling for high-frequency operations

## Examples

### Service Layer

```java
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        
        try {
            Order order = orderRepository.save(new Order(request));
            log.info("Order created successfully: {}", order.getId());
            return order;
        } catch (Exception e) {
            log.error("Failed to create order for user: {}", request.getUserId(), e);
            throw e;
        }
    }
}
```

### Controller Layer

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        log.info("Received order creation request from user: {}", request.getUserId());
        
        Order order = orderService.createOrder(request);
        
        log.info("Order creation completed: {}", order.getId());
        return ResponseEntity.ok(order);
    }
}
```

### Event Consumer

```java
@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    
    @KafkaListener(topics = "order-events")
    public void handleOrderEvent(OrderCreatedEvent event) {
        log.info("Received order event: orderId={}, eventId={}", 
                event.getOrderId(), event.getEventId());
        
        try {
            processOrder(event);
            log.info("Order event processed successfully: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process order event: orderId={}, eventId={}", 
                    event.getOrderId(), event.getEventId(), e);
            throw e;
        }
    }
}
```

## Related Documentation

- [LOGGING_CONFIGURATION.md](../cuckoo-common/src/main/resources/LOGGING_CONFIGURATION.md) - Detailed configuration guide
- [TASK_4.4_LOGGING_TRACING_INTEGRATION.md](TASK_4.4_LOGGING_TRACING_INTEGRATION.md) - Implementation details
- [TRACING_QUICK_REFERENCE.md](TRACING_QUICK_REFERENCE.md) - Distributed tracing guide
