#!/bin/bash

set -e

echo "=========================================="
echo "部署 Jaeger 链路追踪系统到 Kubernetes"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查 kubectl 是否可用
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}错误: kubectl 未安装或不在 PATH 中${NC}"
    exit 1
fi

# 检查 Kubernetes 集群连接
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}错误: 无法连接到 Kubernetes 集群${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Kubernetes 集群连接正常${NC}"

# 步骤 1: 创建 namespace
echo ""
echo "步骤 1/5: 创建 observability namespace..."
kubectl apply -f namespace.yaml
echo -e "${GREEN}✓ Namespace 创建完成${NC}"

# 步骤 2: 部署 Elasticsearch
echo ""
echo "步骤 2/5: 部署 Elasticsearch 存储后端..."
kubectl apply -f elasticsearch-statefulset.yaml

echo -e "${YELLOW}等待 Elasticsearch 集群启动 (这可能需要 2-3 分钟)...${NC}"
kubectl wait --for=condition=ready pod -l app=elasticsearch -n observability --timeout=300s || {
    echo -e "${RED}警告: Elasticsearch 启动超时，请手动检查${NC}"
    echo "运行以下命令检查状态:"
    echo "  kubectl get pods -n observability -l app=elasticsearch"
    echo "  kubectl logs -n observability elasticsearch-0"
}

# 检查 Elasticsearch 健康状态
echo "检查 Elasticsearch 集群健康状态..."
for i in {1..30}; do
    if kubectl exec -n observability elasticsearch-0 -- curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"\|"status":"yellow"'; then
        echo -e "${GREEN}✓ Elasticsearch 集群健康${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${YELLOW}警告: Elasticsearch 集群未达到健康状态，但继续部署${NC}"
    fi
    sleep 10
done

# 步骤 3: 部署 Jaeger Operator
echo ""
echo "步骤 3/5: 部署 Jaeger Operator..."
kubectl apply -f jaeger-operator.yaml

echo "等待 Jaeger Operator 就绪..."
kubectl wait --for=condition=available deployment/jaeger-operator -n observability --timeout=120s
echo -e "${GREEN}✓ Jaeger Operator 部署完成${NC}"

# 步骤 4: 部署 Jaeger 实例
echo ""
echo "步骤 4/5: 部署 Jaeger 实例..."
kubectl apply -f jaeger-instance.yaml

echo -e "${YELLOW}等待 Jaeger 组件启动 (这可能需要 1-2 分钟)...${NC}"
sleep 30

# 等待 Collector 就绪
kubectl wait --for=condition=available deployment -l app.kubernetes.io/component=collector -n observability --timeout=120s || {
    echo -e "${YELLOW}警告: Jaeger Collector 启动超时${NC}"
}

# 等待 Query 就绪
kubectl wait --for=condition=available deployment -l app.kubernetes.io/component=query -n observability --timeout=120s || {
    echo -e "${YELLOW}警告: Jaeger Query 启动超时${NC}"
}

echo -e "${GREEN}✓ Jaeger 实例部署完成${NC}"

# 步骤 5: 创建服务
echo ""
echo "步骤 5/5: 创建 Jaeger 服务..."
kubectl apply -f jaeger-service.yaml
echo -e "${GREEN}✓ Jaeger 服务创建完成${NC}"

# 显示部署状态
echo ""
echo "=========================================="
echo "部署完成！"
echo "=========================================="
echo ""
echo "查看部署状态:"
echo "  kubectl get all -n observability"
echo ""
echo "查看 Jaeger Pods:"
echo "  kubectl get pods -n observability -l app=jaeger"
echo ""
echo "查看 Elasticsearch Pods:"
echo "  kubectl get pods -n observability -l app=elasticsearch"
echo ""
echo "访问 Jaeger UI:"
echo "  kubectl port-forward -n observability svc/jaeger-query 16686:16686"
echo "  然后在浏览器访问: http://localhost:16686"
echo ""
echo "或者通过 NodePort 访问:"
NODEPORT=$(kubectl get svc jaeger-query -n observability -o jsonpath='{.spec.ports[0].nodePort}')
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
echo "  http://${NODE_IP}:${NODEPORT}"
echo ""
echo "Jaeger Collector 端点 (用于应用配置):"
echo "  OTLP gRPC: jaeger-collector.observability.svc.cluster.local:4317"
echo "  OTLP HTTP: jaeger-collector.observability.svc.cluster.local:4318"
echo "  Jaeger Thrift: jaeger-collector.observability.svc.cluster.local:14250"
echo ""
echo "查看日志:"
echo "  kubectl logs -n observability -l app.kubernetes.io/component=collector --tail=50"
echo "  kubectl logs -n observability -l app.kubernetes.io/component=query --tail=50"
echo ""
