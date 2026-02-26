# User Service

User service manages user accounts, authentication, profiles, and user preferences in the Cuckoo e-commerce platform.

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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody CreateUserRequest request) {
        logRequest("创建用户", request.getUsername(), request.getEmail());
        UserDTO user = userService.createUser(request);
        logResponse("创建用户", user.getId());
        return created(user);  // Returns 201 Created
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        logRequest("查询用户", id);
        UserDTO user = userService.getUserById(id);
        logResponse("查询用户", user.getUsername());
        return ok(user);  // Returns 200 OK
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logRequest("删除用户", id);
        userService.deleteUser(id);
        logResponse("删除用户", id);
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
public class UserService {
    
    private final EventPublisherUtil eventPublisher;
    private final UserRepository userRepository;
    
    public UserDTO createUser(CreateUserRequest request) {
        // Create user
        User user = new User(request);
        userRepository.save(user);
        
        // Publish event - metadata auto-populated
        UserCreatedEvent event = new UserCreatedEvent(
            user.getId(),
            user.getUsername(),
            user.getEmail()
        );
        eventPublisher.publish(event);  // eventId, timestamp, traceId auto-set
        
        return userMapper.toDTO(user);
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
    name: user-service
    database-name: user_db

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/${spring.application.database-name}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}

# Service port
server:
  port: 8081
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
public class UserMapper implements DTOMapper<User, UserDTO> {

    @Override
    public UserDTO toDTO(User entity) {
        if (entity == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    @Override
    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User entity = new User();
        entity.setId(dto.getId());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setStatus(dto.getStatus());
        return entity;
    }
    
    // toDTOList() and toEntityList() inherited with default implementations
}

// Usage in service
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toDTOList(users);  // Batch conversion
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
    name = "notification-service",
    configuration = BaseFeignConfig.class,
    fallback = NotificationClientFallback.class
)
public interface NotificationClient {
    
    @PostMapping("/api/notifications/send")
    NotificationDTO sendNotification(@RequestBody SendNotificationRequest request);
}

// Service using Feign client
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final NotificationClient notificationClient;
    
    public void sendWelcomeEmail(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // TraceId automatically propagated in X-Trace-Id header
            notificationClient.sendNotification(
                new SendNotificationRequest(
                    NotificationType.EMAIL,
                    user.getEmail(),
                    "Welcome to Cuckoo!",
                    "Thank you for joining us."
                )
            );
            
        } catch (BusinessException e) {
            // Handle 4xx errors (e.g., invalid email)
            log.warn("发送欢迎邮件失败: {}", userId, e);
            throw e;
        } catch (SystemException e) {
            // Handle 5xx errors (e.g., service unavailable)
            log.error("通知服务异常: {}", userId, e);
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
mvn test -Dtest=UserServiceTest
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

Default port: `8081`

Override with:
```yaml
server:
  port: 8081
```

## API Documentation

API documentation is available at:
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Monitoring

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Application metrics
curl http://localhost:8081/actuator/metrics
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
