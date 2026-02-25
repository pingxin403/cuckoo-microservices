# MySQL 主从复制配置

本目录包含 MySQL 主从复制的配置文件和脚本。

## 架构

- **mysql-master**: 主库，端口 3306，负责所有写操作
- **mysql-slave1**: 从库1，端口 3307，负责读操作
- **mysql-slave2**: 从库2，端口 3308，负责读操作

## 快速开始

### 1. 启动 MySQL 主从集群

```bash
cd docker/mysql
docker-compose -f docker-compose-mysql-replication.yml up -d
```

### 2. 验证复制状态

等待所有容器启动后，复制会自动配置。查看日志：

```bash
docker logs mysql-replication-init
```

### 3. 手动验证复制

```bash
# 检查主库状态
docker exec -it cuckoo-mysql-master mysql -uroot -proot -e "SHOW MASTER STATUS\G"

# 检查从库1状态
docker exec -it cuckoo-mysql-slave1 mysql -uroot -proot -e "SHOW SLAVE STATUS\G"

# 检查从库2状态
docker exec -it cuckoo-mysql-slave2 mysql -uroot -proot -e "SHOW SLAVE STATUS\G"
```

### 4. 监控复制延迟

使用监控脚本：

```bash
chmod +x monitor-replication.sh
./monitor-replication.sh
```

或者使用 Docker 容器运行：

```bash
docker run --rm --network mysql_mysql-net \
  -v $(pwd)/monitor-replication.sh:/monitor.sh \
  mysql:8.0 bash /monitor.sh
```

## 配置说明

### 主库配置 (master/my.cnf)

- `server-id=1`: 主库服务器 ID
- `log-bin=mysql-bin`: 启用二进制日志
- `binlog-format=ROW`: 使用行级复制（更安全）
- `expire_logs_days=7`: 二进制日志保留7天

### 从库配置 (slave1/my.cnf, slave2/my.cnf)

- `server-id=2/3`: 从库服务器 ID（必须唯一）
- `relay-log=mysql-relay-bin`: 启用中继日志
- `read_only=1`: 只读模式
- `slave_parallel_workers=4`: 并行复制线程数

## 复制用户

- 用户名: `repl`
- 密码: `repl123`
- 权限: `REPLICATION SLAVE`

## 监控指标

监控脚本会检查以下指标：

1. **Slave_IO_Running**: IO 线程是否运行（应为 Yes）
2. **Slave_SQL_Running**: SQL 线程是否运行（应为 Yes）
3. **Seconds_Behind_Master**: 从库延迟秒数（应 < 5秒）

### 延迟阈值

默认延迟阈值为 5 秒，可通过环境变量修改：

```bash
LAG_THRESHOLD=10 ./monitor-replication.sh
```

## 故障处理

### 从库复制中断

如果从库复制中断，可以尝试重新配置：

```bash
# 进入从库容器
docker exec -it cuckoo-mysql-slave1 mysql -uroot -proot

# 停止复制
STOP SLAVE;

# 重新配置（需要从主库获取最新的 binlog 位置）
CHANGE MASTER TO
  MASTER_HOST='mysql-master',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl123',
  MASTER_LOG_FILE='mysql-bin.000001',  # 从主库获取
  MASTER_LOG_POS=154;                   # 从主库获取

# 启动复制
START SLAVE;

# 检查状态
SHOW SLAVE STATUS\G
```

### 主从数据不一致

如果发现主从数据不一致，可以重新同步：

```bash
# 1. 在主库锁表并导出数据
docker exec cuckoo-mysql-master mysql -uroot -proot -e "FLUSH TABLES WITH READ LOCK;"
docker exec cuckoo-mysql-master mysqldump -uroot -proot --all-databases --master-data=2 > master-backup.sql

# 2. 解锁主库
docker exec cuckoo-mysql-master mysql -uroot -proot -e "UNLOCK TABLES;"

# 3. 在从库导入数据
docker exec -i cuckoo-mysql-slave1 mysql -uroot -proot < master-backup.sql

# 4. 重新配置复制（使用 master-backup.sql 中的 binlog 位置）
```

### 从库延迟过高

如果从库延迟持续过高：

1. 检查从库硬件资源（CPU、内存、磁盘 IO）
2. 增加并行复制线程数：`slave_parallel_workers`
3. 优化慢查询
4. 考虑增加从库数量分散读压力

## 集成到应用

应用需要配置主从数据源：

```yaml
spring:
  datasource:
    master:
      jdbc-url: jdbc:mysql://mysql-master:3306/order_db
      username: root
      password: root
    slave1:
      jdbc-url: jdbc:mysql://mysql-slave1:3306/order_db
      username: root
      password: root
    slave2:
      jdbc-url: jdbc:mysql://mysql-slave2:3306/order_db
      username: root
      password: root
```

详见应用层的读写分离配置。

## 性能优化

### 主库优化

- 调整 `innodb_buffer_pool_size`（建议为物理内存的 70-80%）
- 调整 `max_connections`
- 启用慢查询日志分析

### 从库优化

- 增加 `slave_parallel_workers`
- 调整 `innodb_buffer_pool_size`
- 考虑使用 SSD 提升 IO 性能

## 安全建议

1. 修改默认的 root 密码
2. 修改复制用户密码
3. 限制复制用户的访问 IP
4. 启用 SSL 加密复制连接
5. 定期备份数据

## 参考资料

- [MySQL 8.0 Replication](https://dev.mysql.com/doc/refman/8.0/en/replication.html)
- [MySQL 主从复制最佳实践](https://dev.mysql.com/doc/refman/8.0/en/replication-solutions.html)
