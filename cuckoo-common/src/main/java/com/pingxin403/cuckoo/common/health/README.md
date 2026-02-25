# Health Check and Warmup Components

This package provides health check and service warmup functionality for microservices.

## Components

### HealthCheckService

Provides health check functionality with two types of checks:

1. **Liveness Check** (`checkLiveness()`): Checks if the service is alive
   - Always returns UP unless the service is completely unresponsive
   - Used by Kubernetes to determine if a Pod should be restarted

2. **Readiness Check** (`checkReadiness()`): Checks if the service is ready to receive traffic
   - Checks warmup status
   - Checks database connectivity (if configured)
   - Checks Redis connectivity (if configured)
   - Used by Kubernetes to determine if traffic should be routed to the Pod

### WarmupService Interface

Services can implement this interface to define custom warmup logic:

```java
@Component
public class ProductWarmupService implements WarmupService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    public void performWarmup() throws Exception {
        // Load hot products into cache
        List<Product> hotProducts = productRepository.findTop100ByOrderBySalesDesc();
        for (Product product : hotProducts) {
            cacheManager.put("product:" + product.getId(), product, Duration.ofHours(1));
        }
        
        // Trigger JVM class loading
        productRepository.findById(1L);
    }
}
```

### HealthController

Exposes health check endpoints for Kubernetes probes:

- `GET /actuator/health/liveness` - Liveness probe endpoint
- `GET /actuator/health/readiness` - Readiness probe endpoint

## Usage

### 1. Basic Usage (Default Warmup)

The health check service is automatically configured and will perform default warmup:
- Database connection pool warmup
- Redis connection pool warmup

No additional configuration needed.

### 2. Custom Warmup Logic

Implement the `WarmupService` interface in your service:

```java
@Component
public class OrderWarmupService implements WarmupService {
    
    @Override
    public void performWarmup() throws Exception {
        // Your custom warmup logic
        // - Load hot data into cache
        // - Initialize connection pools
        // - Trigger JVM class loading
    }
}
```

### 3. Kubernetes Configuration

Configure probes in your Deployment YAML:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  template:
    spec:
      containers:
      - name: order-service
        image: order-service:latest
        
        # Liveness probe: check if service is alive
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Readiness probe: check if service is ready
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
```

## Warmup Process

The warmup process runs automatically when the service starts:

1. **Database Connection Pool**: Executes a simple query to initialize connections
2. **Redis Connection Pool**: Performs a test operation to initialize connections
3. **Custom Logic**: If `WarmupService` is implemented, executes custom warmup logic
4. **Mark as Ready**: Sets `isWarmedUp` flag to true

If warmup fails, the service will still start but log an error. This prevents the service from being unable to start due to temporary issues.

## Health Check Flow

```
Request → HealthController → HealthCheckService
                                    ↓
                            Check Warmup Status
                                    ↓
                            Check Database (if configured)
                                    ↓
                            Check Redis (if configured)
                                    ↓
                            Return UP or DOWN
```

## Best Practices

1. **Keep Liveness Check Simple**: The liveness check should be lightweight and always return UP unless the service is completely broken

2. **Comprehensive Readiness Check**: The readiness check should verify all dependencies are available

3. **Fast Warmup**: Keep warmup logic fast (< 20 seconds) to avoid long startup times

4. **Graceful Failure**: If warmup fails, log the error but allow the service to start

5. **Monitor Warmup**: Add metrics to track warmup duration and success rate

## Requirements Validation

This implementation validates the following requirements:

- **Requirement 8.2**: Initialize database connection pool during warmup
- **Requirement 8.3**: Initialize Redis connection pool during warmup
- **Requirement 8.4**: Load hot data into cache during warmup
- **Requirement 8.5**: Trigger JVM class loading during warmup
- **Requirement 8.8**: Readiness probe checks warmup completion and dependency connectivity

## Example: Product Service Warmup

```java
@Component
public class ProductWarmupService implements WarmupService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    @Override
    public void performWarmup() throws Exception {
        log.info("Starting product service warmup...");
        
        // 1. Load top 100 hot products into cache
        List<Product> hotProducts = productRepository.findTop100ByOrderBySalesDesc();
        for (Product product : hotProducts) {
            String cacheKey = "product:" + product.getId();
            cacheManager.put(cacheKey, product, Duration.ofHours(1));
        }
        log.info("Loaded {} hot products into cache", hotProducts.size());
        
        // 2. Trigger JVM class loading by executing core business logic
        try {
            productRepository.findById(1L);
        } catch (Exception e) {
            // Ignore errors, just trigger class loading
        }
        
        log.info("Product service warmup completed");
    }
}
```

## Troubleshooting

### Service Not Becoming Ready

Check the logs for warmup errors:
```
grep "warmup failed" /var/log/service.log
```

### Readiness Check Failing

Check which dependency is failing:
```
curl http://localhost:8080/actuator/health/readiness
```

### Slow Startup

Reduce warmup data or optimize warmup logic to complete faster.
