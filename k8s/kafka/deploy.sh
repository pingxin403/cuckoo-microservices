#!/bin/bash

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
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

# 检查 kubectl 是否可用
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl 未安装或不在 PATH 中"
        exit 1
    fi
    print_success "kubectl 已安装"
}

# 检查集群连接
check_cluster() {
    if ! kubectl cluster-info &> /dev/null; then
        print_error "无法连接到 Kubernetes 集群"
        exit 1
    fi
    print_success "已连接到 Kubernetes 集群"
}

# 检查资源
check_resources() {
    print_info "检查集群资源..."
    
    # 获取可用资源
    AVAILABLE_CPU=$(kubectl top nodes 2>/dev/null | awk 'NR>1 {sum+=$3} END {print sum}' || echo "unknown")
    AVAILABLE_MEM=$(kubectl top nodes 2>/dev/null | awk 'NR>1 {sum+=$5} END {print sum}' || echo "unknown")
    
    if [ "$AVAILABLE_CPU" != "unknown" ]; then
        print_info "可用 CPU: $AVAILABLE_CPU"
        print_info "可用内存: $AVAILABLE_MEM"
    else
        print_warning "无法获取集群资源信息（metrics-server 可能未安装）"
    fi
}

# 部署命名空间
deploy_namespace() {
    print_info "创建 kafka 命名空间..."
    kubectl apply -f namespace.yaml
    print_success "命名空间创建成功"
}

# 部署 Zookeeper
deploy_zookeeper() {
    print_info "部署 Zookeeper..."
    kubectl apply -f zookeeper-statefulset.yaml
    
    print_info "等待 Zookeeper 就绪（最多 5 分钟）..."
    if kubectl wait --for=condition=ready pod/zookeeper-0 -n kafka --timeout=300s; then
        print_success "Zookeeper 部署成功"
        
        # 验证 Zookeeper
        print_info "验证 Zookeeper 状态..."
        ZOOKEEPER_STATUS=$(kubectl exec -it zookeeper-0 -n kafka -- bash -c "echo ruok | nc localhost 2181" 2>/dev/null | tr -d '\r')
        if [ "$ZOOKEEPER_STATUS" = "imok" ]; then
            print_success "Zookeeper 健康检查通过"
        else
            print_warning "Zookeeper 健康检查失败，但继续部署"
        fi
    else
        print_error "Zookeeper 部署超时"
        exit 1
    fi
}

# 部署 Kafka
deploy_kafka() {
    print_info "部署 Kafka 集群（3 个 brokers）..."
    kubectl apply -f kafka-statefulset.yaml
    
    print_info "等待 Kafka brokers 就绪（最多 5 分钟）..."
    
    for i in 0 1 2; do
        print_info "等待 kafka-$i..."
        if kubectl wait --for=condition=ready pod/kafka-$i -n kafka --timeout=300s; then
            print_success "kafka-$i 就绪"
        else
            print_error "kafka-$i 部署超时"
            exit 1
        fi
    done
    
    print_success "Kafka 集群部署成功"
}

# 创建 Topics
create_topics() {
    print_info "创建 Kafka topics..."
    kubectl apply -f topic-creation-job.yaml
    
    print_info "等待 topic 创建完成（最多 2 分钟）..."
    sleep 10
    
    # 等待 Job 完成
    for i in {1..24}; do
        JOB_STATUS=$(kubectl get job kafka-topic-creation -n kafka -o jsonpath='{.status.succeeded}' 2>/dev/null || echo "0")
        if [ "$JOB_STATUS" = "1" ]; then
            print_success "Topics 创建成功"
            
            # 显示 Job 日志
            print_info "Topic 创建日志："
            kubectl logs -n kafka job/kafka-topic-creation --tail=50
            return 0
        fi
        
        if [ $((i % 4)) -eq 0 ]; then
            print_info "等待中... ($((i * 5))s)"
        fi
        sleep 5
    done
    
    print_error "Topic 创建超时"
    print_info "查看日志："
    kubectl logs -n kafka job/kafka-topic-creation --tail=100
    exit 1
}

# 验证部署
verify_deployment() {
    print_info "验证 Kafka 集群部署..."
    
    echo ""
    print_info "=== Pods 状态 ==="
    kubectl get pods -n kafka
    
    echo ""
    print_info "=== Services ==="
    kubectl get svc -n kafka
    
    echo ""
    print_info "=== StatefulSets ==="
    kubectl get statefulset -n kafka
    
    echo ""
    print_info "=== PVCs ==="
    kubectl get pvc -n kafka
    
    echo ""
    print_info "=== Topics 列表 ==="
    kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --list
    
    echo ""
    print_info "=== Topics 详细信息 ==="
    for topic in order-events payment-events inventory-events notification-events dead-letter-queue; do
        echo ""
        print_info "Topic: $topic"
        kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe --topic $topic
    done
}

# 部署健康检查
deploy_health_check() {
    print_info "部署健康检查 CronJob..."
    kubectl apply -f health-check-cronjob.yaml
    print_success "健康检查 CronJob 部署成功"
    
    print_info "手动触发一次健康检查..."
    kubectl create job --from=cronjob/kafka-health-check kafka-health-check-manual -n kafka 2>/dev/null || true
    sleep 5
    
    print_info "健康检查日志："
    kubectl logs -n kafka job/kafka-health-check-manual --tail=100 2>/dev/null || print_warning "健康检查尚未完成"
}

# 主函数
main() {
    echo ""
    echo "=========================================="
    echo "  Kafka 事件总线基础设施部署脚本"
    echo "=========================================="
    echo ""
    
    # 前置检查
    check_kubectl
    check_cluster
    check_resources
    
    echo ""
    print_info "开始部署..."
    echo ""
    
    # 部署步骤
    deploy_namespace
    echo ""
    
    deploy_zookeeper
    echo ""
    
    deploy_kafka
    echo ""
    
    create_topics
    echo ""
    
    deploy_health_check
    echo ""
    
    verify_deployment
    echo ""
    
    echo "=========================================="
    print_success "Kafka 集群部署完成！"
    echo "=========================================="
    echo ""
    print_info "连接信息："
    echo "  集群内部: kafka.kafka.svc.cluster.local:9092"
    echo "  Broker 0: kafka-0.kafka-headless.kafka.svc.cluster.local:9093"
    echo "  Broker 1: kafka-1.kafka-headless.kafka.svc.cluster.local:9093"
    echo "  Broker 2: kafka-2.kafka-headless.kafka.svc.cluster.local:9093"
    echo ""
    print_info "Topics:"
    echo "  - order-events (3 分区, 2 副本)"
    echo "  - payment-events (3 分区, 2 副本)"
    echo "  - inventory-events (3 分区, 2 副本)"
    echo "  - notification-events (3 分区, 2 副本)"
    echo "  - dead-letter-queue (3 分区, 2 副本)"
    echo ""
    print_info "下一步："
    echo "  1. 查看详细文档: cat README.md"
    echo "  2. 测试消息发送: kubectl exec -it kafka-0 -n kafka -- bash"
    echo "  3. 查看健康检查: kubectl logs -n kafka job/kafka-health-check-manual"
    echo ""
}

# 执行主函数
main
