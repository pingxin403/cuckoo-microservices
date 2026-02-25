# Microservices Kubernetes Deployments

This directory contains Kubernetes deployment configurations for all microservices with health checks and graceful shutdown support.

## Features

### Health Checks

Each service deployment includes:

1. **Liveness Probe** (`/actuator/health/liveness`)
   - Checks if the service is alive
   - If fails 3 times consecutively, Kubernetes restarts the Pod
   - Initial delay: 30 seconds
   - Check interval: 10 seconds

2. **Readiness Probe** (`/actuator/health/readiness`)
   - Checks if the service is ready to receive traffic
   - Verifies warmup completion, database, and Redis connectivity
   - If fails, Kubernetes stops routing traffic to the Pod
   - Initial delay: 20 seconds
   - Check interval: 5 seconds

### Graceful Shutdown

Each service includes graceful shutdown configuration:

1. **PreStop Hook**: Waits 30 seconds before stopping to allow load balancer updates
2. **Termination Grace Period**: 90 seconds total (30s preStop + 60s application shutdown)
3. **Rolling Update Strategy**: Ensures at least 2 Pods remain available during updates
4. **Resource Cleanup**: Automatically closes Kafka, Redis, and database connections

### Resource Management

Each service has resource requests and limits:
- Memory: 512Mi request, 1Gi limit
- CPU: 250m request, 1000m limit

## Deployment

### Deploy All Services

```bash
kubectl apply -f k8s/services/
```

### Deploy Individual Service

```bash
kubectl apply -f k8s/services/order-service-deployment.yaml
kubectl apply -f k8s/services/product-service-deployment.yaml
```

## Verification

### Check Pod Status

```bash
# Check all pods
kubectl get pods -l app=order-service

# Check pod details
kubectl describe pod <pod-name>
```

### Check Health Endpoints

```bash
# Port forward to a pod
kubectl port-forward <pod-name> 8080:8080

# Test liveness endpoint
curl http://localhost:8080/actuator/health/liveness

# Test readiness endpoint
curl http://localhost:8080/actuator/health/readiness
```

### Monitor Pod Events

```bash
# Watch pod events
kubectl get events --sort-by='.lastTimestamp' | grep <pod-name>

# Check pod logs
kubectl logs -f <pod-name>
```

## Health Check Behavior

### Startup Sequence

1. **Container Starts**: Service begins initialization
2. **Warmup Executes**: 
   - Database connection pool initialized
   - Redis connection pool initialized
   - Hot data loaded into cache
   - JVM classes loaded
3. **Readiness Check Passes**: Service marked as ready
4. **Traffic Routing Begins**: Kubernetes routes traffic to the Pod

### Failure Scenarios

#### Liveness Probe Failure
- **Cause**: Service is completely unresponsive
- **Action**: Kubernetes restarts the Pod
- **Recovery**: New Pod starts and goes through warmup

#### Readiness Probe Failure
- **Cause**: Database or Redis unavailable, or warmup not complete
- **Action**: Kubernetes stops routing traffic to the Pod
- **Recovery**: Once dependencies recover, readiness check passes and traffic resumes

### Graceful Shutdown Sequence

1. **Termination Signal Received**: Kubernetes sends SIGTERM
2. **PreStop Hook Executes**: Waits 30 seconds for load balancer update
3. **Service Deregisters**: Service removes itself from Nacos
4. **Stop New Requests**: Service stops accepting new HTTP connections
5. **In-Flight Requests Complete**: Service waits up to 60 seconds for existing requests
6. **Resource Cleanup**: Closes Kafka producer, Redis connections, database connections
7. **Pod Terminates**: Pod is removed after all cleanup completes

## Monitoring

### Check Service Health

```bash
# Check service endpoints
kubectl get endpoints order-service

# Check service status
kubectl get svc order-service
```

### View Metrics

```bash
# Access Prometheus metrics
kubectl port-forward <pod-name> 8080:8080
curl http://localhost:8080/actuator/prometheus
```

### View Logs

```bash
# Tail logs
kubectl logs -f <pod-name>

# View logs from all replicas
kubectl logs -l app=order-service --tail=100
```

