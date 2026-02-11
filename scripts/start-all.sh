#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志目录
LOGS_DIR="logs"
mkdir -p "$LOGS_DIR"

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 微服务列表（按启动顺序）
declare -A SERVICES=(
    ["cuckoo-user"]="8081"
    ["cuckoo-product"]="8082"
    ["cuckoo-inventory"]="8084"
    ["cuckoo-order"]="8083"
    ["cuckoo-payment"]="8085"
    ["cuckoo-notification"]="8086"
    ["cuckoo-gateway"]="8080"
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

# 检查 Docker Compose 状态
check_docker_compose() {
    print_info "检查 Docker Compose 基础设施状态..."
    
    if ! docker compose ps | grep -q "Up"; then
        print_warning "Docker Compose 基础设施未运行，正在启动..."
        cd "$PROJECT_ROOT" || exit 1
        docker compose up -d
        
        print_info "等待基础设施就绪（60秒）..."
        sleep 60
        
        print_success "Docker Compose 基础设施已启动"
    else
        print_success "Docker Compose 基础设施已运行"
    fi
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0  # 端口被占用
    else
        return 1  # 端口空闲
    fi
}

# 启动单个微服务
start_service() {
    local service_name=$1
    local port=$2
    local jar_file="$PROJECT_ROOT/$service_name/target/$service_name-1.0.0-SNAPSHOT.jar"
    local log_file="$LOGS_DIR/$service_name.log"
    local pid_file="$LOGS_DIR/$service_name.pid"
    
    print_info "启动 $service_name (端口: $port)..."
    
    # 检查 JAR 文件是否存在
    if [ ! -f "$jar_file" ]; then
        print_error "JAR 文件不存在: $jar_file"
        print_info "请先运行: mvn clean package -DskipTests"
        return 1
    fi
    
    # 检查端口是否已被占用
    if check_port $port; then
        print_warning "$service_name 端口 $port 已被占用，跳过启动"
        return 0
    fi
    
    # 启动服务
    nohup java -jar "$jar_file" > "$log_file" 2>&1 &
    local pid=$!
    echo $pid > "$pid_file"
    
    print_success "$service_name 已启动 (PID: $pid, 日志: $log_file)"
    
    # 等待服务启动
    sleep 5
}

# 主函数
main() {
    echo ""
    print_info "=========================================="
    print_info "  Cuckoo Microservices 启动脚本"
    print_info "=========================================="
    echo ""
    
    # 检查并启动 Docker Compose
    check_docker_compose
    echo ""
    
    # 按顺序启动所有微服务
    print_info "开始启动微服务..."
    echo ""
    
    for service in cuckoo-user cuckoo-product cuckoo-inventory cuckoo-order cuckoo-payment cuckoo-notification cuckoo-gateway; do
        start_service "$service" "${SERVICES[$service]}"
    done
    
    echo ""
    print_success "=========================================="
    print_success "  所有微服务启动完成！"
    print_success "=========================================="
    echo ""
    print_info "查看日志: tail -f $LOGS_DIR/<service-name>.log"
    print_info "停止服务: ./scripts/stop-all.sh"
    echo ""
}

# 执行主函数
main
