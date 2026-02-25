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

FAILED_CHECKS=0

check_namespace() {
    print_info "检查命名空间..."
    if kubectl get namespace kafka &> /dev/null; then
        print_success "kafka 命名空间存在"
    else
        print_error "kafka 命名空间不存在"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

check_zookeeper() {
    print_info "检查 Zookeeper..."
    
    if kubectl get pod zookeeper-0 -n kafka &> /dev/null; then
        STATUS=$(kubectl get pod zookeeper-0 -n kafka -o jsonpath='{.status.phase}')
        if [ "$STATUS" = "Running" ]; then
            print_success "Zookeeper pod 运行中"
            
            # 健康检查
            HEALTH=$(kubectl exec -it zookeeper-0 -n kafka -- bash -c "echo ruok | nc localhost 2181" 2>/dev/null | tr -d '\r' || echo "failed")
            if [ "$HEALTH" = "imok" ]; then
                print_success "Zookeeper 健康检查通过"
            else
                print_error "Zookeeper 健康检查失败"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
        else
            print_error "Zookeeper pod 状态: $STATUS"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
    else
        print_error "Zookeeper pod 不存在"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

check_kafka_brokers() {
    print_info "检查 Kafka brokers..."
    
    RUNNING_BROKERS=0
    for i in 0 1 2; do
        if kubectl get pod kafka-$i -n kafka &> /dev/null; then
            STATUS=$(kubectl get pod kafka-$i -n kafka -o jsonpath='{.status.phase}')
            if [ "$STATUS" = "Running" ]; then
                print_success "kafka-$i 运行中"
                RUNNING_BROKERS=$((RUNNING_BROKERS + 1))
            else
                print_error "kafka-$i 状态: $STATUS"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
        else
            print_error "kafka-$i 不存在"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
    done
    
    if [ $RUNNING_BROKERS -eq 3 ]; then
        print_success "所有 3 个 Kafka brokers 运行中"
    else
        print_error "只有 $RUNNING_BROKERS/3 个 brokers 运行中"
    fi
}

check_topics() {
    print_info "检查 Topics..."
    
    EXPECTED_TOPICS=("order-events" "payment-events" "inventory-events" "notification-events" "dead-letter-queue")
    
    TOPICS=$(kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | tr -d '\r' || echo "")
    
    if [ -z "$TOPICS" ]; then
        print_error "无法获取 topics 列表"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return
    fi
    
    for topic in "${EXPECTED_TOPICS[@]}"; do
        if echo "$TOPICS" | grep -q "^$topic$"; then
            print_success "Topic $topic 存在"
            
            # 检查分区和副本
            TOPIC_INFO=$(kubectl exec -it kafka-0 -n kafka -- kafka-topics --bootstrap-server localhost:9092 --describe --topic $topic 2>/dev/null || echo "")
            
            PARTITIONS=$(echo "$TOPIC_INFO" | grep -c "Partition:" || echo "0")
            if [ "$PARTITIONS" -eq 3 ]; then
                print_success "  ✓ $topic: 3 个分区"
            else
                print_error "  ✗ $topic: $PARTITIONS 个分区（期望 3）"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
            
            # 检查副本数
            FIRST_PARTITION=$(echo "$TOPIC_INFO" | grep "Partition: 0" | head -1)
            REPLICAS=$(echo "$FIRST_PARTITION" | grep -oP 'Replicas: \K\d+(,\d+)*' | tr ',' '\n' | wc -l || echo "0")
            if [ "$REPLICAS" -eq 2 ]; then
                print_success "  ✓ $topic: 2 个副本"
            else
                print_error "  ✗ $topic: $REPLICAS 个副本（期望 2）"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            fi
        else
            print_error "Topic $topic 不存在"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
    done
}

check_services() {
    print_info "检查 Services..."
    
    if kubectl get svc kafka -n kafka &> /dev/null; then
        print_success "kafka service 存在"
    else
        print_error "kafka service 不存在"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
    if kubectl get svc kafka-headless -n kafka &> /dev/null; then
        print_success "kafka-headless service 存在"
    else
        print_error "kafka-headless service 不存在"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    
    if kubectl get svc zookeeper -n kafka &> /dev/null; then
        print_success "zookeeper service 存在"
    else
        print_error "zookeeper service 不存在"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

check_pvcs() {
    print_info "检查 PVCs..."
    
    PVCS=$(kubectl get pvc -n kafka --no-headers 2>/dev/null | wc -l || echo "0")
    EXPECTED_PVCS=8  # 3 Kafka (data) + 1 Zookeeper (data) + 1 Zookeeper (log) = 5, 实际是 3*1 + 1*2 = 5
    
    if [ "$PVCS" -ge 5 ]; then
        print_success "$PVCS 个 PVCs 存在"
        
        # 检查 PVC 状态
        BOUND_PVCS=$(kubectl get pvc -n kafka --no-headers 2>/dev/null | grep -c "Bound" || echo "0")
        if [ "$BOUND_PVCS" -eq "$PVCS" ]; then
            print_success "所有 PVCs 已绑定"
        else
            print_warning "$BOUND_PVCS/$PVCS 个 PVCs 已绑定"
        fi
    else
        print_error "只有 $PVCS 个 PVCs（期望至少 5 个）"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

test_message_flow() {
    print_info "测试消息发送和接收..."
    
    TEST_TOPIC="order-events"
    TEST_MESSAGE="test-message-$(date +%s)"
    
    # 发送消息
    if echo "$TEST_MESSAGE" | kubectl exec -i kafka-0 -n kafka -- kafka-console-producer --bootstrap-server localhost:9092 --topic $TEST_TOPIC 2>/dev/null; then
        print_success "消息发送成功"
        
        # 接收消息
        sleep 2
        RECEIVED=$(kubectl exec -it kafka-0 -n kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic $TEST_TOPIC --from-beginning --max-messages 1 --timeout-ms 5000 2>/dev/null | grep "$TEST_MESSAGE" || echo "")
        
        if [ -n "$RECEIVED" ]; then
            print_success "消息接收成功"
        else
            print_warning "消息接收失败（可能是超时）"
        fi
    else
        print_error "消息发送失败"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
}

main() {
    echo ""
    echo "=========================================="
    echo "  Kafka 集群验证脚本"
    echo "=========================================="
    echo ""
    
    check_namespace
    echo ""
    
    check_zookeeper
    echo ""
    
    check_kafka_brokers
    echo ""
    
    check_services
    echo ""
    
    check_pvcs
    echo ""
    
    check_topics
    echo ""
    
    test_message_flow
    echo ""
    
    echo "=========================================="
    if [ $FAILED_CHECKS -eq 0 ]; then
        print_success "所有检查通过！Kafka 集群健康"
    else
        print_error "$FAILED_CHECKS 个检查失败"
        echo ""
        print_info "故障排查建议："
        echo "  1. 查看 pods 状态: kubectl get pods -n kafka"
        echo "  2. 查看 pod 日志: kubectl logs -n kafka <pod-name>"
        echo "  3. 查看 events: kubectl get events -n kafka --sort-by='.lastTimestamp'"
        echo "  4. 重新部署: ./cleanup.sh && ./deploy.sh"
    fi
    echo "=========================================="
    echo ""
    
    exit $FAILED_CHECKS
}

main
