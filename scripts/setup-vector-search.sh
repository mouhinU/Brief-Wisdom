#!/bin/bash

# ============================================
# 知识库向量化检索功能 - 快速启动脚本
# ============================================

echo "=========================================="
echo "  Brief-Wisdom 知识库向量化检索快速启动"
echo "=========================================="
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: 未检测到 Docker,请先安装 Docker"
    exit 1
fi

# 1. 启动 Milvus 向量数据库
echo "📦 步骤 1/4: 启动 Milvus 向量数据库..."
docker ps | grep milvus-standalone > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Milvus 已在运行"
else
    echo "   正在启动 Milvus..."
    docker run -d \
      --name milvus-standalone \
      -p 19530:19530 \
      -p 9091:9091 \
      milvusdb/milvus:v2.4.0 \
      milvus run standalone
    
    if [ $? -eq 0 ]; then
        echo "✅ Milvus 启动成功"
    else
        echo "❌ Milvus 启动失败"
        exit 1
    fi
fi

# 等待 Milvus 就绪
echo "   等待 Milvus 就绪(最多30秒)..."
for i in {1..30}; do
    if docker exec milvus-standalone milvus-proxy health 2>/dev/null | grep -q "healthy"; then
        echo "✅ Milvus 已就绪"
        break
    fi
    sleep 1
done

# 2. 执行数据库迁移
echo ""
echo "🗄️  步骤 2/4: 执行数据库迁移..."
read -p "请输入 MySQL 数据库名称 (默认 brief_wisdom): " DB_NAME
DB_NAME=${DB_NAME:-brief_wisdom}

read -p "请输入 MySQL root 密码: " -s DB_PASSWORD
echo ""

mysql -u root -p"$DB_PASSWORD" "$DB_NAME" < brief-wisdom-web/src/main/resources/migration_add_embedding_field.sql

if [ $? -eq 0 ]; then
    echo "✅ 数据库迁移成功"
else
    echo "⚠️  数据库迁移可能失败,请手动执行 SQL 脚本"
fi

# 3. 配置 API Key
echo ""
echo "🔑 步骤 3/4: 配置 DashScope API Key..."
if [ -z "$DASHSCOPE_API_KEY" ]; then
    read -p "请输入 DashScope API Key: " API_KEY
    export DASHSCOPE_API_KEY="$API_KEY"
    echo "✅ API Key 已设置(当前会话有效)"
    echo "   提示: 建议将 API Key 添加到 ~/.bashrc 或 .env 文件中永久生效"
else
    echo "✅ 使用环境变量中的 API Key"
fi

# 4. 启动应用
echo ""
echo "🚀 步骤 4/4: 启动 Brief-Wisdom 应用..."
echo ""
echo "=========================================="
echo "  环境准备完成!"
echo "=========================================="
echo ""
echo "接下来你可以:"
echo "  1. 运行 mvn spring-boot:run 启动应用"
echo "  2. 访问 http://localhost:8090 进入系统"
echo "  3. 在 AI 助手中测试语义检索功能"
echo ""
echo "测试示例:"
echo "  - 创建一篇关于 'Spring Boot 自动配置' 的文档"
echo "  - 在 AI 助手中提问: 'Spring Boot 是怎么实现自动配置的?'"
echo "  - 观察系统是否通过向量检索找到相关文档"
echo ""
echo "详细文档: docs/guides/VECTOR_SEARCH_GUIDE.md"
echo "=========================================="
