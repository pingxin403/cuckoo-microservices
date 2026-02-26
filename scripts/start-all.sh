#!/bin/bash

################################################################################
# Cuckoo Microservices - Start All Services Script
#
# DESCRIPTION:
#   Starts all 6 microservices in dependency order with health checks.
#   Each service is started only after the previous service becomes healthy.
#
# USAGE:
#   ./start-all.sh
#
# FEATURES:
#   - Starts services in dependency order (user, product, inventory, order, payment, notification)
#   - Health check with 30-attempt retry (60 seconds total)
#   - Timestamped and color-coded logging
#   - Graceful failure handling - continues with remaining services
#   - Creates logs directory for service output
#   - Stores PID files for each service
#
# REQUIREMENTS:
#   - JAR files must be built: mvn clean package -DskipTests
#   - Docker Compose infrastructure must be running
#   - Actuator health endpoint must be available at /actuator/health
#
# AUTHOR: cuckoo-team
################################################################################

set -e

# Color definitions for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directory configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOGS_DIR="$SCRIPT_DIR/logs"

# Create logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

# Service startup order (respecting dependencies)
# Format: "service_name:port"
SERVICES=(
    "cuckoo-user:8081"
    "cuckoo-product:8082"
    "cuckoo-inventory:8084"
    "cuckoo-order:8083"
    "cuckoo-payment:8085"
    "cuckoo-notification:8086"
)

################################################################################
# Logging Functions
################################################################################

# Log informational message with timestamp
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

# Log error message with timestamp
error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

# Log warning message with timestamp
warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARN:${NC} $1"
}

# Log info message with timestamp (blue)
info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

################################################################################
# Health Check Function
################################################################################

# Check if service is healthy
# Args:
#   $1 - port number
# Returns:
#   0 if healthy, 1 if unhealthy after max attempts
check_health() {
    local port=$1
    local max_attempts=30
    local attempt=0
    
    info "等待服务健康检查 (端口: $port, 最大尝试: $max_attempts 次, 间隔: 2 秒)..."
    
    while [ $attempt -lt $max_attempts ]; do
        # Try to curl the health endpoint
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            log "✓ 服务健康检查通过 (端口: $port, 尝试: $((attempt + 1))/$max_attempts)"
            return 0
        fi
        
        attempt=$((attempt + 1))
        
        # Show progress every 5 attempts
        if [ $((attempt % 5)) -eq 0 ]; then
            info "健康检查进度: $attempt/$max_attempts 次尝试..."
        fi
        
        sleep 2
    done
    
    error "✗ 服务健康检查失败 (端口: $port, 已尝试: $max_attempts 次)"
    return 1
}

################################################################################
# Service Start Function
################################################################################

# Start a single service
# Args:
#   $1 - service name
#   $2 - port number
# Returns:
#   0 if started successfully, 1 if failed
start_service() {
    local service_name=$1
    local port=$2
    local jar_file="$PROJECT_ROOT/$service_name/target/$service_name-1.0.0-SNAPSHOT.jar"
    local log_file="$LOGS_DIR/$service_name.log"
    local pid_file="$LOGS_DIR/$service_name.pid"
    
    log "=========================================="
    log "启动服务: $service_name (端口: $port)"
    log "=========================================="
    
    # Check if JAR file exists
    if [ ! -f "$jar_file" ]; then
        error "JAR 文件不存在: $jar_file"
        error "请先运行: mvn clean package -DskipTests"
        warn "跳过 $service_name，继续启动其他服务..."
        return 1
    fi
    
    # Check if port is already in use
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        warn "$service_name 端口 $port 已被占用"
        
        # Check if it's healthy
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            log "✓ $service_name 已在运行且健康，跳过启动"
            return 0
        else
            warn "$service_name 端口被占用但服务不健康，请手动检查"
            warn "跳过 $service_name，继续启动其他服务..."
            return 1
        fi
    fi
    
    # Start service in background
    info "启动 $service_name 进程..."
    nohup java -jar "$jar_file" > "$log_file" 2>&1 &
    local pid=$!
    
    # Save PID to file
    echo $pid > "$pid_file"
    log "✓ $service_name 进程已启动 (PID: $pid)"
    info "日志文件: $log_file"
    
    # Wait for service to become healthy
    if check_health $port; then
        log "✓✓✓ $service_name 启动成功并通过健康检查 ✓✓✓"
        return 0
    else
        error "✗✗✗ $service_name 启动失败或健康检查超时 ✗✗✗"
        warn "查看日志以获取详细信息: tail -f $log_file"
        warn "继续启动其他服务..."
        return 1
    fi
}

################################################################################
# Docker Compose Check Function
################################################################################

# Check and start Docker Compose infrastructure if needed
check_docker_compose() {
    info "检查 Docker Compose 基础设施状态..."
    
    if ! docker compose ps | grep -q "Up"; then
        warn "Docker Compose 基础设施未运行，正在启动..."
        cd "$PROJECT_ROOT" || exit 1
        docker compose up -d
        
        info "等待基础设施就绪（60秒）..."
        sleep 60
        
        log "✓ Docker Compose 基础设施已启动"
    else
        log "✓ Docker Compose 基础设施已运行"
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    echo ""
    log "=========================================="
    log "  Cuckoo Microservices 启动脚本"
    log "  启动时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log "=========================================="
    echo ""
    
    # Check and start Docker Compose infrastructure
    check_docker_compose
    echo ""
    
    # Start all services in dependency order
    log "开始按依赖顺序启动所有微服务..."
    echo ""
    
    local success_count=0
    local failure_count=0
    
    for service_info in "${SERVICES[@]}"; do
        # Parse service name and port
        IFS=':' read -r service port <<< "$service_info"
        
        if start_service "$service" "$port"; then
            success_count=$((success_count + 1))
        else
            failure_count=$((failure_count + 1))
        fi
        
        echo ""
    done
    
    # Print summary
    echo ""
    log "=========================================="
    log "  启动完成摘要"
    log "=========================================="
    log "成功启动: $success_count 个服务"
    if [ $failure_count -gt 0 ]; then
        warn "启动失败: $failure_count 个服务"
    fi
    log "=========================================="
    echo ""
    
    # Print usage instructions
    info "查看所有日志: tail -f $LOGS_DIR/*.log"
    info "查看单个服务日志: tail -f $LOGS_DIR/<service-name>.log"
    info "停止所有服务: $SCRIPT_DIR/stop-all.sh"
    echo ""
}

# Execute main function
main
