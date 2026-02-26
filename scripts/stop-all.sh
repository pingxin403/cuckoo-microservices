#!/bin/bash

################################################################################
# Cuckoo Microservices - Stop All Services Script
#
# DESCRIPTION:
#   Stops all 6 microservices gracefully in reverse dependency order.
#   Each service receives SIGTERM for graceful shutdown with 30-second timeout.
#   If graceful shutdown fails, the service is force-killed with SIGKILL.
#
# USAGE:
#   ./stop-all.sh
#
# FEATURES:
#   - Stops services in reverse dependency order (notification, payment, order, inventory, product, user)
#   - Graceful shutdown with SIGTERM and 30-second timeout
#   - Force-kill with SIGKILL if graceful shutdown fails
#   - Timestamped and color-coded logging
#   - Cleans up PID files after shutdown
#   - Handles missing PID files and non-existent processes gracefully
#
# REQUIREMENTS:
#   - Services must have been started with start-all.sh
#   - PID files must exist in logs directory
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

# Service shutdown order (reverse of startup order)
# Format: "service_name"
SERVICES=(
    "cuckoo-notification"
    "cuckoo-payment"
    "cuckoo-order"
    "cuckoo-inventory"
    "cuckoo-product"
    "cuckoo-user"
)

# Graceful shutdown timeout in seconds
GRACEFUL_TIMEOUT=30

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
# Service Stop Function
################################################################################

# Stop a single service gracefully
# Args:
#   $1 - service name
# Returns:
#   0 if stopped successfully, 1 if service was not running
stop_service() {
    local service_name=$1
    local pid_file="$LOGS_DIR/$service_name.pid"
    
    log "=========================================="
    log "停止服务: $service_name"
    log "=========================================="
    
    # Check if PID file exists
    if [ ! -f "$pid_file" ]; then
        info "服务 $service_name 未运行 (PID 文件不存在: $pid_file)"
        return 0
    fi
    
    # Read PID from file
    local pid=$(cat "$pid_file")
    
    # Check if process exists
    if ! ps -p $pid > /dev/null 2>&1; then
        info "服务 $service_name 未运行 (进程 $pid 不存在)"
        rm -f "$pid_file"
        log "✓ 已清理 PID 文件"
        return 0
    fi
    
    info "发送 SIGTERM 信号进行优雅停止 (PID: $pid, 超时: ${GRACEFUL_TIMEOUT}秒)..."
    
    # Send SIGTERM for graceful shutdown
    kill -TERM $pid
    
    # Wait for graceful shutdown with timeout
    local elapsed=0
    local check_interval=1
    
    while ps -p $pid > /dev/null 2>&1 && [ $elapsed -lt $GRACEFUL_TIMEOUT ]; do
        sleep $check_interval
        elapsed=$((elapsed + check_interval))
        
        # Show progress every 5 seconds
        if [ $((elapsed % 5)) -eq 0 ]; then
            info "等待优雅停止... ($elapsed/${GRACEFUL_TIMEOUT}秒)"
        fi
    done
    
    # Check if process is still running
    if ps -p $pid > /dev/null 2>&1; then
        error "✗ 服务 $service_name 未能在 ${GRACEFUL_TIMEOUT} 秒内优雅停止"
        warn "发送 SIGKILL 信号强制终止进程..."
        
        # Force-kill with SIGKILL
        kill -9 $pid
        sleep 1
        
        # Verify process is killed
        if ps -p $pid > /dev/null 2>&1; then
            error "✗✗✗ 无法终止进程 $pid，请手动检查 ✗✗✗"
            return 1
        else
            warn "✓ 服务 $service_name 已被强制终止 (PID: $pid)"
        fi
    else
        log "✓✓✓ 服务 $service_name 已优雅停止 (PID: $pid, 耗时: ${elapsed}秒) ✓✓✓"
    fi
    
    # Clean up PID file
    rm -f "$pid_file"
    log "✓ 已清理 PID 文件"
    
    return 0
}

################################################################################
# Main Execution
################################################################################

main() {
    echo ""
    log "=========================================="
    log "  Cuckoo Microservices 停止脚本"
    log "  停止时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log "=========================================="
    echo ""
    
    # Check if logs directory exists
    if [ ! -d "$LOGS_DIR" ]; then
        warn "日志目录不存在: $LOGS_DIR"
        warn "可能没有服务在运行"
        return 0
    fi
    
    # Stop all services in reverse dependency order
    log "开始按反向依赖顺序停止所有微服务..."
    echo ""
    
    local success_count=0
    local not_running_count=0
    local failure_count=0
    
    for service in "${SERVICES[@]}"; do
        if stop_service "$service"; then
            # Check if service was actually running
            if [ -f "$LOGS_DIR/$service.pid" ] || ps aux | grep -v grep | grep -q "$service"; then
                success_count=$((success_count + 1))
            else
                not_running_count=$((not_running_count + 1))
            fi
        else
            failure_count=$((failure_count + 1))
        fi
        
        echo ""
    done
    
    # Print summary
    echo ""
    log "=========================================="
    log "  停止完成摘要"
    log "=========================================="
    log "成功停止: $success_count 个服务"
    if [ $not_running_count -gt 0 ]; then
        info "未运行: $not_running_count 个服务"
    fi
    if [ $failure_count -gt 0 ]; then
        error "停止失败: $failure_count 个服务"
    fi
    log "=========================================="
    echo ""
    
    # Print cleanup instructions
    if [ $failure_count -eq 0 ]; then
        log "所有服务已成功停止！"
    else
        warn "部分服务停止失败，请手动检查进程状态"
        info "查看运行中的 Java 进程: ps aux | grep java"
        info "手动终止进程: kill -9 <PID>"
    fi
    echo ""
}

# Execute main function
main
