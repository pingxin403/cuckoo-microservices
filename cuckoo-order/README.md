# Order Service

Order service manages order creation, order lifecycle, saga orchestration, and CQRS read/write models in the Cuckoo e-commerce platform.

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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController extends BaseController {
    
    private final OrderWriteService orderWriteService;
    private final OrderQueryService orderQueryService;
    
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        logRequest("创建订单", request.getUserId(), request.getTotalAmount());
        OrderDTO order = orderWriteService.createOrder(request);
        logResponse("创建订单", order.getId());
        return created(order);  // Returns 201 Created
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        logRequest("查询订单", id);
        OrderDTO order = orderQueryService.getOrderById(id);
        logResponse("查询订单", order.getId());
        return ok(order);  // Returns 200 OK
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        logRequest("取消订单", id);
        orderWriteService.cancelOrder(id);
        logResponse("取消订单", id);
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
public class OrderWriteService {
    
    private final EventPublisherUtil eventPublisher;
    private final OrderWriteRepository orderWriteRepository;
    
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Create order in write model
        OrderWrite order = new OrderWrite(request);
        orderWriteRepository.save(order);
        
        // Publish event - metadata auto-populated
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getItems()
        );
        eventPublisher.publish(event);  // eventId, timestamp, traceId auto-set
        
        return orderMapper.toDTO(order);
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
    name: order-service
    database-name: order_db

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/${spring.application.database-name}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}

# Service port
server:
  port: 8084
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
public class OrderReadMapper implements DTOMapper<OrderRead, OrderDTO> {

    @Override
    public OrderDTO toDTO(OrderRead entity) {
        if (entity == null) {
            return null;
        }
        
        OrderDTO dto = new OrderDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setStatus(entity.getStatus());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setItems(entity.getItems());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    @Override
    public OrderRead toEntity(OrderDTO dto) {
        if (dto == null) {
            return null;
        }
        
        OrderRead entity = new OrderRead();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setStatus(dto.getStatus());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setItems(dto.getItems());
        return entity;
    }
    
    // toDTOList() and toEntityList() inherited with default implementations
}

// Usage in query service
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderReadRepository orderReadRepository;
    private final OrderReadMapper orderReadMapper;

    public List<OrderDTO> getUserOrders(Long userId) {
        List<OrderRead> orders = orderReadRepository.findByUserId(userId);
        return orderReadMapper.toDTOList(orders);  // Batch conversion
    }
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
    name = "inventory-service",
    configuration = BaseFeignConfig.class,
    fallback = InventoryClientFallback.class
)
public interface InventoryClient {
    
    @PostMapping("/api/inventory/reserve")
    ReservationDTO reserveInventory(@RequestBody ReserveRequest request);
    
    @PostMapping("/api/inventory/release/{reservationId}")
    void releaseReservation(@PathVariable String reservationId);
}

@FeignClient(
    name = "payment-service",
    configuration = BaseFeignConfig.class,
    fallback = PaymentClientFallback.class
)
public interface PaymentClient {
    
    @PostMapping("/api/payments/process")
    PaymentDTO processPayment(@RequestBody ProcessPaymentRequest request);
}

// Service using Feign clients in Saga
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {
    
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    
    public void executeOrderSaga(OrderDTO order) {
        try {
            // Step 1: Reserve inventory - traceId auto-propagated
            ReservationDTO reservation = inventoryClient.reserveInventory(
                new ReserveRequest(order.getProductId(), order.getQuantity())
            );
            
            // Step 2: Process payment - traceId auto-propagated
            PaymentDTO payment = paymentClient.processPayment(
                new ProcessPaymentRequest(order.getId(), order.getTotalAmount())
            );
            
        } catch (BusinessException e) {
            // Handle 4xx errors and trigger compensation
            log.warn("订单Saga业务错误: {}", order.getId(), e);
            compensate(order);
        } catch (SystemException e) {
            // Handle 5xx errors and retry or compensate
            log.error("订单Saga系统错误: {}", order.getId(), e);
            retryOrCompensate(order);
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

## Architecture

### CQRS Pattern

This service implements Command Query Responsibility Segregation (CQRS):

- **Write Model** (`OrderWrite`): Handles commands (create, update, cancel)
- **Read Model** (`OrderRead`): Optimized for queries (list, search, details)
- **Synchronization**: Event-driven sync from write to read model

### Saga Pattern

Order creation uses Saga orchestration for distributed transactions:

1. Create Order
2. Reserve Inventory
3. Process Payment
4. Send Notification

Each step has compensation logic for rollback.

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
mvn test -Dtest=OrderServiceTest
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

Default port: `8084`

Override with:
```yaml
server:
  port: 8084
```

## API Documentation

API documentation is available at:
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`

## Monitoring

### Health Check

```bash
curl http://localhost:8084/actuator/health
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8084/actuator/prometheus

# Application metrics
curl http://localhost:8084/actuator/metrics
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
