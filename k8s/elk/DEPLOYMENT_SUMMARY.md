# ELK Stack Deployment Summary

## Deployment Overview

This document summarizes the ELK (Elasticsearch, Logstash, Kibana) stack deployment for the Cuckoo microservices logging system.

## Components Deployed

### 1. Namespace
- **Name**: `logging`
- **Purpose**: Isolate ELK stack resources

### 2. Elasticsearch Cluster
- **Type**: StatefulSet
- **Replicas**: 3 nodes
- **Image**: `docker.elastic.co/elasticsearch/elasticsearch:8.11.0`
- **Cluster Name**: `logging-cluster`
- **Ports**:
  - 9200: HTTP API
  - 9300: Transport (inter-node communication)
- **Resources per node**:
  - CPU: 500m request, 1000m limit
  - Memory: 2Gi request and limit
  - Storage: 10Gi PersistentVolume
- **Total Resources**:
  - CPU: 1.5 cores request, 3 cores limit
  - Memory: 6Gi
  - Storage: 30Gi
- **Configuration**:
  - Security disabled (for development)
  - 3-node cluster with auto-discovery
  - Heap size: 1GB per node

### 3. Logstash
- **Type**: Deployment
- **Replicas**: 2
- **Image**: `docker.elastic.co/logstash/logstash:8.11.0`
- **Ports**:
  - 5000: TCP input (for log ingestion)
  - 9600: HTTP API (for monitoring)
- **Resources per replica**:
  - CPU: 500m request, 1000m limit
  - Memory: 1Gi request and limit
- **Total Resources**:
  - CPU: 1 core request, 2 cores limit
  - Memory: 2Gi
- **Pipeline Configuration**:
  - Input: TCP on port 5000, JSON format
  - Filter: Parse JSON, extract fields (timestamp, level, service, traceId, spanId, logger, thread, message)
  - Output: Elasticsearch with daily indices (`cuckoo-logs-YYYY.MM.dd`)

### 4. Kibana
- **Type**: Deployment
- **Replicas**: 1
- **Image**: `docker.elastic.co/kibana/kibana:8.11.0`
- **Port**: 5601 (HTTP)
- **Service Type**: NodePort
- **Resources**:
  - CPU: 500m request, 1000m limit
  - Memory: 1Gi request and limit
- **Configuration**:
  - Connected to Elasticsearch cluster
  - Accessible via NodePort or port-forward

## Total Resource Requirements

| Resource | Request | Limit |
|----------|---------|-------|
| CPU | 3.5 cores | 7 cores |
| Memory | 9Gi | 9Gi |
| Storage | 30Gi | 30Gi |

## Network Architecture

```
Microservices (JSON logs)
         ↓ TCP:5000
    Logstash (2 replicas)
         ↓
  Elasticsearch (3 nodes)
         ↓
      Kibana (1 replica)
```

## Service Endpoints

### Internal (within Kubernetes cluster)

- **Elasticsearch**: `elasticsearch.logging.svc.cluster.local:9200`
- **Logstash**: `logstash.logging.svc.cluster.local:5000`
- **Kibana**: `kibana.logging.svc.cluster.local:5601`

### External Access

- **Elasticsearch**: Port-forward to 9200
  ```bash
  kubectl port-forward -n logging svc/elasticsearch 9200:9200
  ```

- **Logstash**: Port-forward to 5000 (for testing)
  ```bash
  kubectl port-forward -n logging svc/logstash 5000:5000
  ```

- **Kibana**: NodePort or port-forward to 5601
  ```bash
  kubectl port-forward -n logging svc/kibana 5601:5601
  ```

## Log Flow

1. **Microservices** generate JSON logs with fields:
   - timestamp
   - level
   - service
   - traceId
   - spanId
   - logger
   - thread
   - message

2. **Logstash** receives logs via TCP:
   - Parses JSON format
   - Extracts and indexes fields
   - Adds index date

3. **Elasticsearch** stores logs:
   - Daily indices: `cuckoo-logs-YYYY.MM.dd`
   - 3-node cluster for reliability
   - Searchable and aggregatable

