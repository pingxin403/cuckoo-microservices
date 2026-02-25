#!/bin/bash

set -e

echo "=========================================="
echo "清理 Jaeger 链路追踪系统"
echo "=========================================="

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 确认删除
read -p "确定要删除 Jaeger 和 Elasticsearch 吗？这将删除所有追踪数据！(yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "取消清理操作"
    exit 0
fi

echo ""
echo "步骤 1/5: 删除 Jaeger 服务..."
kubectl delete -f jaeger-service.yaml --ignore-not-found=true
echo -e "${GREEN}✓ Jaeger 服务已删除${NC}"

echo ""
echo "步骤 2/5: 删除 Jaeger 实例..."
kubectl delete -f jaeger-instance.yaml --ignore-not-found=true
echo -e "${YELLOW}等待 Jaeger 组件清理...${NC}"
sleep 10
echo -e "${GREEN}✓ Jaeger 实例已删除${NC}"

echo ""
echo "步骤 3/5: 删除 Jaeger Operator..."
kubectl delete -f jaeger-operator.yaml --ignore-not-found=true
echo -e "${GREEN}✓ Jaeger Operator 已删除${NC}"

echo ""
echo "步骤 4/5: 删除 Elasticsearch..."
kubectl delete -f elasticsearch-statefulset.yaml --ignore-not-found=true
echo -e "${YELLOW}等待 Elasticsearch 清理...${NC}"
sleep 10
echo -e "${GREEN}✓ Elasticsearch 已删除${NC}"

echo ""
echo "步骤 5/5: 删除 PVC (持久化数据)..."
kubectl delete pvc -n observability -l app=elasticsearch --ignore-not-found=true
echo -e "${GREEN}✓ PVC 已删除${NC}"

echo ""
echo "是否删除 observability namespace? (yes/no): "
read delete_ns
if [ "$delete_ns" = "yes" ]; then
    kubectl delete namespace observability --ignore-not-found=true
    echo -e "${GREEN}✓ Namespace 已删除${NC}"
fi

echo ""
echo "=========================================="
echo "清理完成！"
echo "=========================================="
echo ""
echo "验证清理结果:"
echo "  kubectl get all -n observability"
echo ""
