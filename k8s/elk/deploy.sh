#!/bin/bash

# ELK Stack Deployment Script
# This script deploys Elasticsearch, Logstash, and Kibana to Kubernetes

set -e

echo "========================================="
echo "Deploying ELK Stack to Kubernetes"
echo "========================================="

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Step 1: Create namespace
echo ""
echo "Step 1: Creating logging namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"

# Step 2: Deploy Elasticsearch
echo ""
echo "Step 2: Deploying Elasticsearch cluster (3 nodes)..."
kubectl apply -f "$SCRIPT_DIR/elasticsearch-statefulset.yaml"

# Wait for Elasticsearch to be ready
echo ""
echo "Waiting for Elasticsearch pods to be ready (this may take 2-3 minutes)..."
kubectl wait --for=condition=ready pod -l app=elasticsearch -n logging --timeout=300s || {
    echo "Warning: Elasticsearch pods not ready yet. Check status with: kubectl get pods -n logging"
}

# Step 3: Deploy Logstash
echo ""
echo "Step 3: Deploying Logstash..."
kubectl apply -f "$SCRIPT_DIR/logstash-deployment.yaml"

# Wait for Logstash to be ready
echo ""
echo "Waiting for Logstash pods to be ready..."
kubectl wait --for=condition=ready pod -l app=logstash -n logging --timeout=180s || {
    echo "Warning: Logstash pods not ready yet. Check status with: kubectl get pods -n logging"
}

# Step 4: Deploy Kibana
echo ""
echo "Step 4: Deploying Kibana..."
kubectl apply -f "$SCRIPT_DIR/kibana-deployment.yaml"

# Wait for Kibana to be ready
echo ""
echo "Waiting for Kibana pod to be ready (this may take 1-2 minutes)..."
kubectl wait --for=condition=ready pod -l app=kibana -n logging --timeout=180s || {
    echo "Warning: Kibana pod not ready yet. Check status with: kubectl get pods -n logging"
}

# Step 5: Display deployment status
echo ""
echo "========================================="
echo "ELK Stack Deployment Complete!"
echo "========================================="
echo ""
echo "Deployment Status:"
kubectl get pods -n logging
echo ""
echo "Services:"
kubectl get svc -n logging
echo ""
echo "========================================="
echo "Access Information:"
echo "========================================="
echo ""
echo "Elasticsearch:"
echo "  Internal: http://elasticsearch.logging.svc.cluster.local:9200"
echo "  Port Forward: kubectl port-forward -n logging svc/elasticsearch 9200:9200"
echo "  Test: curl http://localhost:9200/_cluster/health"
echo ""
echo "Logstash:"
echo "  Internal: logstash.logging.svc.cluster.local:5000 (TCP)"
echo "  Port Forward: kubectl port-forward -n logging svc/logstash 5000:5000"
echo ""
echo "Kibana:"
KIBANA_PORT=$(kubectl get svc kibana -n logging -o jsonpath='{.spec.ports[0].nodePort}')
echo "  NodePort: http://localhost:$KIBANA_PORT"
echo "  Port Forward: kubectl port-forward -n logging svc/kibana 5601:5601"
echo "  Access: http://localhost:5601"
echo ""
echo "========================================="
echo "Next Steps:"
echo "========================================="
echo "1. Configure services to send logs to Logstash"
echo "2. Create index patterns in Kibana"
echo "3. Set up log retention policies"
echo ""
echo "For more information, see README.md"
echo ""
