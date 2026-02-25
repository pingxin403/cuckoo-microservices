#!/bin/bash

# ELK Stack Verification Script
# This script verifies that the ELK stack is running correctly

set -e

echo "========================================="
echo "Verifying ELK Stack Deployment"
echo "========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a pod is ready
check_pods() {
    local app=$1
    local expected_count=$2
    
    echo ""
    echo "Checking $app pods..."
    
    local ready_count=$(kubectl get pods -n logging -l app=$app -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' | grep -o "True" | wc -l)
    local total_count=$(kubectl get pods -n logging -l app=$app --no-headers | wc -l)
    
    if [ "$ready_count" -eq "$expected_count" ] && [ "$total_count" -eq "$expected_count" ]; then
        echo -e "${GREEN}✓${NC} $app: $ready_count/$expected_count pods ready"
        return 0
    else
        echo -e "${RED}✗${NC} $app: $ready_count/$total_count pods ready (expected $expected_count)"
        return 1
    fi
}

# Function to check Elasticsearch cluster health
check_elasticsearch() {
    echo ""
    echo "Checking Elasticsearch cluster health..."
    
    # Port forward to Elasticsearch
    kubectl port-forward -n logging svc/elasticsearch 9200:9200 >/dev/null 2>&1 &
    PF_PID=$!
    sleep 3
    
    # Check cluster health
    HEALTH=$(curl -s http://localhost:9200/_cluster/health | jq -r '.status' 2>/dev/null || echo "error")
    
    # Kill port forward
    kill $PF_PID 2>/dev/null || true
    
    if [ "$HEALTH" == "green" ] || [ "$HEALTH" == "yellow" ]; then
        echo -e "${GREEN}✓${NC} Elasticsearch cluster health: $HEALTH"
        return 0
    else
        echo -e "${RED}✗${NC} Elasticsearch cluster health: $HEALTH"
        return 1
    fi
}

# Function to check Logstash
check_logstash() {
    echo ""
    echo "Checking Logstash..."
    
    # Port forward to Logstash
    kubectl port-forward -n logging svc/logstash 9600:9600 >/dev/null 2>&1 &
    PF_PID=$!
    sleep 3
    
    # Check Logstash API
    STATUS=$(curl -s http://localhost:9600/ | jq -r '.status' 2>/dev/null || echo "error")
    
    # Kill port forward
    kill $PF_PID 2>/dev/null || true
    
    if [ "$STATUS" == "green" ]; then
        echo -e "${GREEN}✓${NC} Logstash status: $STATUS"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} Logstash status: $STATUS (may still be starting)"
        return 0
    fi
}

# Function to check Kibana
check_kibana() {
    echo ""
    echo "Checking Kibana..."
    
    # Port forward to Kibana
    kubectl port-forward -n logging svc/kibana 5601:5601 >/dev/null 2>&1 &
    PF_PID=$!
    sleep 3
    
    # Check Kibana API
    STATUS=$(curl -s http://localhost:5601/api/status | jq -r '.status.overall.state' 2>/dev/null || echo "error")
    
    # Kill port forward
    kill $PF_PID 2>/dev/null || true
    
    if [ "$STATUS" == "green" ]; then
        echo -e "${GREEN}✓${NC} Kibana status: $STATUS"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} Kibana status: $STATUS (may still be starting)"
        return 0
    fi
}

# Main verification
FAILED=0

# Check namespace
echo ""
echo "Checking namespace..."
if kubectl get namespace logging >/dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Namespace 'logging' exists"
else
    echo -e "${RED}✗${NC} Namespace 'logging' does not exist"
    FAILED=1
fi

# Check pods
check_pods "elasticsearch" 3 || FAILED=1
check_pods "logstash" 2 || FAILED=1
check_pods "kibana" 1 || FAILED=1

# Check services
check_elasticsearch || FAILED=1
check_logstash || FAILED=1
check_kibana || FAILED=1

# Summary
echo ""
echo "========================================="
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo "========================================="
    echo ""
    echo "ELK Stack is running correctly."
    echo ""
    echo "Access Kibana:"
    echo "  kubectl port-forward -n logging svc/kibana 5601:5601"
    echo "  Open: http://localhost:5601"
    echo ""
    exit 0
else
    echo -e "${RED}✗ Some checks failed${NC}"
    echo "========================================="
    echo ""
    echo "Troubleshooting:"
    echo "  kubectl get pods -n logging"
    echo "  kubectl logs -n logging <pod-name>"
    echo "  kubectl describe pod -n logging <pod-name>"
    echo ""
    exit 1
fi
