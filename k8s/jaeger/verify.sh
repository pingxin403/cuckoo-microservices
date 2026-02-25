#!/bin/bash

set -e

echo "=========================================="
echo "验证 Jaeger 部署状态"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查 namespace
echo ""
echo "1. 检查 observability namespace..."
if kubectl get namespace observability &> /dev/null; then
    echo -e "${GREEN}✓ Namespace 存在${NC}"
else
    echo -e "${RED}✗ Namespace 不存在${NC}"
    exit 1
fi

# 检查 Elasticsearch
echo ""
echo "2. 检查 Elasticsearch 集群..."
ES_PODS=$(kubectl get pods -n observability -l app=elasticsearch --no-headers 2>/dev/null | wc -l)
ES_READY=$(kubectl get pods -n observability -l app=elasticsearch --no-headers 2>/dev/null | grep "Running" | wc -l)

echo "   Elasticsearch Pods: $ES_READY/$ES_PODS 运行中"
if [ "$ES_READY" -eq 3 ]; then
    echo -e "${GREEN}✓ Elasticsearch 集群正常 (3/3)${NC}"
else
    echo -e "${YELLOW}⚠ Elasticsearch 集群未完全就绪 ($ES_READY/3)${NC}"
fi

# 检查 Elasticsearch 健康状态
echo ""
echo "3. 检查 Elasticsearch 集群健康..."
if kubectl exec -n observability elasticsearch-0 -- curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -q '"status":"green"\|"status":"yellow"'; then
    HEALTH=$(kubectl exec -n observability elasticsearch-0 -- curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -o '"status":"[^"]*"')
    echo -e "${GREEN}✓ Elasticsearch 集群健康: $HEALTH${NC}"
else
    echo -e "${RED}✗ Elasticsearch 集群不健康${NC}"
fi

