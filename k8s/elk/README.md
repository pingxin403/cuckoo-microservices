# ELK Stack Deployment for Cuckoo Microservices

## Overview

This directory contains Kubernetes manifests and scripts to deploy the ELK (Elasticsearch, Logstash, Kibana) stack for centralized log collection and analysis.

## Architecture

```
┌─────────────────┐
│  Microservices  │
│   (JSON Logs)   │
└────────┬────────┘
         │ TCP:5000
         ▼
┌─────────────────┐
│    Logstash     │ ← Parse JSON, Extract Fields
│   (2 replicas)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Elasticsearch   │ ← Store & Index Logs
│   (3 nodes)     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Kibana      │ ← Query & Visualize
│   (1 replica)   │
└─────────────────┘
```

## Components

### Elasticsearch (3-node cluster)
- **Purpose**: Store and index logs
- **Replicas**: 3 (for high availability)
- **Storage**: 10Gi per node (30Gi total)
- **Memory**: 2Gi per node
- **Port**: 9200 (HTTP), 9300 (Transport)

### Logstash (2 replicas)
- **Purpose**: Parse JSON logs and send to Elasticsearch
- **Replicas**: 2 (for load balancing)
- **Memory**: 1Gi per replica
- **Port**: 5000 (TCP input), 9600 (HTTP API)

### Kibana (1 replica)
- **Purpose**: Web UI for log visualization and querying
- **Replicas**: 1
- **Memory**: 1Gi
- **Port**: 5601 (HTTP)

## Prerequisites

- Kubernetes cluster (Minikube, Kind, or cloud provider)
- kubectl configured
- At least 8Gi of available memory
- At least 30Gi of available storage

## Quick Start

### 1. Deploy ELK Stack

```bash
cd cuckoo-microservices/k8s/elk
./deploy.sh
```

This will:
1. Create the `logging` namespace
2. Deploy Elasticsearch cluster (3 nodes)
3. Deploy Logstash (2 replicas)
4. Deploy Kibana (1 replica)
5. Wait for all pods to be ready

### 2. Verify Deployment

```bash
./verify.sh
```

This will check:
- All pods are running and ready
- Elasticsearch cluster health is green/yellow
- Logstash is accepting connections
- Kibana is accessible

### 3. Access Kibana

**Option A: Port Forward (Recommended for local development)**
```bash
kubectl port-forward -n logging svc/kibana 5601:5601
```
Then open: http://localhost:5601

**Option B: NodePort (Already configured)**
```bash
# Get the NodePort
kubectl get svc kibana -n logging

# Access via NodePort
# http://localhost:<NodePort>
```

## Configuration

### Logstash Pipeline

The Logstash pipeline is configured to:

1. **Input**: Accept JSON logs via TCP on port 5000
2. **Filter**: 
   - Parse JSON logs
   - Extract fields: timestamp, level, service, traceId, spanId, logger, thread, message
   - Add index date for daily indices
3. **Output**: Send to Elasticsearch with index pattern `cuckoo-logs-YYYY.MM.dd`

Configuration file: `logstash-deployment.yaml` (ConfigMap)

### Elasticsearch Cluster

- **Cluster name**: `logging-cluster`
- **Discovery**: Seed hosts for 3-node cluster
- **Security**: Disabled (for development; enable in production)
- **Storage**: 10Gi per node with PersistentVolumeClaims

Configuration file: `elasticsearch-statefulset.yaml`

### Kibana

- **Elasticsearch URL**: `http://elasticsearch.logging.svc.cluster.local:9200`
- **Server host**: `0.0.0.0` (accessible from outside the pod)

Configuration file: `kibana-deployment.yaml`

## Service Configuration

To send logs from microservices to Logstash, configure the Logback appender:

```xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash.logging.svc.cluster.local:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
    </encoder>
</appender>
```

See `../docs/LOGGING_CONFIGURATION.md` for complete configuration.

## Kibana Setup

### 1. Create Index Pattern

1. Open Kibana: http://localhost:5601
2. Go to **Management** → **Stack Management** → **Index Patterns**
3. Click **Create index pattern**
4. Enter pattern: `cuckoo-logs-*`
5. Select time field: `timestamp`
6. Click **Create index pattern**

### 2. Explore Logs

1. Go to **Discover**
2. Select the `cuckoo-logs-*` index pattern
3. Use the search bar to filter logs:
   - By service: `service: "cuckoo-order"`
   - By trace ID: `traceId: "4bf92f3577b34da6a3ce929d0e0e4736"`
   - By log level: `level: "ERROR"`
   - By time range: Use the time picker

### 3. Common Queries

**Find all errors:**
```
level: "ERROR"
```

**Find logs for a specific trace:**
```
traceId: "4bf92f3577b34da6a3ce929d0e0e4736"
```

**Find logs for a specific service:**
```
service: "cuckoo-order"
```

**Find logs with specific message:**
```
message: *"Order created"*
```

**Combine filters:**
```
service: "cuckoo-order" AND level: "ERROR"
```

## Log Retention

### Configure Index Lifecycle Management (ILM)

1. Go to **Management** → **Stack Management** → **Index Lifecycle Policies**
2. Create a new policy:
   - **Hot phase**: Keep for 7 days
   - **Warm phase**: Move to warm nodes after 7 days
   - **Delete phase**: Delete after 30 days

