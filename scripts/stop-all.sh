#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志目录
LOGS_DIR="logs"

# 微服务列表
SERVICES=(
    "cuckoo-gateway"
    "cuckoo-notification"
    "cuckoo-payment"
    "cuckoo-order"
    "cuckoo-inventory"
    "cuckoo-product"
    "cuckoo-user"
)

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 停止单个微服务
stop_service() {
    local service_name=$1
    local pid_file="$LOGS_DIR/$service_name.pid"
    
    if [ ! -f "$pid_file" ]; then
        print_warning "$service_name PID 文件不存在，跳过"
        return 0
    fi
    
    local pid=$(cat "$pid_file")
    
    if [ -z "$pid" ]; then
        print_warning "$service_name PID 为空，删除 PID 文件"
        rm -f "$pid_file"
        return 0
    fi
    
    # 检查进程是否存在
    if ! ps -p $pid > /dev/null 2>&1; then
        print_warning "$service_name 进程不存在 (PID: $pid)，删除 PID 文件"
        rm -f "$pid_file"
        return 0
    fi
    
    print_info "停止 $service_name (PID: $pid)..."
    
    # 尝试优雅停止
    kill $pid 2>/dev/null
    
    # 等待进程结束（最多10秒）
    local count=0
    while ps -p $pid > /dev/null 2>&1 && [ $count -lt 10 ]; do
        sleep 1
        count=$((count + 1))
    done
    
    # 如果进程仍然存在，强制停止
    if ps -p $pid > /dev/null 2>&1; then
        print_warning "$service_name 未响应，强制停止..."
        kill -9 $pid 2>/dev/null
        sleep 1
    fi
    
    # 删除 PID 文件
    rm -f "$pid_file"
    
    print_success "$service_name 已停止"
}

# 主函数
main() {
    echo ""
    print_info "=========================================="
    print_info "  Cuckoo Microservices 停止脚本"
    print_info "=========================================="
    echo ""
    
    # 检查日志目录是否存在
    if [ ! -d "$LOGS_DIR" ]; then
        print_warning "日志目录不存在，没有服务需要停止"
        return 0
    fi
    
    # 按相反顺序停止所有微服务
    print_info "开始停止微服务..."
    echo ""
    
    for service in "${SERVICES[@]}"; do
        stop_service "$service"
    done
    
    echo ""
    print_success "=========================================="
    print_success "  所有微服务已停止！"
    print_success "=========================================="
    echo ""
}

# 执行主函数
main