## Troubleshooting

### Pod Not Becoming Ready

**Symptoms**: Pod stuck in `0/1 Ready` state

**Diagnosis**:
```bash
# Check readiness probe status
kubectl describe pod <pod-name> | grep -A 10 "Readiness"

# Check logs for warmup errors
kubectl logs <pod-name> | grep -i "warmup\|health"
```

**Common Causes**:
- Database connection failed
- Redis connection failed
- Warmup timeout
- Application startup error

**Solutions**:
- Verify database and Redis are running
- Check network connectivity
- Increase `initialDelaySeconds` if warmup takes longer
- Check application logs for errors

### Pod Restarting Frequently

**Symptoms**: Pod shows high restart count

**Diagnosis**:
```bash
# Check restart count
kubectl get pods -l app=order-service

# Check events
kubectl describe pod <pod-name> | grep -A 20 "Events"

# Check previous logs
kubectl logs <pod-name> --previous
```

**Common Causes**:
- Liveness probe failing
- Out of memory (OOMKilled)
- Application crash

**Solutions**:
- Fix application errors causing liveness failures
- Increase memory limits if OOMKilled
- Review application logs for crash causes

### Traffic Not Routing to Pod

**Symptoms**: Service exists but no traffic reaches Pod

**Diagnosis**:
```bash
# Check endpoints
kubectl get endpoints order-service

# Check readiness status
kubectl get pods -l app=order-service -o wide
```

**Common Causes**:
- Readiness probe failing
- Service selector mismatch
- Network policy blocking traffic

**Solutions**:
- Fix readiness probe failures
- Verify service selector matches pod labels
- Check network policies

### Slow Rolling Updates

**Symptoms**: Deployment takes long time to complete

**Diagnosis**:
```bash
# Check rollout status
kubectl rollout status deployment/order-service

# Check pod status during rollout
kubectl get pods -l app=order-service -w
```

**Common Causes**:
- Long warmup time
- Slow readiness probe
- Insufficient resources

**Solutions**:
- Optimize warmup logic
- Adjust readiness probe timing
- Increase cluster resources

## Configuration Reference

### Probe Configuration

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30  # Wait before first check
  periodSeconds: 10        # Check interval
  timeoutSeconds: 3        # Request timeout
  successThreshold: 1      # Success count to mark healthy
  failureThreshold: 3      # Failure count to restart

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 5
  timeoutSeconds: 3
  successThreshold: 1
  failureThreshold: 3
```

### Graceful Shutdown Configuration

```yaml
lifecycle:
  preStop:
    exec:
      # Wait 30 seconds for load balancer to update
      command: ["/bin/sh", "-c", "sleep 30"]

# Total grace period: 90 seconds (30s preStop + 60s app shutdown)
terminationGracePeriodSeconds: 90
```

Application configuration (application-common.yml):

```yaml
server:
  shutdown: graceful  # Enable graceful shutdown

spring:
  lifecycle:
    timeout-per-shutdown-phase: 60s  # Max time per shutdown phase

graceful:
  shutdown:
    grace-period: 30  # Wait for load balancer update (seconds)
    max-wait: 60      # Max wait for in-flight requests (seconds)
```

### Rolling Update Configuration

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1          # Max new pods during update
    maxUnavailable: 0    # Min pods that must remain available
```

## Best Practices

1. **Always Configure Both Probes**: Use both liveness and readiness probes
2. **Set Appropriate Delays**: Allow enough time for warmup before checks
3. **Monitor Probe Failures**: Set up alerts for probe failures
4. **Test Graceful Shutdown**: Verify no requests are dropped during updates
5. **Use Resource Limits**: Prevent pods from consuming excessive resources
6. **Maintain Minimum Replicas**: Keep at least 2 replicas for high availability

## Requirements Validation

This implementation validates:

- **Requirement 8.2**: Database connection pool warmup
- **Requirement 8.3**: Redis connection pool warmup
- **Requirement 8.4**: Hot data loading
- **Requirement 8.5**: JVM class loading
- **Requirement 8.8**: Kubernetes probes configuration
- **Requirement 9.1-9.10**: Graceful shutdown implementation
