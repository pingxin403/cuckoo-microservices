# 阶段 1: Maven 构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
COPY cuckoo-common/pom.xml cuckoo-common/
RUN mvn dependency:go-offline -B -pl cuckoo-common

# 复制源代码并构建
COPY cuckoo-common/src cuckoo-common/src
COPY cuckoo-gateway/pom.xml cuckoo-gateway/
COPY cuckoo-gateway/src cuckoo-gateway/src
RUN mvn clean package -DskipTests -pl cuckoo-gateway -am

# 阶段 2: JRE 运行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/cuckoo-gateway/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
