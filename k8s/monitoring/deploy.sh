#!/bin/bash

# Prometheus + Grafana 监控系统部署脚本
# 用途：一键部署完整的监控系统到 Kubernetes 集群

set -e

echo "========================================="
echo "部署 Prometheus + Grafana 监控系统"
echo "========================================="

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

# 1. 创建 monitoring 命名空间
echo ""
echo "步骤 1/6: 创建 monitoring 命名空间..."
kubectl apply -f namespace.yaml
echo -e "${GREEN}✓ 命名空间创建完成${NC}"

# 2. 部署 Prometheus
echo ""
echo "步骤 2/6: 部署 Prometheus..."
kubectl apply -f prometheus-config.yaml
kubectl apply -f prometheus-rules.yaml
kubectl apply -f prometheus-deployment.yaml

echo "等待 Prometheus 就绪..."
kubectl wait --for=condition=available --timeout=300s deployment/prometheus -n monitoring
echo -e "${GREEN}✓ Prometheus 部署完成${NC}"

# 3. 部署 AlertManager
echo ""
echo "步骤 3/6: 部署 AlertManager..."
kubectl apply -f alertmanager-config.yaml
kubectl apply -f alertmanager-deployment.yaml

echo "等待 AlertManager 就绪..."
kubectl wait --for=condition=available --timeout=180s deployment/alertmanager -n monitoring
echo -e "${GREEN}✓ AlertManager 部署完成${NC}"

# 4. 部署 Grafana
echo ""
echo "步骤 4/6: 部署 Grafana..."
kubectl apply -f grafana-config.yaml
kubectl apply -f grafana-dashboards.yaml
kubectl apply -f grafana-deployment.yaml

echo "等待 Grafana 就绪..."
kubectl wait --for=condition=available --timeout=300s deployment/grafana -n monitoring
echo -e "${GREEN}✓ Grafana 部署完成${NC}"

# 5. 显示部署状态
echo ""
echo "步骤 5/6: 检查部署状态..."
kubectl get pods -n monitoring
kubectl get svc -n monitoring

# 6. 显示访问信息
echo ""
echo "========================================="
echo "部署完成！"
echo "========================================="
echo ""
echo -e "${GREEN}访问信息：${NC}"
echo ""

# 获取 NodePort
PROMETHEUS_PORT=$(kubectl get svc prometheus -n monitoring -o jsonpath='{.spec.ports[0].nodePort}')
ALERTMANAGER_PORT=$(kubectl get svc alertmanager -n monitoring -o jsonpath='{.spec.ports[0].nodePort}')
GRAFANA_PORT=$(kubectl get svc grafana -n monitoring -o jsonpath='{.spec.ports[0].nodePort}')

# 获取节点 IP（使用第一个节点）
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')

echo "Prometheus UI:"
echo "  URL: http://${NODE_IP}:${PROMETHEUS_PORT}"
echo "  NodePort: ${PROMETHEUS_PORT}"
echo ""

echo "AlertManager UI:"
echo "  URL: http://${NODE_IP}:${ALERTMANAGER_PORT}"
echo "  NodePort: ${ALERTMANAGER_PORT}"
echo ""

echo "Grafana UI:"
echo "  URL: http://${NODE_IP}:${GRAFANA_PORT}"
echo "  NodePort: ${GRAFANA_PORT}"
echo "  默认用户名: admin"
echo "  默认密码: admin123"
echo ""

echo -e "${YELLOW}注意事项：${NC}"
echo "1. 首次访问 Grafana 后，建议修改默认密码"
echo "2. AlertManager 的邮件、钉钉、企业微信通知需要配置实际的 Token 和密钥"
echo "3. 确保微服务的 Pod 添加了 prometheus.io/scrape 注解"
echo "4. Prometheus 数据保留 30 天，请定期检查存储空间"
echo ""

echo -e "${GREEN}快速验证命令：${NC}"
echo "  查看 Prometheus 目标: curl http://${NODE_IP}:${PROMETHEUS_PORT}/api/v1/targets"
echo "  查看告警规则: curl http://${NODE_IP}:${PROMETHEUS_PORT}/api/v1/rules"
echo "  查看 Grafana 健康状态: curl http://${NODE_IP}:${GRAFANA_PORT}/api/health"
echo ""

echo "========================================="
echo "部署脚本执行完成"
echo "========================================="