# 检查 Jaeger Operator
echo ""
echo "4. 检查 Jaeger Operator..."
if kubectl get deployment jaeger-operator -n observability &> /dev/null; then
    OPERATOR_READY=$(kubectl get deployment jaeger-operator -n observability -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    if [ "$OPERATOR_READY" -ge 1 ]; then
        echo -e "${GREEN}✓ Jaeger Operator 运行中${NC}"
    else
        echo -e "${RED}✗ Jaeger Operator 未就绪${NC}"
    fi
else
    echo -e "${RED}✗ Jaeger Operator 不存在${NC}"
fi

# 检查 Jaeger Collector
echo ""
echo "5. 检查 Jaeger Collector..."
COLLECTOR_PODS=$(kubectl get pods -n observability -l app.kubernetes.io/component=collector --no-headers 2>/dev/null | wc -l)
COLLECTOR_READY=$(kubectl get pods -n observability -l app.kubernetes.io/component=collector --no-headers 2>/dev/null | grep "Running" | wc -l)

echo "   Collector Pods: $COLLECTOR_READY/$COLLECTOR_PODS 运行中"
if [ "$COLLECTOR_READY" -ge 1 ]; then
    echo -e "${GREEN}✓ Jaeger Collector 正常${NC}"
else
    echo -e "${RED}✗ Jaeger Collector 未就绪${NC}"
fi

# 检查 Jaeger Query
echo ""
echo "6. 检查 Jaeger Query..."
QUERY_PODS=$(kubectl get pods -n observability -l app.kubernetes.io/component=query --no-headers 2>/dev/null | wc -l)
QUERY_READY=$(kubectl get pods -n observability -l app.kubernetes.io/component=query --no-headers 2>/dev/null | grep "Running" | wc -l)

echo "   Query Pods: $QUERY_READY/$QUERY_PODS 运行中"
if [ "$QUERY_READY" -ge 1 ]; then
    echo -e "${GREEN}✓ Jaeger Query 正常${NC}"
else
    echo -e "${RED}✗ Jaeger Query 未就绪${NC}"
fi

# 检查 Jaeger Agent
echo ""
echo "7. 检查 Jaeger Agent..."
AGENT_PODS=$(kubectl get pods -n observability -l app.kubernetes.io/component=agent --no-headers 2>/dev/null | wc -l)
AGENT_READY=$(kubectl get pods -n observability -l app.kubernetes.io/component=agent --no-headers 2>/dev/null | grep "Running" | wc -l)

echo "   Agent Pods: $AGENT_READY/$AGENT_PODS 运行中"
if [ "$AGENT_READY" -ge 1 ]; then
    echo -e "${GREEN}✓ Jaeger Agent 正常${NC}"
else
    echo -e "${YELLOW}⚠ Jaeger Agent 未就绪 (可选组件)${NC}"
fi

# 检查服务
echo ""
echo "8. 检查 Jaeger 服务..."
if kubectl get svc jaeger-collector -n observability &> /dev/null; then
    echo -e "${GREEN}✓ jaeger-collector 服务存在${NC}"
else
    echo -e "${RED}✗ jaeger-collector 服务不存在${NC}"
fi

if kubectl get svc jaeger-query -n observability &> /dev/null; then
    echo -e "${GREEN}✓ jaeger-query 服务存在${NC}"
    NODEPORT=$(kubectl get svc jaeger-query -n observability -o jsonpath='{.spec.ports[0].nodePort}')
    echo "   NodePort: $NODEPORT"
else
    echo -e "${RED}✗ jaeger-query 服务不存在${NC}"
fi

# 测试 Collector 连接
echo ""
echo "9. 测试 Collector 连接..."
if kubectl run test-collector --image=curlimages/curl:latest --rm -i --restart=Never -n observability -- curl -s -o /dev/null -w "%{http_code}" http://jaeger-collector:14269/ 2>/dev/null | grep -q "200\|404"; then
    echo -e "${GREEN}✓ Collector 端点可访问${NC}"
else
    echo -e "${YELLOW}⚠ Collector 端点测试失败 (可能正在启动)${NC}"
fi

# 测试 Query UI
echo ""
echo "10. 测试 Query UI..."
if kubectl run test-query --image=curlimages/curl:latest --rm -i --restart=Never -n observability -- curl -s -o /dev/null -w "%{http_code}" http://jaeger-query:16686/ 2>/dev/null | grep -q "200"; then
    echo -e "${GREEN}✓ Query UI 可访问${NC}"
else
    echo -e "${YELLOW}⚠ Query UI 测试失败 (可能正在启动)${NC}"
fi

# 显示所有资源
echo ""
echo "=========================================="
echo "所有 Jaeger 相关资源:"
echo "=========================================="
kubectl get all -n observability

# 显示访问信息
echo ""
echo "=========================================="
echo "访问信息:"
echo "=========================================="
echo ""
echo "Jaeger UI (通过 port-forward):"
echo "  kubectl port-forward -n observability svc/jaeger-query 16686:16686"
echo "  浏览器访问: http://localhost:16686"
echo ""
echo "Jaeger UI (通过 NodePort):"
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
NODEPORT=$(kubectl get svc jaeger-query -n observability -o jsonpath='{.spec.ports[0].nodePort}')
echo "  http://${NODE_IP}:${NODEPORT}"
echo ""
echo "应用配置端点:"
echo "  OTLP gRPC: jaeger-collector.observability.svc.cluster.local:4317"
echo "  OTLP HTTP: jaeger-collector.observability.svc.cluster.local:4318"
echo "  Jaeger Thrift: jaeger-collector.observability.svc.cluster.local:14250"
echo ""
echo "查看日志:"
echo "  kubectl logs -n observability -l app.kubernetes.io/component=collector --tail=50"
echo "  kubectl logs -n observability -l app.kubernetes.io/component=query --tail=50"
echo "  kubectl logs -n observability -l app=elasticsearch --tail=50"
echo ""
