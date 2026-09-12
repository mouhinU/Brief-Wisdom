# ============================================
# Brief-Wisdom Docker 镜像（使用预构建 jar）
# ============================================

FROM maven:3.9-eclipse-temurin-21

LABEL maintainer="Brief-Wisdom"
LABEL description="Brief-Wisdom 智能简历与AI助手平台"

WORKDIR /app

# 复制预构建的 jar
COPY brief-wisdom-web/target/brief-wisdom-web-0.0.1-SNAPSHOT.jar app.jar

# 创建日志和数据目录
RUN mkdir -p /app/logs /app/data

# 暴露端口
EXPOSE 8092

# JVM 参数（可通过环境变量 JAVA_OPTS 覆盖），启用虚拟线程
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom -Dspring.threads.virtual.enabled=true"

# 健康检查（使用首页检查避免认证问题）
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-check-certificate -qO- http://localhost:8092/ || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
