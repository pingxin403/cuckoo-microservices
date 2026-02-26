# Payment Service

Payment service handles payment processing, transaction management, and payment gateway integration in the Cuckoo e-commerce platform.

## Common Components Usage

This service uses common components from `cuckoo-common` module to reduce code duplication and maintain consistency across services.

### 1. BaseController

All controllers extend `BaseController` for unified response patterns and logging.

**Benefits:**
- Standardized HTTP response methods
- Automatic request/response logging with traceId
- Reduced controller boilerplate by 30%

**Usage Example:**

```java
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController extends BaseController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentDTO> processPayment(@RequestBody ProcessPaymentRequest request) {
        logRequest("处理支付", request.getOrderId(), request.getAmount());
        PaymentDTO payment = paymentService.processPayment(request);
        logResponse("处理支付", payment.getPaymentId());
        return created(payment);  // Returns 201 Created
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable String paymentId) {
        logRequest("查询支付", paymentId);
        PaymentDTO payment = paymentService.getPaymentById(paymentId);
        logResponse("查询支付", payment.getStatus());
        return ok(payment);  // Returns 200 OK
    }
    
    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<Void> refundPayment(@PathVariable String paymentId) {
        logRequest("退款", paymentId);
        paymentService.refundPayment(paymentId);
        logResponse("退款", paymentId);
        return noContent();  // Returns 204 No Content
    }
}
```

**Available Methods:**
- `created(T body)` - Returns HTTP 201 with body
- `ok(T body)` - Returns HTTP 200 with body
- `noContent()` - Returns HTTP 204 with no body
- `logRequest(String operation, Object... params)` - Logs request with traceId
- `logResponse(String operation, Object result)` - Logs response with traceId

### 2. EventPublisherUtil

Use `EventPublisherUtil` instead of direct `KafkaEventPublisher` for automatic metadata population.

**Benefits:**
- Auto-generates eventId (UUID)
- Auto-populates timestamp
- Auto-propagates traceId from MDC
- Unified event logging
- Reduced event publishing code by 50%

**Usage Example:**

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final EventPublisherUtil eventPublisher;
    private final PaymentRepository paymentRepository;
    
    public PaymentDTO processPayment(ProcessPaymentRequest request) {
        // Process payment
        Payment payment = new Payment(request);
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        
        // Call payment gateway
        PaymentResult result = paymentGateway.charge(request);
        payment.setStatus(result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        paymentRepository.save(payment);
        
        // Publish event - metadata auto-populated
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getStatus()
        );
        eventPublisher.publish(event);  // eventId, timestamp, traceId auto-set
        
        return paymentMapper.toDTO(payment);
    }
}
```

**Available Methods:**
- `publish(T event)` - Publishes event with auto-populated metadata
- `publish(String topic, T event)` - Publishes to specific topic
- `publish(String topic, String key, T event)` - Publishes with custom key

**Auto-populated Fields:**
- `eventId` - UUID if not present
- `timestamp` - Current time in milliseconds if not present
- `traceId` - From MDC context if not present

### 3. Common Configuration

Import common configuration to reduce configuration duplication by 60%.

**Configuration File:** `application.yml`

```yaml
# Import common configuration
spring:
  config:
    import: classpath:application-common.yml

  # Service-specific overrides
  application:
    name: payment-service
    database-name: payment_db

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/${spring.application.database-name}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}

# Service port
server:
  port: 8085
```

**Common Configuration Includes:**
- Actuator endpoints (health, info, prometheus, metrics)
- OpenTelemetry tracing configuration
- JPA and Hibernate settings
- Nacos discovery and config
- Redis connection settings
- Sentinel circuit breaker
- Feign client defaults
- Logging patterns with traceId
- Graceful shutdown settings

**Override Strategy:**
- Import `application-common.yml` first
- Override only service-specific properties
- Use environment variables for deployment-specific values

### 4. DTOMapper Interface

Implement `DTOMapper<E, D>` for standardized entity-DTO conversions.

**Benefits:**
- Consistent method names across services
- Null-safe conversions
- Built-in batch conversion methods
- Flexible implementation strategy

**Usage Example:**

```java
@Component
public class PaymentMapper implements DTOMapper<Payment, PaymentDTO> {

