# 知识库向量化检索功能 - 实现完成 ✅

## 📋 功能说明

为 Brief-Wisdom 项目实现了基于 **Spring AI Vector Store + Milvus** 的知识库向量化检索功能,实现语义级别的智能文档检索,大幅提升 AI 助手回答的准确性和相关性。

---

## ✨ 核心特性

- ✅ **语义理解**: 不再依赖关键词匹配,能理解用户查询的真实意图
- ✅ **混合检索**: 优先使用向量检索,失败时自动降级为关键词匹配(向后兼容)
- ✅ **自动向量化**: 创建/更新文档时自动触发向量化,无需手动操作
- ✅ **异常隔离**: 向量化失败不影响文档保存/更新的主流程

---

## 📦 交付内容

### 1. 代码文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `brief-wisdom-ai/pom.xml` | 修改 | 添加 Milvus 依赖 |
| `KnowledgeDocument.java` | 修改 | 添加 embedding 字段 |
| `VectorStoreService.java` | 新增 | 向量存储服务 |
| `MilvusVectorStoreConfig.java` | 新增 | Milvus 配置类 |
| `KnowledgeRagService.java` | 修改 | 升级为混合检索 |
| `KnowledgeServiceImpl.java` | 修改 | 集成自动向量化 |

### 2. 配置文件

| 文件 | 说明 |
|------|------|
| `migration_add_embedding_field.sql` | 数据库迁移脚本 |
| `application-vector.properties` | 向量检索配置示例 |

### 3. 文档

| 文件 | 说明 |
|------|------|
| `docs/guides/VECTOR_SEARCH_GUIDE.md` | 详细使用指南 |
| `docs/features/VECTOR_SEARCH_IMPLEMENTATION_SUMMARY.md` | 实现总结文档 |
| `scripts/setup-vector-search.sh` | 快速启动脚本 |

---

## 🚀 快速开始

### 方式一: 使用快速启动脚本(推荐)

```bash
cd /Users/mac/CodeDir/Brief-Wisdom
./scripts/setup-vector-search.sh
```

脚本会自动完成:
1. 启动 Milvus 向量数据库
2. 执行数据库迁移
3. 配置 API Key
4. 提供启动指引

### 方式二: 手动配置

#### 1. 启动 Milvus

```bash
docker run -d \
  --name milvus-standalone \
  -p 19530:19530 \
  milvusdb/milvus:v2.4.0 \
  milvus run standalone
```

#### 2. 执行数据库迁移

```bash
mysql -u root -p your_database < brief-wisdom-web/src/main/resources/migration_add_embedding_field.sql
```

#### 3. 配置 API Key

```bash
export DASHSCOPE_API_KEY=sk-your-api-key
```

或在 `application.properties` 中配置:

```properties
spring.ai.openai.api-key=sk-your-api-key
spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
spring.ai.openai.embedding.options.model=text-embedding-v3
```

#### 4. 启动应用

```bash
mvn spring-boot:run
```

---

## 🧪 测试验证

### 1. 创建测试文档

通过知识库管理界面创建一篇内部文档:

```
标题: Spring Boot 自动配置原理
内容: Spring Boot 通过 @EnableAutoConfiguration 注解实现自动配置...
标签: Spring Boot, 自动配置, Java
```

### 2. 测试语义检索

在 AI 助手中提问:

```
用户: "Spring Boot 是怎么实现自动配置的?"
```

### 3. 查看日志

```log
INFO  - 文档向量化完成: documentId=1, title=Spring Boot 自动配置原理
INFO  - 使用向量检索找到 1 篇相关文档
INFO  - RAG 检索到 1 篇相关文档
```

---

## 📊 工作原理

```
用户提问
    ↓
KnowledgeRagService.retrieveRelevantDocuments()
    ↓
尝试向量检索 (VectorStoreService)
    ├─ 成功 → 返回相关文档ID列表
    └─ 失败 → 降级为关键词匹配
    ↓
根据文档ID查询完整文档信息
    ↓
过滤已发布状态的文档
    ↓
返回给 AI 助手作为上下文
```

---

## ⚙️ 配置说明

### Milvus 配置

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        host: localhost
        port: 19530
        collection-name: knowledge_embeddings
        dimension: 1536  # 与 Embedding 模型维度匹配
```

### Embedding 模型配置

本项目使用阿里云 DashScope 的 `text-embedding-v3` 模型:

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      embedding:
        options:
          model: text-embedding-v3
```

---

## 📖 相关文档

- [详细使用指南](docs/guides/VECTOR_SEARCH_GUIDE.md)
- [实现总结文档](docs/features/VECTOR_SEARCH_IMPLEMENTATION_SUMMARY.md)

---

## 🔧 技术栈

- **Spring AI 1.0.0**: AI 框架
- **Milvus v2.4.0**: 向量数据库
- **DashScope text-embedding-v3**: Embedding 模型
- **MyBatis-Plus**: ORM 框架

---

## ⚠️ 注意事项

1. **Milvus 连接**: 确保 Milvus 服务正常运行且端口 `19530` 可访问
2. **API Key**: 确保 DashScope API Key 有效且有余额
3. **向量维度**: 不同 Embedding 模型维度不同,需调整 `dimension` 配置
4. **性能优化**: 生产环境建议使用 `@Async` 异步化向量化过程

---

## 🎯 后续优化方向

- [ ] 真正的异步处理 (`@Async`)
- [ ] 批量历史文档向量化接口
- [ ] 向量缓存机制
- [ ] 增量更新策略
- [ ] 多语言支持
- [ ] 检索重排序(BM25 + 向量相似度)

---

## 📞 技术支持

如遇问题,请检查:
1. Milvus 服务状态: `docker logs milvus-standalone`
2. 应用日志: `logs/brief-wisdom-ai.log`
3. 数据库迁移是否成功执行

---

**版本**: v1.0  
**实现日期**: 2026-07-08  
**作者**: Brief-Wisdom AI Team