3. Apply policy to index template:
```bash
curl -X PUT "localhost:9200/_index_template/cuckoo-logs" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["cuckoo-logs-*"],
  "template": {
    "settings": {
      "index.lifecycle.name": "cuckoo-logs-policy",
      "index.lifecycle.rollover_alias": "cuckoo-logs"
    }
  }
}
'
```

## Monitoring

### Check Elasticsearch Cluster Health

```bash
kubectl port-forward -n logging svc/elasticsearch 9200:9200
curl http://localhost:9200/_cluster/health?pretty
```

### Check Logstash Status

```bash
kubectl port-forward -n logging svc/logstash 9600:9600
curl http://localhost:9600/?pretty
```

### View Logs

```bash
# Elasticsearch logs
kubectl logs -n logging elasticsearch-0

# Logstash logs
kubectl logs -n logging -l app=logstash

# Kibana logs
kubectl logs -n logging -l app=kibana
```

## Troubleshooting

### Elasticsearch pods not starting

**Symptom**: Pods stuck in `Pending` or `CrashLoopBackOff`

**Solutions**:
1. Check if `vm.max_map_count` is set correctly:
   ```bash
   # For Minikube
   minikube ssh
   sudo sysctl -w vm.max_map_count=262144
   
   # For Docker Desktop
   # Add to Docker Desktop settings
   ```

2. Check storage availability:
   ```bash
   kubectl get pvc -n logging
   ```

3. Check pod logs:
   ```bash
   kubectl logs -n logging elasticsearch-0
   ```

### Logstash not receiving logs

**Symptom**: No logs appearing in Kibana

**Solutions**:
1. Check Logstash is running:
   ```bash
   kubectl get pods -n logging -l app=logstash
   ```

2. Check Logstash logs:
   ```bash
   kubectl logs -n logging -l app=logstash
   ```

3. Test TCP connection:
   ```bash
   kubectl port-forward -n logging svc/logstash 5000:5000
   echo '{"message":"test"}' | nc localhost 5000
   ```

4. Verify service configuration sends to correct endpoint:
   ```
   logstash.logging.svc.cluster.local:5000
   ```

### Kibana not accessible

**Symptom**: Cannot access Kibana UI

**Solutions**:
1. Check Kibana pod is running:
   ```bash
   kubectl get pods -n logging -l app=kibana
   ```

2. Check Kibana logs:
   ```bash
   kubectl logs -n logging -l app=kibana
   ```

3. Port forward and access:
   ```bash
   kubectl port-forward -n logging svc/kibana 5601:5601
   ```

### No data in Kibana

**Symptom**: Index pattern created but no data visible

**Solutions**:
1. Check if indices exist:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

2. Check if logs are being sent:
   ```bash
   # Check service logs
   kubectl logs -n default <service-pod>
   ```

3. Verify time range in Kibana (top right corner)

4. Check index pattern matches indices:
   - Pattern: `cuckoo-logs-*`
   - Indices: `cuckoo-logs-2024.01.15`

## Cleanup

To remove the ELK stack:

```bash
./cleanup.sh
```

**Warning**: This will delete all logs and persistent volumes.

## Resource Requirements

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit | Storage |
|-----------|-------------|-----------|----------------|--------------|---------|
| Elasticsearch (per node) | 500m | 1000m | 2Gi | 2Gi | 10Gi |
| Logstash (per replica) | 500m | 1000m | 1Gi | 1Gi | - |
| Kibana | 500m | 1000m | 1Gi | 1Gi | - |
| **Total** | **3.5 CPU** | **7 CPU** | **9Gi** | **9Gi** | **30Gi** |

## Production Considerations

### Security

1. **Enable Elasticsearch Security**:
   - Set `xpack.security.enabled: true`
   - Configure authentication and authorization
   - Enable TLS for transport and HTTP

2. **Network Policies**:
   - Restrict access to Elasticsearch (only from Logstash and Kibana)
   - Restrict access to Logstash (only from microservices)
   - Restrict access to Kibana (only from authorized users)

3. **Secrets Management**:
   - Store credentials in Kubernetes Secrets
   - Use RBAC for access control

### High Availability

1. **Elasticsearch**:
   - Use at least 3 master-eligible nodes
   - Configure replica shards for indices
   - Use dedicated master nodes for large clusters

2. **Logstash**:
   - Scale replicas based on log volume
   - Use persistent queues for reliability

3. **Kibana**:
   - Scale to 2+ replicas for high availability
   - Use session affinity for load balancing

### Performance

1. **Elasticsearch**:
   - Increase heap size for large log volumes
   - Use SSD storage for better performance
   - Configure index refresh interval

2. **Logstash**:
   - Tune batch size and workers
   - Use multiple pipelines for different log types
   - Monitor pipeline performance

3. **Log Volume**:
   - Implement sampling for high-volume logs
   - Use log levels appropriately (DEBUG only in dev)
   - Aggregate similar log messages

## Integration with Jaeger

The ELK stack integrates with Jaeger for complete observability:

1. **Logs → Traces**: Copy `traceId` from logs and search in Jaeger
2. **Traces → Logs**: Copy `traceId` from Jaeger and search in Kibana

See `../docs/LOGGING_QUICK_REFERENCE.md` for correlation workflows.

## References

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review pod logs: `kubectl logs -n logging <pod-name>`
3. Check cluster events: `kubectl get events -n logging`
4. Refer to the official Elastic documentation