    @Override
    public PaymentDTO toDTO(Payment entity) {
        if (entity == null) {
            return null;
        }
        
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(entity.getPaymentId());
        dto.setOrderId(entity.getOrderId());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    @Override
    public Payment toEntity(PaymentDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Payment entity = new Payment();
        entity.setPaymentId(dto.getPaymentId());
        entity.setOrderId(dto.getOrderId());
        entity.setAmount(dto.getAmount());
        entity.setStatus(dto.getStatus());
        entity.setPaymentMethod(dto.getPaymentMethod());
        return entity;
    }
    
    // toDTOList() and toEntityList() inherited with default implementations
}
```

**Interface Methods:**
- `toDTO(E entity)` - Converts entity to DTO (null-safe)
- `toEntity(D dto)` - Converts DTO to entity (null-safe)
- `toDTOList(List<E> entities)` - Batch entity to DTO conversion
- `toEntityList(List<D> dtos)` - Batch DTO to entity conversion

### 5. BaseFeignConfig

Use `BaseFeignConfig` for all Feign clients to ensure consistent error handling and tracing.

**Benefits:**
- Unified error decoding (4xx → BusinessException, 5xx → SystemException)
- Automatic traceId propagation
- Request/response logging
- Configured timeouts (connection: 5s, read: 10s)

**Usage Example:**

```java
@FeignClient(
    name = "order-service",
    configuration = BaseFeignConfig.class,
    fallback = OrderClientFallback.class
)
public interface OrderClient {
    
    @GetMapping("/api/orders/{id}")
    OrderDTO getOrder(@PathVariable Long id);
    
    @PutMapping("/api/orders/{id}/status")
    void updateOrderStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request);
}

// Service using Feign client
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final OrderClient orderClient;
    
    public void updateOrderAfterPayment(String paymentId, PaymentStatus status) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            
            // TraceId automatically propagated in X-Trace-Id header
            orderClient.updateOrderStatus(
                payment.getOrderId(),
                new UpdateStatusRequest(status == PaymentStatus.SUCCESS ? "PAID" : "PAYMENT_FAILED")
            );
            
        } catch (BusinessException e) {
            // Handle 4xx errors (e.g., order not found)
            log.warn("订单更新失败: {}", paymentId, e);
            throw e;
        } catch (SystemException e) {
            // Handle 5xx errors (e.g., service unavailable)
            log.error("订单服务异常: {}", paymentId, e);
            throw e;
        }
    }
}
```

**Features:**
- **Error Decoder**: Converts HTTP errors to typed exceptions
  - 4xx → `BusinessException` (client errors)
  - 5xx → `SystemException` (server errors)
- **Request Interceptor**: Adds `X-Trace-Id` header from MDC
- **Logging**: Logs all requests and errors with traceId
- **Timeouts**: Connection timeout 5000ms, read timeout 10000ms

## Development

### Running Locally

```bash
# Start the service
mvn spring-boot:run

# Or use the automation script to start all services
cd ../..
./scripts/start-all.sh
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentServiceTest
```

### Building

```bash
# Build without tests
mvn clean package -DskipTests

# Build with tests
mvn clean package
```

## Configuration

### Environment Variables

- `MYSQL_HOST` - MySQL host (default: localhost)
- `MYSQL_USER` - MySQL username (default: root)
- `MYSQL_PASSWORD` - MySQL password (default: root)
- `REDIS_HOST` - Redis host (default: localhost)
- `REDIS_PORT` - Redis port (default: 6379)
- `NACOS_SERVER` - Nacos server address (default: localhost:8848)
- `JAEGER_ENDPOINT` - Jaeger collector endpoint
- `TRACING_SAMPLE_RATE` - Tracing sample rate (default: 1.0)
- `DEPLOYMENT_ENV` - Deployment environment (default: development)

### Service Port

Default port: `8085`

Override with:
```yaml
server:
  port: 8085
```

## API Documentation

API documentation is available at:
- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

## Monitoring

### Health Check

```bash
curl http://localhost:8085/actuator/health
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8085/actuator/prometheus

# Application metrics
curl http://localhost:8085/actuator/metrics
```

### Tracing

Distributed traces are sent to Jaeger. View traces at:
- Jaeger UI: `http://localhost:16686`

## Related Documentation

- [Simplification Guide](../../.kiro/specs/microservice-simplification/SIMPLIFICATION_GUIDE.md) - Migration guide for common components
- [Common Configuration](../cuckoo-common/src/main/resources/application-common.yml) - Shared configuration template
- [BaseController](../cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/controller/BaseController.java) - Controller base class
- [EventPublisherUtil](../cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/event/EventPublisherUtil.java) - Event publishing utility
- [DTOMapper](../cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/mapper/DTOMapper.java) - DTO mapping interface
- [BaseFeignConfig](../cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/feign/BaseFeignConfig.java) - Feign configuration
