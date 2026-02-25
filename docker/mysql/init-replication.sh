#!/bin/bash
# MySQL 主从复制初始化脚本

set -e

echo "等待 MySQL Master 启动..."
until mysql -h mysql-master -uroot -proot -e "SELECT 1" &> /dev/null; do
  echo "MySQL Master 未就绪，等待..."
  sleep 2
done

echo "MySQL Master 已就绪"

# 在主库创建复制用户
echo "在主库创建复制用户..."
mysql -h mysql-master -uroot -proot <<EOF
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl123';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
EOF

# 获取主库状态
echo "获取主库二进制日志位置..."
MASTER_STATUS=$(mysql -h mysql-master -uroot -proot -e "SHOW MASTER STATUS\G")
MASTER_LOG_FILE=$(echo "$MASTER_STATUS" | grep "File:" | awk '{print $2}')
MASTER_LOG_POS=$(echo "$MASTER_STATUS" | grep "Position:" | awk '{print $2}')

echo "Master Log File: $MASTER_LOG_FILE"
echo "Master Log Position: $MASTER_LOG_POS"

# 配置从库1
echo "配置从库1..."
until mysql -h mysql-slave1 -uroot -proot -e "SELECT 1" &> /dev/null; do
  echo "MySQL Slave1 未就绪，等待..."
  sleep 2
done

mysql -h mysql-slave1 -uroot -proot <<EOF
STOP SLAVE;
CHANGE MASTER TO
  MASTER_HOST='mysql-master',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl123',
  MASTER_LOG_FILE='$MASTER_LOG_FILE',
  MASTER_LOG_POS=$MASTER_LOG_POS;
START SLAVE;
EOF

echo "从库1配置完成"

# 配置从库2
echo "配置从库2..."
until mysql -h mysql-slave2 -uroot -proot -e "SELECT 1" &> /dev/null; do
  echo "MySQL Slave2 未就绪，等待..."
  sleep 2
done

mysql -h mysql-slave2 -uroot -proot <<EOF
STOP SLAVE;
CHANGE MASTER TO
  MASTER_HOST='mysql-master',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl123',
  MASTER_LOG_FILE='$MASTER_LOG_FILE',
  MASTER_LOG_POS=$MASTER_LOG_POS;
START SLAVE;
EOF

echo "从库2配置完成"

# 验证复制状态
echo "验证从库1复制状态..."
mysql -h mysql-slave1 -uroot -proot -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master"

echo "验证从库2复制状态..."
mysql -h mysql-slave2 -uroot -proot -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master"

echo "MySQL 主从复制配置完成！"
