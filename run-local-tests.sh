#!/bin/bash

# Cuckoo Microservices - 本地测试运行脚本
# 用于在本地环境运行各种测试

set -e

echo "========================================="
echo "Cuckoo Microservices 本地测试"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 测试选项
TEST_TYPE=${1:-all}

# 函数：运行单元测试
run_unit_tests() {
    echo -e "${YELLOW}运行单元测试...${NC}"
    mvn test -Dtest=*Test -DfailIfNoTests=false
    echo -e "${GREEN}✓ 单元测试完成${NC}"
    echo ""
}

# 函数：运行集成测试
run_integration_tests() {
    echo -e "${YELLOW}运行集成测试...${NC}"
    mvn verify -Dtest=*IntegrationTest -DfailIfNoTests=false
    echo -e "${GREEN}✓ 集成测试完成${NC}"
    echo ""
}

# 函数：运行属性测试
run_property_tests() {
    echo -e "${YELLOW}运行属性测试...${NC}"
    mvn test -Dtest=*PropertiesTest -DfailIfNoTests=false
    echo -e "${GREEN}✓ 属性测试完成${NC}"
    echo ""
}

# 函数：编译检查
compile_check() {
    echo -e "${YELLOW}编译检查...${NC}"
    mvn clean compile -DskipTests
    echo -e "${GREEN}✓ 编译成功${NC}"
    echo ""
}

# 函数：运行特定模块测试
run_module_tests() {
    local module=$1
    echo -e "${YELLOW}运行 ${module} 模块测试...${NC}"
    mvn test -pl ${module} -DfailIfNoTests=false
    echo -e "${GREEN}✓ ${module} 测试完成${NC}"
    echo ""
}

# 主逻辑
case $TEST_TYPE in
    compile)
        compile_check
        ;;
    unit)
        run_unit_tests
        ;;
    integration)
        run_integration_tests
        ;;
    property)
        run_property_tests
        ;;
    common)
        run_module_tests "cuckoo-common"
        ;;
    order)
        run_module_tests "cuckoo-order"
        ;;
    all)
        compile_check
        run_unit_tests
        ;;
    *)
        echo -e "${RED}未知的测试类型: $TEST_TYPE${NC}"
        echo ""
        echo "用法: ./run-local-tests.sh [TEST_TYPE]"
        echo ""
        echo "TEST_TYPE 选项:"
        echo "  compile      - 仅编译检查"
        echo "  unit         - 运行单元测试"
        echo "  integration  - 运行集成测试"
        echo "  property     - 运行属性测试"
        echo "  common       - 运行 cuckoo-common 模块测试"
        echo "  order        - 运行 cuckoo-order 模块测试"
        echo "  all          - 运行所有测试 (默认)"
        echo ""
        exit 1
        ;;
esac

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}测试完成！${NC}"
echo -e "${GREEN}=========================================${NC}"
