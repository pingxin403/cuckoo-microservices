# ELK Stack Quick Start Guide

## 5-Minute Setup

### 1. Deploy ELK Stack

```bash
cd cuckoo-microservices/k8s/elk
./deploy.sh
```

Wait for all pods to be ready (2-3 minutes).

### 2. Verify Deployment

```bash
./verify.sh
```

Expected output:
```
✓ Namespace 'logging' exists
✓ elasticsearch: 3/3 pods ready
✓ logstash: 2/2 pods ready
✓ kibana: 1/1 pods ready
✓ Elasticsearch cluster health: green
✓ Logstash status: green
✓ Kibana status: green
```

### 3. Access Kibana

```bash
kubectl port-forward -n logging svc/kibana 5601:5601
```

Open: http://localhost:5601

### 4. Create Index Pattern

1. Go to **Management** → **Stack Management** → **Index Patterns**
2. Click **Create index pattern**
3. Enter: `cuckoo-logs-*`
4. Select time field: `timestamp`
5. Click **Create**

### 5. View Logs

1. Go to **Discover**
2. Select `cuckoo-logs-*` index pattern
3. Start exploring logs!

## Common Queries

### Find errors
```
level: "ERROR"
```

### Find logs by trace ID
```
traceId: "4bf92f3577b34da6a3ce929d0e0e4736"
```

### Find logs by service
```
service: "cuckoo-order"
```

### Combine filters
```
service: "cuckoo-order" AND level: "ERROR"
```

## Troubleshooting

### Pods not starting?
```bash
# Check pod status
kubectl get pods -n logging

# Check pod logs
kubectl logs -n logging <pod-name>

# Check events
kubectl get events -n logging
```

### No logs in Kibana?
1. Check time range (top right corner)
2. Verify services are sending logs to Logstash
3. Check Logstash logs: `kubectl logs -n logging -l app=logstash`

### Elasticsearch health yellow?
This is normal for a 3-node cluster with default settings. Yellow means:
- All primary shards are allocated
- Some replica shards are not allocated (acceptable for development)

## Next Steps

1. Configure services to send logs to Logstash (see README.md)
2. Set up log retention policies
3. Create custom dashboards
4. Configure alerts

## Cleanup

```bash
./cleanup.sh
```

**Warning**: This deletes all logs!

## Resources

- Full documentation: `README.md`
- Service configuration: `../docs/LOGGING_CONFIGURATION.md`
- Troubleshooting: See README.md
