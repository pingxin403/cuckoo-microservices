#!/bin/bash

# Prometheus + Grafana 监控系统验证脚本
# 用途：验证监控系统是否正常工作

set -e

echo "========================================="
echo "验证 Prometheus + Grafana 监控系统"
echo "========================================="

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查命名空间
echo ""
echo "1. 检查 monitoring 命名空间..."
if kubectl get namespace monitoring &> /dev/null; then
    echo -e "${GREEN}✓ monitoring 命名空间存在${NC}"
else
    echo -e "${RED}✗ monitoring 命名空间不存在${NC}"
    exit 1
fi

# 检查 Pod 状态
echo ""
echo "2. 检查 Pod 状态..."
echo ""
kubectl get pods -n monitoring

PROMETHEUS_POD=$(kubectl get pods -n monitoring -l app=prometheus -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
ALERTMANAGER_POD=$(kubectl get pods -n monitoring -l app=alertmanager -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
GRAFANA_POD=$(kubectl get pods -n monitoring -l app=grafana -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")

if [ -z "$PROMETHEUS_POD" ]; then
    echo -e "${RED}✗ Prometheus Pod 不存在${NC}"
    exit 1
fi

if [ -z "$ALERTMANAGER_POD" ]; then
    echo -e "${RED}✗ AlertManager Pod 不存在${NC}"
    exit 1
fi

if [ -z "$GRAFANA_POD" ]; then
    echo -e "${RED}✗ Grafana Pod 不存在${NC}"
    exit 1
fi

# 检查 Pod 是否 Running
PROMETHEUS_STATUS=$(kubectl get pod $PROMETHEUS_POD -n monitoring -o jsonpath='{.status.phase}')
ALERTMANAGER_STATUS=$(kubectl get pod $ALERTMANAGER_POD -n monitoring -o jsonpath='{.status.phase}')
GRAFANA_STATUS=$(kubectl get pod $GRAFANA_POD -n monitoring -o jsonpath='{.status.phase}')

echo ""
if [ "$PROMETHEUS_STATUS" == "Running" ]; then
    echo -e "${GREEN}✓ Prometheus Pod 运行正常${NC}"
else
    echo -e "${RED}✗ Prometheus Pod 状态: $PROMETHEUS_STATUS${NC}"
fi

if [ "$ALERTMANAGER_STATUS" == "Running" ]; then
    echo -e "${GREEN}✓ AlertManager Pod 运行正常${NC}"
else
    echo -e "${RED}✗ AlertManager Pod 状态: $ALERTMANAGER_STATUS${NC}"
fi

if [ "$GRAFANA_STATUS" == "Running" ]; then
    echo -e "${GREEN}✓ Grafana Pod 运行正常${NC}"
else
    echo -e "${RED}✗ Grafana Pod 状态: $GRAFANA_STATUS${NC}"
fi

# 检查 Service
echo ""
echo "3. 检查 Service..."
echo ""
kubectl get svc -n monitoring

# 获取访问信息
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
PROMETHEUS_PORT=$(kubectl get svc prometheus -n monitoring -o jsonpath='{.spec.ports[0].nodePort}')
ALERTMANAGER_PORT=$(kubectl get svc alertmanager -n monitoring -o jsonpath='{.spec.ports[0].nodePort}')
GRAFANA_PORT=$(kubectl get svc grafana -n monitoring -o jsonpath='{.spec.ports[0].nodePort}')

# 测试 Prometheus 健康检查
echo ""
echo "4. 测试 Prometheus 健康检查..."
if kubectl exec -n monitoring $PROMETHEUS_POD -- wget -q -O- http://localhost:9090/-/healthy &> /dev/null; then
    echo -e "${GREEN}✓ Prometheus 健康检查通过${NC}"
else
    echo -e "${RED}✗ Prometheus 健康检查失败${NC}"
fi

# 测试 Prometheus 配置
echo ""
echo "5. 测试 Prometheus 配置..."
CONFIG_STATUS=$(kubectl exec -n monitoring $PROMETHEUS_POD -- wget -q -O- http://localhost:9090/api/v1/status/config 2>/dev/null | grep -o '"status":"success"' || echo "")
if [ -n "$CONFIG_STATUS" ]; then
    echo -e "${GREEN}✓ Prometheus 配置加载成功${NC}"
else
    echo -e "${YELLOW}⚠ Prometheus 配置可能有问题${NC}"
fi

# 测试 Prometheus 目标
echo ""
echo "6. 测试 Prometheus 抓取目标..."
TARGETS=$(kubectl exec -n monitoring $PROMETHEUS_POD -- wget -q -O- http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"health":"up"' | wc -l || echo "0")
echo "发现 $TARGETS 个健康的抓取目标"
if [ "$TARGETS" -gt 0 ]; then
    echo -e "${GREEN}✓ Prometheus 正在抓取指标${NC}"
else
    echo -e "${YELLOW}⚠ 没有发现健康的抓取目标，请检查微服务是否已部署${NC}"
fi

# 测试 AlertManager 健康检查
echo ""
echo "7. 测试 AlertManager 健康检查..."
if kubectl exec -n monitoring $ALERTMANAGER_POD -- wget -q -O- http://localhost:9093/-/healthy &> /dev/null; then
    echo -e "${GREEN}✓ AlertManager 健康检查通过${NC}"
else
    echo -e "${RED}✗ AlertManager 健康检查失败${NC}"
fi

# 测试 Grafana 健康检查
echo ""
echo "8. 测试 Grafana 健康检查..."
if kubectl exec -n monitoring $GRAFANA_POD -- wget -q -O- http://localhost:3000/api/health 2>/dev/null | grep -q '"database":"ok"'; then
    echo -e "${GREEN}✓ Grafana 健康检查通过${NC}"
else
    echo -e "${RED}✗ Grafana 健康检查失败${NC}"
fi

# 测试 Grafana 数据源
echo ""
echo "9. 测试 Grafana 数据源..."
DATASOURCE_STATUS=$(kubectl exec -n monitoring $GRAFANA_POD -- wget -q -O- --header="Authorization: Basic YWRtaW46YWRtaW4xMjM=" http://localhost:3000/api/datasources 2>/dev/null | grep -o '"name":"Prometheus"' || echo "")
if [ -n "$DATASOURCE_STATUS" ]; then
    echo -e "${GREEN}✓ Grafana Prometheus 数据源配置成功${NC}"
else
    echo -e "${YELLOW}⚠ Grafana Prometheus 数据源可能未配置${NC}"
fi

# 检查告警规则
echo ""
echo "10. 检查告警规则..."
RULES_COUNT=$(kubectl exec -n monitoring $PROMETHEUS_POD -- wget -q -O- http://localhost:9090/api/v1/rules 2>/dev/null | grep -o '"name":"' | wc -l || echo "0")
echo "发现 $RULES_COUNT 条告警规则"
if [ "$RULES_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ 告警规则加载成功${NC}"
else
    echo -e "${YELLOW}⚠ 没有发现告警规则${NC}"
fi

# 显示访问信息
echo ""
echo "========================================="
echo "验证完成！"
echo "========================================="
echo ""
echo -e "${GREEN}访问信息：${NC}"
echo ""
echo "Prometheus UI: http://${NODE_IP}:${PROMETHEUS_PORT}"
echo "AlertManager UI: http://${NODE_IP}:${ALERTMANAGER_PORT}"
echo "Grafana UI: http://${NODE_IP}:${GRAFANA_PORT}"
echo "  用户名: admin"
echo "  密码: admin123"
echo ""

echo -e "${YELLOW}下一步操作：${NC}"
echo "1. 访问 Grafana 查看预配置的监控面板"
echo "2. 在 Prometheus UI 中查看抓取目标和告警规则"
echo "3. 部署微服务并确保添加了 prometheus.io/scrape 注解"
echo "4. 配置 AlertManager 的实际通知渠道（邮件、钉钉、企业微信）"
echo ""

echo "========================================="
