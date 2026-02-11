# 开发启动脚本

本目录包含用于快速启动和停止所有微服务的脚本。

## 脚本列表

- `start-all.sh` - 启动所有微服务
- `stop-all.sh` - 停止所有微服务

## 使用方法

### 启动所有服务

```bash
cd cuckoo-microservices
./scripts/start-all.sh
```

**功能说明：**

1. 检查 Docker Compose 基础设施状态
   - 如果未运行，自动启动 Docker Compose
   - 等待基础设施就绪（60秒）

2. 按顺序启动所有微服务：
   - cuckoo-user (8081)
   - cuckoo-product (8082)
   - cuckoo-inventory (8084)
   - cuckoo-order (8083)
   - cuckoo-payment (8085)
   - cuckoo-notification (8086)
   - cuckoo-gateway (8080)

3. 每个服务启动后：
   - 日志输出到 `logs/<service-name>.log`
   - 进程 PID 保存到 `logs/<service-name>.pid`
   - 等待 5 秒后启动下一个服务

### 停止所有服务

```bash
cd cuckoo-microservices
./scripts/stop-all.sh
```

**功能说明：**

1. 读取所有 PID 文件
2. 按相反顺序停止所有微服务
3. 尝试优雅停止（SIGTERM）
4. 如果 10 秒后仍未停止，强制停止（SIGKILL）
5. 删除 PID 文件

## 前置条件

### 1. 构建项目

在首次使用启动脚本之前，需要先构建项目：

```bash
cd cuckoo-microservices
mvn clean package -DskipTests
```

### 2. 启动 Docker Compose

脚本会自动检查并启动 Docker Compose，但你也可以手动启动：

```bash
cd cuckoo-microservices
docker compose up -d
```

### 3. 赋予执行权限

```bash
chmod +x scripts/start-all.sh
chmod +x scripts/stop-all.sh
```

## 日志管理

### 查看服务日志

```bash
# 查看特定服务的日志
tail -f logs/cuckoo-user.log

# 查看所有服务的日志
tail -f logs/*.log
```

### 清理日志

```bash
# 删除所有日志文件
rm -f logs/*.log

# 删除所有 PID 文件
rm -f logs/*.pid
```

## 故障排查

### 问题：端口已被占用

**现象：** 启动脚本提示端口已被占用

**解决方法：**

1. 检查端口占用情况：
   ```bash
   lsof -i :8081  # 检查 user-service 端口
   ```

2. 停止占用端口的进程：
   ```bash
   kill <PID>
   ```

3. 或者使用停止脚本：
   ```bash
   ./scripts/stop-all.sh
   ```

### 问题：JAR 文件不存在

**现象：** 启动脚本提示 JAR 文件不存在

**解决方法：**

```bash
cd cuckoo-microservices
mvn clean package -DskipTests
```

### 问题：Docker Compose 未启动

**现象：** 服务启动失败，提示无法连接到 MySQL、Redis 等

**解决方法：**

1. 检查 Docker Compose 状态：
   ```bash
   docker compose ps
   ```

2. 启动 Docker Compose：
   ```bash
   docker compose up -d
   ```

3. 等待所有服务健康检查通过：
   ```bash
   docker compose ps
   ```

### 问题：服务启动失败

**现象：** 服务启动后立即退出

**解决方法：**

1. 查看服务日志：
   ```bash
   tail -f logs/<service-name>.log
   ```

2. 检查配置文件是否正确

3. 确保所有依赖服务（MySQL、Redis、Nacos、Kafka）已启动

## 高级用法

### 只启动特定服务

如果只想启动特定服务，可以手动启动：

```bash
# 启动 user-service
java -jar cuckoo-user/target/cuckoo-user-1.0.0-SNAPSHOT.jar > logs/cuckoo-user.log 2>&1 &
echo $! > logs/cuckoo-user.pid
```

### 修改启动顺序

编辑 `start-all.sh` 文件，修改 `SERVICES` 数组的顺序。

### 修改等待时间

编辑 `start-all.sh` 文件，修改 `sleep 5` 的值（单位：秒）。

## 注意事项

1. **首次启动时间较长**：Docker Compose 需要拉取镜像并启动所有基础设施服务，可能需要几分钟时间。

2. **服务启动顺序**：脚本按照依赖关系启动服务，请勿随意修改启动顺序。

3. **资源占用**：所有服务同时运行会占用较多内存（约 4-6GB），请确保机器有足够资源。

4. **端口冲突**：确保以下端口未被占用：
   - 8080 (gateway)
   - 8081 (user)
   - 8082 (product)
   - 8083 (order)
   - 8084 (inventory)
   - 8085 (payment)
   - 8086 (notification)
   - 3306 (MySQL)
   - 6379 (Redis)
   - 8848 (Nacos)
   - 9092 (Kafka)

5. **日志文件大小**：长时间运行会产生大量日志，建议定期清理。

## 相关文档

- [项目 README](../README.md)
- [Docker Compose 配置](../docker-compose.yml)
- [微服务优化设计文档](../.kiro/specs/microservice-optimization/design.md)