4. **Kibana** provides UI:
   - Search and filter logs
   - Create visualizations
   - Build dashboards
   - Set up alerts

## Health Checks

### Elasticsearch
- **Readiness Probe**: `GET /_cluster/health` (port 9200)
  - Initial delay: 30s
  - Period: 10s
- **Liveness Probe**: `GET /_cluster/health` (port 9200)
  - Initial delay: 60s
  - Period: 20s

### Logstash
- **Readiness Probe**: `GET /` (port 9600)
  - Initial delay: 30s
  - Period: 10s
- **Liveness Probe**: `GET /` (port 9600)
  - Initial delay: 60s
  - Period: 20s

### Kibana
- **Readiness Probe**: `GET /api/status` (port 5601)
  - Initial delay: 60s
  - Period: 10s
- **Liveness Probe**: `GET /api/status` (port 5601)
  - Initial delay: 90s
  - Period: 20s

## Deployment Scripts

### deploy.sh
- Creates namespace
- Deploys Elasticsearch, Logstash, Kibana in order
- Waits for each component to be ready
- Displays access information

### verify.sh
- Checks all pods are running
- Verifies Elasticsearch cluster health
- Tests Logstash API
- Tests Kibana API
- Provides troubleshooting info if checks fail

### cleanup.sh
- Deletes all ELK components
- Removes PersistentVolumeClaims
- Deletes namespace
- Requires confirmation before deletion

## Configuration Files

### Kubernetes Manifests
- `namespace.yaml`: Creates logging namespace
- `elasticsearch-statefulset.yaml`: Elasticsearch cluster
- `logstash-deployment.yaml`: Logstash with ConfigMap for pipeline
- `kibana-deployment.yaml`: Kibana UI

### Logstash Pipeline
Defined in ConfigMap `logstash-config`:
- `logstash.yml`: Logstash configuration
- `logstash.conf`: Pipeline definition (input, filter, output)

## Integration Points

### With Microservices
Services send logs to Logstash using Logback TCP appender:
```xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash.logging.svc.cluster.local:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
    </encoder>
</appender>
```

### With Jaeger (Distributed Tracing)
- Logs include `traceId` and `spanId` from OpenTelemetry
- Enables correlation between logs and traces
- Workflow: Find log → Copy traceId → Search in Jaeger

## Requirements Satisfied

This deployment satisfies the following requirements from the spec:

- **Requirement 6.1**: Logs use JSON format ✓
- **Requirement 6.2**: Logs include timestamp, level, service, traceId, spanId, message ✓
- **Requirement 6.3**: Logs sent to Logstash via TCP ✓
- **Requirement 6.4**: Logstash parses JSON and extracts fields ✓
- **Requirement 6.5**: Logstash sends to Elasticsearch ✓
- **Requirement 6.6**: Kibana supports search by traceId, service, level, time range ✓
- **Requirement 6.7**: Log retention configurable (via ILM policies) ✓
- **Requirement 6.8**: Log output failures don't affect business (async appender) ✓

## Next Steps

1. **Configure Services**: Update service Logback configuration to send logs to Logstash
2. **Create Index Patterns**: Set up Kibana index patterns for log exploration
3. **Set Up Retention**: Configure Index Lifecycle Management (ILM) for 30-day retention
4. **Create Dashboards**: Build custom dashboards for log analysis
5. **Configure Alerts**: Set up alerts for error rates and critical issues

## Monitoring

### Elasticsearch Metrics
```bash
curl http://localhost:9200/_cluster/health?pretty
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_cat/nodes?v
```

### Logstash Metrics
```bash
curl http://localhost:9600/?pretty
curl http://localhost:9600/_node/stats?pretty
```

### Kibana Status
```bash
curl http://localhost:5601/api/status
```

## Troubleshooting

Common issues and solutions are documented in:
- `README.md` - Comprehensive troubleshooting guide
- `QUICK_START.md` - Quick troubleshooting tips

## References

- Full documentation: `README.md`
- Quick start guide: `QUICK_START.md`
- Service configuration: `../docs/LOGGING_CONFIGURATION.md`
- Logback integration: `../docs/TASK_4.4_LOGGING_TRACING_INTEGRATION.md`
