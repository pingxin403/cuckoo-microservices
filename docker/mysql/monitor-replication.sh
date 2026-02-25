#!/bin/bash
# MySQL 主从复制监控脚本
# 用于监控主从延迟和复制状态

set -e

# 配置
MASTER_HOST="${MASTER_HOST:-mysql-master}"
SLAVE1_HOST="${SLAVE1_HOST:-mysql-slave1}"
SLAVE2_HOST="${SLAVE2_HOST:-mysql-slave2}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
LAG_THRESHOLD="${LAG_THRESHOLD:-5}"  # 延迟阈值（秒）

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "MySQL 主从复制监控"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="

# 检查主库状态
echo -e "\n${GREEN}[主库状态]${NC}"
mysql -h $MASTER_HOST -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SHOW MASTER STATUS\G" 2>/dev/null || {
  echo -e "${RED}错误: 无法连接到主库${NC}"
  exit 1
}

# 检查从库1状态
echo -e "\n${GREEN}[从库1状态]${NC}"
SLAVE1_STATUS=$(mysql -h $SLAVE1_HOST -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SHOW SLAVE STATUS\G" 2>/dev/null) || {
  echo -e "${RED}错误: 无法连接到从库1${NC}"
  exit 1
}

SLAVE1_IO_RUNNING=$(echo "$SLAVE1_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SLAVE1_SQL_RUNNING=$(echo "$SLAVE1_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')
SLAVE1_LAG=$(echo "$SLAVE1_STATUS" | grep "Seconds_Behind_Master:" | awk '{print $2}')

echo "Slave_IO_Running: $SLAVE1_IO_RUNNING"
echo "Slave_SQL_Running: $SLAVE1_SQL_RUNNING"
echo "Seconds_Behind_Master: $SLAVE1_LAG"

if [ "$SLAVE1_IO_RUNNING" != "Yes" ] || [ "$SLAVE1_SQL_RUNNING" != "Yes" ]; then
  echo -e "${RED}警告: 从库1复制未运行！${NC}"
  echo "$SLAVE1_STATUS" | grep -E "Last_IO_Error|Last_SQL_Error"
fi

if [ "$SLAVE1_LAG" != "NULL" ] && [ "$SLAVE1_LAG" -gt "$LAG_THRESHOLD" ]; then
  echo -e "${YELLOW}警告: 从库1延迟超过阈值 (${SLAVE1_LAG}s > ${LAG_THRESHOLD}s)${NC}"
fi

# 检查从库2状态
echo -e "\n${GREEN}[从库2状态]${NC}"
SLAVE2_STATUS=$(mysql -h $SLAVE2_HOST -u$MYSQL_USER -p$MYSQL_PASSWORD -e "SHOW SLAVE STATUS\G" 2>/dev/null) || {
  echo -e "${RED}错误: 无法连接到从库2${NC}"
  exit 1
}

SLAVE2_IO_RUNNING=$(echo "$SLAVE2_STATUS" | grep "Slave_IO_Running:" | awk '{print $2}')
SLAVE2_SQL_RUNNING=$(echo "$SLAVE2_STATUS" | grep "Slave_SQL_Running:" | awk '{print $2}')
SLAVE2_LAG=$(echo "$SLAVE2_STATUS" | grep "Seconds_Behind_Master:" | awk '{print $2}')

echo "Slave_IO_Running: $SLAVE2_IO_RUNNING"
echo "Slave_SQL_Running: $SLAVE2_SQL_RUNNING"
echo "Seconds_Behind_Master: $SLAVE2_LAG"

if [ "$SLAVE2_IO_RUNNING" != "Yes" ] || [ "$SLAVE2_SQL_RUNNING" != "Yes" ]; then
  echo -e "${RED}警告: 从库2复制未运行！${NC}"
  echo "$SLAVE2_STATUS" | grep -E "Last_IO_Error|Last_SQL_Error"
fi

if [ "$SLAVE2_LAG" != "NULL" ] && [ "$SLAVE2_LAG" -gt "$LAG_THRESHOLD" ]; then
  echo -e "${YELLOW}警告: 从库2延迟超过阈值 (${SLAVE2_LAG}s > ${LAG_THRESHOLD}s)${NC}"
fi

# 总结
echo -e "\n${GREEN}[监控总结]${NC}"
if [ "$SLAVE1_IO_RUNNING" = "Yes" ] && [ "$SLAVE1_SQL_RUNNING" = "Yes" ] && \
   [ "$SLAVE2_IO_RUNNING" = "Yes" ] && [ "$SLAVE2_SQL_RUNNING" = "Yes" ]; then
  echo -e "${GREEN}✓ 所有从库复制正常运行${NC}"
else
  echo -e "${RED}✗ 存在从库复制异常${NC}"
  exit 1
fi

if [ "$SLAVE1_LAG" != "NULL" ] && [ "$SLAVE1_LAG" -le "$LAG_THRESHOLD" ] && \
   [ "$SLAVE2_LAG" != "NULL" ] && [ "$SLAVE2_LAG" -le "$LAG_THRESHOLD" ]; then
  echo -e "${GREEN}✓ 所有从库延迟在正常范围内${NC}"
else
  echo -e "${YELLOW}⚠ 存在从库延迟超过阈值${NC}"
fi

echo "========================================="
