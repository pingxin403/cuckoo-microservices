#!/bin/bash

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

echo ""
echo "=========================================="
echo "  Kafka 集群清理脚本"
echo "=========================================="
echo ""

print_warning "此操作将删除所有 Kafka 资源和数据！"
echo ""
read -p "确认删除？(yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    print_info "取消清理操作"
    exit 0
fi

echo ""
print_info "开始清理 Kafka 资源..."
echo ""

# 删除 CronJob
print_info "删除健康检查 CronJob..."
kubectl delete -f health-check-cronjob.yaml 2>/dev/null || print_warning "CronJob 不存在或已删除"

# 删除手动触发的 Job
kubectl delete job kafka-health-check-manual -n kafka 2>/dev/null || true

# 删除 Topic 创建 Job
print_info "删除 Topic 创建 Job..."
kubectl delete -f topic-creation-job.yaml 2>/dev/null || print_warning "Job 不存在或已删除"

# 删除 Kafka
print_info "删除 Kafka 集群..."
kubectl delete -f kafka-statefulset.yaml 2>/dev/null || print_warning "Kafka 不存在或已删除"

# 等待 Kafka pods 删除
print_info "等待 Kafka pods 删除..."
kubectl wait --for=delete pod/kafka-0 -n kafka --timeout=60s 2>/dev/null || true
kubectl wait --for=delete pod/kafka-1 -n kafka --timeout=60s 2>/dev/null || true
kubectl wait --for=delete pod/kafka-2 -n kafka --timeout=60s 2>/dev/null || true

# 删除 Zookeeper
print_info "删除 Zookeeper..."
kubectl delete -f zookeeper-statefulset.yaml 2>/dev/null || print_warning "Zookeeper 不存在或已删除"

# 等待 Zookeeper pod 删除
print_info "等待 Zookeeper pod 删除..."
kubectl wait --for=delete pod/zookeeper-0 -n kafka --timeout=60s 2>/dev/null || true

# 删除 PVCs
print_info "删除 PVCs..."
kubectl delete pvc -n kafka --all 2>/dev/null || print_warning "PVCs 不存在或已删除"

# 删除命名空间
echo ""
print_warning "是否删除 kafka 命名空间？这将永久删除所有数据！"
read -p "删除命名空间？(yes/no): " DELETE_NS

if [ "$DELETE_NS" = "yes" ]; then
    print_info "删除命名空间..."
    kubectl delete -f namespace.yaml 2>/dev/null || print_warning "命名空间不存在或已删除"
    print_success "命名空间已删除"
else
    print_info "保留命名空间"
fi

echo ""
echo "=========================================="
print_success "Kafka 集群清理完成！"
echo "=========================================="
echo ""

# 验证清理
print_info "验证清理结果："
kubectl get all -n kafka 2>/dev/null || print_success "kafka 命名空间中没有资源"
echo ""
