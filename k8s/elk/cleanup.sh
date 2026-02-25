#!/bin/bash

# ELK Stack Cleanup Script
# This script removes the ELK stack from Kubernetes

set -e

echo "========================================="
echo "Cleaning up ELK Stack"
echo "========================================="

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Confirm deletion
read -p "Are you sure you want to delete the ELK stack? This will remove all logs. (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo "Deleting Kibana..."
kubectl delete -f "$SCRIPT_DIR/kibana-deployment.yaml" --ignore-not-found=true

echo ""
echo "Deleting Logstash..."
kubectl delete -f "$SCRIPT_DIR/logstash-deployment.yaml" --ignore-not-found=true

echo ""
echo "Deleting Elasticsearch..."
kubectl delete -f "$SCRIPT_DIR/elasticsearch-statefulset.yaml" --ignore-not-found=true

echo ""
echo "Waiting for pods to terminate..."
kubectl wait --for=delete pod -l app=elasticsearch -n logging --timeout=60s 2>/dev/null || true
kubectl wait --for=delete pod -l app=logstash -n logging --timeout=60s 2>/dev/null || true
kubectl wait --for=delete pod -l app=kibana -n logging --timeout=60s 2>/dev/null || true

echo ""
echo "Deleting PersistentVolumeClaims..."
kubectl delete pvc -n logging -l app=elasticsearch --ignore-not-found=true

echo ""
echo "Deleting namespace..."
kubectl delete -f "$SCRIPT_DIR/namespace.yaml" --ignore-not-found=true

echo ""
echo "========================================="
echo "ELK Stack cleanup complete!"
echo "========================================="
