# 知识库向量化检索功能使用指南

## 📋 功能概述

本功能为知识库系统增加了**语义检索能力**,通过向量数据库(Milvus)实现基于语义相似度的智能文档检索,大幅提升 AI 助手回答的准确性和相关性。

### 核心特性

- ✅ **语义理解**: 不再依赖关键词匹配,能理解用户查询的真实意图
- ✅ **混合检索**: 优先使用向量检索,失败时自动降级为关键词匹配(向后兼容)
- ✅ **自动向量化**: 创建/更新文档时自动触发向量化,无需手动操作
- ✅ **异常隔离**: 向量化失败不影响文档保存/更新的主流程

---

## 🚀 快速开始

### 1. 安装 Milvus 向量数据库

#### 方式一: Docker Compose (推荐)

```bash
# 在项目根目录执行
docker-compose up -d milvus
```

如果项目中没有 Milvus 的 docker-compose 配置,可以使用官方镜像:

```bash
docker run -d \
  --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  milvusdb/milvus:v2.4.0 \
  milvus run standalone
```

#### 方式二: 本地安装

参考 [Milvus 官方文档](https://milvus.io/docs/install_standalone-docker.md)

### 2. 执行数据库迁移脚本

```bash
# 连接 MySQL 数据库
mysql -u root -p your_database_name < brief-wisdom-web/src/main/resources/migration_add_embedding_field.sql
```

### 3. 配置 API Key

在 `application.properties` 或环境变量中配置 DashScope API Key:

```properties
# 方式一: 直接配置
spring.ai.openai.api-key=sk-your-dashscope-api-key

# 方式二: 使用环境变量(推荐)
DASHSCOPE_API_KEY=sk-your-dashscope-api-key
```

获取 API Key: [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/)

### 4. 启动应用

```bash
mvn spring-boot:run
```

---

## 🔧 配置说明

### Milvus 配置

在 `application-vector.properties` 中配置:

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        host: localhost          # Milvus 服务地址
        port: 19530              # Milvus 服务端口
        collection-name: knowledge_embeddings  # 集合名称
        dimension: 1536          # 向量维度(与 Embedding 模型匹配)
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

## 📖 工作原理

### 1. 文档向量化流程

```
用户创建/更新文档
    ↓
提取纯文本内容(去除HTML标签)
    ↓
调用 Embedding 模型生成向量(1536维)
    ↓
存储到 Milvus 向量数据库
    ↓
关联 documentId 元数据
```

### 2. 语义检索流程

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

### 3. 混合检索策略

```java
// 优先使用向量检索
List<Long> vectorDocIds = vectorStoreService.searchSimilarDocuments(query);
if (!vectorDocIds.isEmpty()) {
    return fetchDocumentsByIds(vectorDocIds);
}

// 降级: 关键词匹配
return retrieveByKeywords(query);
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

系统会:
1. 将问题转换为向量
2. 在 Milvus 中检索相似度最高的文档
3. 找到"Spring Boot 自动配置原理"文档
4. 将文档内容注入到 AI 的系统提示词中
5. AI 基于文档内容回答问题

### 3. 查看日志

```log
INFO  - 文档向量化完成: documentId=1, title=Spring Boot 自动配置原理
INFO  - 使用向量检索找到 1 篇相关文档
INFO  - RAG 检索到 1 篇相关文档（关键词: [Spring Boot, 自动配置]）
```

---

## ⚠️ 注意事项

### 1. Milvus 连接问题

如果启动时报错 `Connection refused`,请检查:
- Milvus 服务是否正常运行: `docker ps | grep milvus`
- 端口是否正确映射: `19530`
- 防火墙是否开放端口

### 2. API Key 配置

确保 DashScope API Key 有效且有余额:
- 访问 [DashScope 控制台](https://dashscope.console.aliyun.com/)
- 检查账户余额和用量限制

### 3. 向量维度不匹配

不同 Embedding 模型的维度不同:
- OpenAI/DashScope `text-embedding-v3`: **1536** 维
- OpenAI `text-embedding-ada-002`: **1536** 维
- 其他模型请参考官方文档

修改 `dimension` 配置以匹配使用的模型。

### 4. 性能优化建议

- **批量向量化**: 对于历史文档,可编写脚本批量向量化
- **异步处理**: 当前为同步调用,生产环境建议使用 `@Async` 异步化
- **缓存机制**: 可考虑缓存热门查询的向量结果

---

## 🔄 批量向量化历史文档

如果需要为已有文档补充向量嵌入,可创建一个临时接口:

```java
@GetMapping("/api/knowledge/embedding/batch")
public void batchEmbedAllDocuments() {
    List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findAll();
    for (KnowledgeDocument doc : allDocs) {
        String content = extractDocumentContent(doc);
        if (content != null && !content.isBlank()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", doc.getTitle());
            metadata.put("tags", doc.getTags());
            vectorStoreService.addDocumentEmbedding(doc.getId(), content, metadata);
        }
    }
}
```

⚠️ **注意**: 此操作可能耗时较长,建议在低峰期执行。

---

## 📊 监控与调试

### 查看向量检索效果

```java
// 在 KnowledgeRagService 中添加日志
log.info("向量检索相似度分数: {}", result.getScore());
```

### 调整相似度阈值

在 `VectorStoreService` 中修改:

```java
private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7; // 默认 0.7
```

- **调高**(如 0.8): 更严格,返回更少但更相关的文档
- **调低**(如 0.5): 更宽松,返回更多但可能不够精准的文档

---

## 🎯 后续优化方向

1. **真正的异步处理**: 使用 `@Async` + 线程池异步向量化
2. **增量更新**: 只向量化变更的内容片段
3. **多语言支持**: 针对不同语言使用专用 Embedding 模型
4. **向量缓存**: 缓存高频查询的向量结果
5. **检索重排序**: 结合 BM25 + 向量相似度进行二次排序

---

## 📞 技术支持

如遇问题,请检查:
1. Milvus 服务状态: `docker logs milvus-standalone`
2. 应用日志: `logs/brief-wisdom-ai.log`
3. 数据库迁移是否成功执行

---

**版本**: v1.0  
**更新日期**: 2026-07-08  
**作者**: Brief-Wisdom Team
