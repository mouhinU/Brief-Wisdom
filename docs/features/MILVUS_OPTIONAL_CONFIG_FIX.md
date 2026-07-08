# Milvus 向量数据库可选配置修复说明

## 🔧 问题描述

之前实现的知识库向量化检索功能在启动时会强制连接 Milvus 向量数据库,如果 Milvus 服务未运行,会导致应用启动失败。

**错误信息**:
```
Failed to initialize connection. Error: DEADLINE_EXCEEDED: deadline exceeded after 9.970444089s
```

**根本原因**: Spring AI 自带的 `MilvusVectorStoreAutoConfiguration` 自动配置类会无条件尝试创建 Milvus 客户端,即使我们自定义的配置类已经设为条件化加载。

## ✅ 解决方案

将 Milvus 向量数据库配置改为**可选启用**,默认禁用,避免影响正常业务运行。

### 核心改动

#### 1. MilvusVectorStoreConfig - 条件化配置

添加 `@ConditionalOnProperty` 注解,只在明确启用时才创建 Bean:

```java
@ConditionalOnProperty(name = "spring.ai.vectorstore.milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusVectorStoreConfig {
    // ...
}
```

#### 2. VectorStoreService - 条件化创建

添加 `@ConditionalOnBean(VectorStore.class)` 注解,只在 VectorStore Bean 存在时才创建:

```java
@Service
@ConditionalOnBean(VectorStore.class)
public class VectorStoreService {
    // ...
}
```

#### 3. KnowledgeRagService - Optional 注入

使用 `Optional<VectorStoreService>` 代替直接注入,支持降级方案:

```java
private final Optional<VectorStoreService> vectorStoreServiceOptional;

// 使用时检查是否可用
if (vectorStoreServiceOptional.isPresent()) {
    // 使用向量检索
} else {
    // 降级为关键词匹配
}
```

#### 4. KnowledgeServiceImpl - Optional 注入

同样使用 `Optional` 注入,在向量化时先检查服务是否可用:

```java
private final Optional<VectorStoreService> vectorStoreServiceOptional;

private void triggerDocumentEmbeddingAsync(KnowledgeDocument doc) {
    if (!vectorStoreServiceOptional.isPresent()) {
        log.debug("VectorStoreService 未启用，跳过向量化");
        return;
    }
    // ...
}
```

#### 5. application.yml - 排除自动配置 + 默认禁用

**关键修复**: 排除 Spring AI 的 Milvus 自动配置类:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration
```

然后添加自定义 Milvus 配置,默认 `enabled: false`:

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        enabled: false  # 默认禁用
        host: localhost
        port: 19530
        collection-name: knowledge_embeddings
        dimension: 1536
```

## 📋 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `MilvusVectorStoreConfig.java` | 添加 `@ConditionalOnProperty`,异常处理优化 |
| `VectorStoreService.java` | 添加 `@ConditionalOnBean`,初始化检测,可用性标记 |
| `KnowledgeRagService.java` | 改用 `Optional` 注入,条件调用 |
| `KnowledgeServiceImpl.java` | 改用 `Optional` 注入,条件调用 |
| `application.yml` | 添加 Milvus 配置(默认禁用) |

## 🚀 如何使用

### 方式一: 不使用向量检索(默认)

无需任何操作,应用会正常启动并使用关键词匹配作为 RAG 检索方案。

### 方式二: 启用向量检索

#### 1. 启动 Milvus 服务

```bash
docker run -d \
  --name milvus-standalone \
  -p 19530:19530 \
  milvusdb/milvus:v2.4.0 \
  milvus run standalone
```

#### 2. 修改配置文件

在 `application.yml` 或 `application-dev.yml` 中设置:

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        enabled: true  # 启用 Milvus
```

或在环境变量中设置:

```bash
export SPRING_AI_VECTORSTORE_MILVUS_ENABLED=true
```

#### 3. 重启应用

```bash
mvn spring-boot:run
```

应用启动时会尝试连接 Milvus:
- **成功**: 日志输出 `Milvus 向量数据库连接成功，向量化检索功能已启用`
- **失败**: 日志输出 `Milvus 向量数据库不可用，将使用关键词匹配降级方案`,但应用仍可正常运行

## ✨ 优势

1. **向后兼容**: 不影响现有功能,没有 Milvus 也能正常使用
2. **渐进式增强**: 需要时可以随时启用向量检索
3. **故障容错**: Milvus 连接失败不会阻止应用启动
4. **灵活配置**: 通过配置文件轻松开关功能

## 📊 工作流程

```
应用启动
    ↓
检查 spring.ai.vectorstore.milvus.enabled
    ├─ false → 不创建 Milvus 相关 Bean
    │           ↓
    │        使用关键词匹配(RAG降级方案)
    │
    └─ true  → 尝试创建 MilvusServiceClient
                ├─ 成功 → 启用向量检索
                └─ 失败 → 降级为关键词匹配
```

## ⚠️ 注意事项

1. **首次启用**: 需要先启动 Milvus 服务,否则会自动降级
2. **历史文档**: 启用后,新创建的文档会自动向量化,历史文档需要手动触发批量向量化
3. **性能影响**: 向量化会增加文档创建/更新的耗时(异步执行,影响较小)

## 🔍 验证方法

### 检查 Milvus 是否启用

查看应用启动日志:

```log
# 成功启用
INFO  - Milvus 向量数据库连接成功，向量化检索功能已启用

# 自动降级
WARN  - Milvus 向量数据库不可用，将使用关键词匹配降级方案
```

### 测试向量检索

1. 创建一篇知识库文档
2. 在 AI 助手中提问相关内容
3. 查看日志是否出现 `使用向量检索找到 X 篇相关文档`

---

**修复日期**: 2026-07-08  
**修复者**: Brief-Wisdom AI Team  
**版本**: v1.1
