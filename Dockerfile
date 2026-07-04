# ============================================
# Brief-Wisdom 多阶段构建 Dockerfile
# ============================================

# 阶段1: 构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom 文件，利用 Docker 缓存加速依赖下载
COPY pom.xml .
COPY brief-wisdom-common/pom.xml brief-wisdom-common/
COPY brief-wisdom-persistence/pom.xml brief-wisdom-persistence/
COPY brief-wisdom-service/pom.xml brief-wisdom-service/
COPY brief-wisdom-api/pom.xml brief-wisdom-api/
COPY brief-wisdom-ai/pom.xml brief-wisdom-ai/
COPY brief-wisdom-system/pom.xml brief-wisdom-system/
COPY brief-wisdom-resume/pom.xml brief-wisdom-resume/
COPY brief-wisdom-web/pom.xml brief-wisdom-web/

RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY . .
RUN mvn clean package -DskipTests -B

# 阶段2: 运行
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Brief-Wisdom"
LABEL description="Brief-Wisdom 智能简历与AI助手平台"

# 创建应用用户
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /build/brief-wisdom-web/target/*.jar app.jar

# 创建日志和数据目录
RUN mkdir -p /app/logs /app/data && chown -R app:app /app

USER app

# 暴露端口
EXPOSE 8090

# JVM 参数（可通过环境变量 JAVA_OPTS 覆盖）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
