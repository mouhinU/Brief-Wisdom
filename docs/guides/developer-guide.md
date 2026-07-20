# Brief-Wisdom 开发者指南

> **目标读者**: 新加入项目的开发者
> **最后更新**: 2026-07-15
> **项目版本**: 0.0.1-SNAPSHOT
> **技术栈**: Spring Boot 4.0.7 / Spring AI 2.0.0 / Java 21

---

## 目录

- [环境准备](#环境准备)
- [快速启动](#快速启动)
- [项目架构](#项目架构)
- [开发规范](#开发规范)
- [常见问题](#常见问题)
- [调试技巧](#调试技巧)

---

## 环境准备

### 必需软件

| 软件    | 版本要求 | 说明                        |
|-------|------|---------------------------|
| JDK   | 21+  | 推荐使用 Adoptium Temurin 21  |
| Maven | 3.8+ | 或使用项目自带 `mvnw`            |
| MySQL | 8.0+ | 本地开发建议使用 Docker 容器        |
| Redis | 6.0+ | 推荐启用 RediSearch 模块以支持向量检索 |

### 推荐工具

- **IDE**: IntelliJ IDEA 2024+ (安装 Lombok、MyBatisX 插件)
- **数据库工具**: DataGrip / Navicat / DBeaver
- **Redis 客户端**: Another Redis Desktop Manager
- **API 测试**: Postman / Apifox

### 环境变量配置

复制 `.env.example` 为 `.env`,填写敏感配置:

```bash
cp .env.example .env
```

编辑 `.env`:

```bash
# AI API Key（必填）
AI_API_KEY=your_dashscope_api_key_here
DEEPSEEK_API_KEY=your_deepseek_api_key_here

# OAuth 配置（可选，第三方登录需要）
WECHAT_APP_ID=xxx
WECHAT_APP_SECRET=xxx
DINGTALK_APP_ID=xxx
DINGTALK_APP_SECRET=xxx
ALIPAY_APP_ID=xxx
ALIPAY_PRIVATE_KEY=xxx
```

> **安全提示**: `.env` 文件已在 `.gitignore` 中排除，不会提交到 Git。

---

## 快速启动

### 1. 初始化数据库

```bash
# 方式一：使用 Docker Compose（推荐）
docker-compose up -d mysql redis

# 方式二：手动启动 MySQL 和 Redis
# 确保 MySQL 运行在 localhost:3306，Redis 运行在 localhost:6379
```

执行初始化脚本:

```bash
mysql -u root -p < brief-wisdom-web/src/main/resources/init-20260629.sql
```

> 该脚本会创建数据库、所有表结构、RBAC 权限数据、AI 模型配置和示例数据。

### 2. 启动应用

```bash
# 使用 Maven Wrapper（推荐，无需安装 Maven）
./mvnw spring-boot:run -pl brief-wisdom-web

# 或使用系统 Maven
mvn spring-boot:run -pl brief-wisdom-web
```

### 3. 验证启动

访问以下地址确认服务正常:

- **主页**: http://localhost:8090
- **个人简历**: http://localhost:8090/about.html
- **登录页面**: http://localhost:8090/login.html

**默认账号**:

- 访客用户: `default-user` / `guest`
- 超级管理员: `admin` / `mouhin`

---

## 项目架构

### 模块划分

```
Brief-Wisdom/
├── brief-wisdom-common/          # 公共模块（DTO、常量、注解、i18n）
├── brief-wisdom-persistence/     # 数据持久化（Entity、Mapper、Repository）
├── brief-wisdom-ai/              # AI 领域模块（对话、知识库、模型管理、工具调用）
├── brief-wisdom-system/          # 系统模块（用户、角色、菜单、认证）
├── brief-wisdom-resume/          # 简历模块（CRUD + AI 润色）
├── brief-wisdom-web/             # Web 入口（Controller、配置、前端资源）
├── brief-wisdom-api/             # API 接口定义（预留扩展）
└── brief-wisdom-service/         # 通用服务层（逐步迁移至领域模块）
```

### 依赖关系

```
brief-wisdom-web (Web 入口)
    ├── brief-wisdom-ai (AI 功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-system (系统管理)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-resume (简历功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-persistence (数据访问)
    └── brief-wisdom-common (公共 DTO/常量/注解)
```

### 核心技术栈

| 技术              | 版本     | 用途                                         |
|-----------------|--------|--------------------------------------------|
| Spring Boot     | 4.0.7  | 应用框架                                       |
| Spring AI       | 2.0.0  | AI 集成（DashScope/DeepSeek/OpenAI/Anthropic） |
| MyBatis-Plus    | 3.5.16 | ORM 框架                                     |
| Redis           | 6.0+   | 缓存 + Session 存储 + 向量检索（RediSearch）         |
| Redisson        | 3.40.2 | 分布式锁（独立客户端，避免冲突）                           |
| Spring Security | 7      | 认证授权（RBAC）                                 |
| SpringDoc       | 2.8.6  | API 文档（OpenAPI 3）                          |

详细技术栈请参考 [README.md](../README.md#技术栈)。

---

## 开发规范

### 代码规范

严格遵循 [AGENTS.md](../AGENTS.md) 中的编码规范，核心要点:

1. **命名规范**: 类名 UpperCamelCase，方法名 lowerCamelCase，常量全大写下划线
2. **注释规范**: 所有类必须添加 `@author` 和 `@date`，抽象方法必须有 Javadoc
3. **异常处理**: 禁止空 catch，业务异常使用自定义异常类
4. **日志规范**: 使用 SLF4J，日志输出使用占位符 `{}`
5. **集合处理**: 禁止在 foreach 中 remove/add，集合初始化时指定容量

### Git 提交规范

```bash
# 提交格式
git commit -m "<type>: <subject>"

# type 类型
feat:     新功能
fix:      修复 bug
docs:     文档变更
style:    代码格式（不影响功能）
refactor: 重构
test:     测试相关
chore:    构建过程或辅助工具变动
```

### 分支管理

- **main**: 主分支，保持稳定可发布状态
- **feature/xxx**: 功能分支，从 main 检出
- **hotfix/xxx**: 紧急修复分支，从 main 检出

---

## 常见问题

### Q1: 启动时报 StackOverflowError?

**原因**: Redisson 与 Spring Data Redis 的 `pExpire()` 方法存在递归冲突。

**解决**: 已修复，使用独立 Redisson 客户端（非 starter），详见 [变更日志](../CHANGELOG.md)。

### Q2: 会话创建失败，sessionId 为 null?

**排查步骤**:

1. 清除浏览器缓存（硬刷新 Ctrl+Shift+R）
2. 检查浏览器 Console 是否有 JS 错误
3. 检查 Network 标签，确认请求 URL 包含正确的 sessionId
4. 测试后端 API:
   ```bash
   curl -X POST http://localhost:8090/api/ai/session
   # 应返回: {"success":true,"data":"uuid-here"}
   ```

### Q3: 数据库字段缺失导致 SQL 错误?

**解决**: 执行初始化脚本或对应的迁移脚本。

### Q4: Redis 反序列化失败?

**原因**: 序列化器变更后旧缓存数据不兼容。

**解决**: 清空缓存

```bash
redis-cli FLUSHDB
# 或清空指定缓存域
redis-cli KEYS "bw:*" | xargs redis-cli DEL
```

### Q5: 如何新增一个 AI 提供商?

**步骤**:

1. 在 `application.yml` 中添加提供商配置:
   ```yaml
   app:
     ai:
       providers:
         new-provider:
           type: openai-compatible  # 或 anthropic
           base-url: https://api.xxx.com
           api-key: ${NEW_PROVIDER_API_KEY}
           default-model: xxx-model
   ```

2. 重启应用，新提供商会自动注册到 `ChatModelRegistry`
3. 在 AI 管理后台启用该提供商的模型

### Q6: 知识库向量检索不工作?

**原因**: Redis 未安装 RediSearch 模块。

**解决**: 系统会优雅降级（LazyVectorStore），应用正常运行但向量检索不可用。安装 RediSearch 模块或启用 Redis Stack
即可。详见 [RAG 架构设计](architecture/rag-architecture.md)。

---

## 调试技巧

### 1. 查看 SQL 日志

MyBatis-Plus 已配置 SQL 日志输出，控制台会打印完整 SQL:

```yaml
# application-dev.yml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2. 热部署

DevTools 已启用，修改 Java 代码后自动重启。

> 注意: 修改配置文件或静态资源需手动重启。

### 3. 查看 Redis 缓存

```bash
# 查看所有 bw: 开头的 key
redis-cli KEYS "bw:*"

# 查看具体 key 的值
redis-cli GET "bw:menu:tree::[\"admin\"]"

# 查看 TTL
redis-cli TTL "bw:menu:tree::[\"admin\"]"
```

### 4. API 测试

```bash
# 获取启用的 AI 模型列表
curl http://localhost:8090/api/ai/models/enabled

# 创建会话
curl -X POST http://localhost:8090/api/ai/session \
  -H "Content-Type: application/json" \
  -d '{"title":"测试会话","pageContext":"/about.html"}'

# 发送聊天消息
curl -X POST http://localhost:8090/api/ai/chat/session/{sessionId} \
  -H "Content-Type: application/json" \
  -d '{"content":"你好","model":"qwen-max"}'
```

---

## 下一步

- 阅读 [README.md](../README.md) 了解完整功能列表
- 查看 [RAG 架构设计](architecture/rag-architecture.md) 理解知识库实现
- 查看 [跨会话记忆](architecture/chat-memory.md) 理解记忆功能
- 参考 [数据库表结构](database-schema.md) 了解数据模型
- 参考 [日志配置](LOG_CONFIGURATION.md) 配置日志

---

**祝你开发愉快！**
