# Brief-Wisdom

> 基于 Spring Boot 3 + Spring AI 的 AI 智能对话助手，支持多会话管理、历史记录持久化、微信扫码登录等功能。

---

## 目录

- [项目简介](#项目简介)
- [模块架构](#模块架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [数据库配置与初始化](#数据库配置与初始化)
- [数据库表结构](#数据库表结构)
- [MyBatis-Plus 配置](#mybatis-plus-配置)
- [逻辑删除](#逻辑删除)
- [自动填充](#自动填充)
- [AI 智能体使用](#ai-智能体使用)
- [会话管理](#会话管理)
- [API 接口](#api-接口)
- [微信扫码登录](#微信扫码登录)
- [常见问题排查](#常见问题排查)
- [编译与运行](#编译与运行)
- [前端页面](#前端页面)

---

## 项目简介

Brief-Wisdom 是一个 AI 智能对话平台，核心功能包括：

- **多会话管理**：支持创建、切换、删除多个会话
- **历史记录持久化**：所有对话内容保存到 MySQL，支持上下文记忆
- **用户维度隔离**：多用户独立会话空间，默认使用访客用户
- **AI 多模型支持**：支持 Ollama 本地模型、OpenAI、阿里通义千问等
- **微信扫码登录**：通过 OAuth2.0 实现微信扫码登录（可扩展钉钉/QQ/支付宝）
- **个人主页**：集成个人简历展示页面（about.html，可直接访问）

---

## 模块架构

```
Brief-Wisdom/
├── brief-wisdom-persistence/   # 数据持久化模块（Entity + Mapper）
├── brief-wisdom-ai/            # AI 智能模块（Service + 业务逻辑）
├── brief-wisdom-web/           # Web 应用模块（Controller + 前端资源）
├── brief-wisdom-api/           # API 接口定义模块
├── brief-wisdom-service/       # 通用服务模块
├── brief-wisdom-resume/        # 个人简历模块
└── pom.xml                     # 父 POM
```

**依赖关系**：

```
brief-wisdom-web (Web入口)
    ├── brief-wisdom-ai (AI功能)
    │       └── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-persistence (数据访问)
    ├── brief-wisdom-api (API定义)
    └── brief-wisdom-service (通用服务)
```

### 各模块职责

| 模块 | 职责 | 关键类 |
|------|------|--------|
| persistence | 数据存储和访问层 | `ChatUser`, `ChatSession`, `ChatMessage`, `UserOauth`, 各 Mapper |
| ai | AI 服务和业务逻辑 | `AiAgentService` |
| web | Web 入口和前端资源 | `WebApplication`, `AiAgentController`, `WechatAuthController`, `SecurityConfig` |
| api | API 接口定义和 DTO | 通用响应封装 |
| service | 通用服务层 | 业务服务类 |
| resume | 个人简历展示页面 | 简历页面 |

---

## 技术栈

| 组件 | 版本/说明 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.5.7 |
| Spring AI | Spring AI Alibaba |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0+ |
| 安全 | Spring Security 6 |
| 前端 | 原生 HTML/CSS/JS |

---

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.0+
- Maven（或使用项目自带 `mvnw`）

### 2. 初始化数据库

```bash
mysql -u root -p < brief-wisdom-web/src/main/resources/init.sql
```

### 3. 配置 AI 提供商

编辑 `brief-wisdom-web/src/main/resources/application-ai.yml`，配置 AI 模型参数。

### 4. 启动应用

```bash
./mvnw spring-boot:run -pl brief-wisdom-web
```

访问：http://localhost:8090

> 支持 DevTools 热部署，修改 Java 代码后自动重启。

---

## 数据库配置与初始化

### 连接信息

- **主机**：localhost:3306
- **数据库名**：brief_wisdom
- **用户名**：root
- **密码**：123456
- **字符集**：utf8mb4（支持 emoji）

### 初始化方式

**方式一：手动执行初始化脚本（推荐）**

```bash
mysql -u root -p123456 < brief-wisdom-web/src/main/resources/init.sql
```

**方式二：应用启动时自动建表**

MyBatis-Plus 启动后会自动创建表结构（需提前创建数据库）。

### 初始化数据

初始化脚本会自动：
1. 创建数据库 `brief_wisdom`（如果不存在）
2. 创建所有必要的表
3. 插入默认用户：`default-user/guest`（访客）和 `admin/mouhin`

### 数据库备份与恢复

```bash
# 备份
mysqldump -u root -p brief_wisdom > backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p brief_wisdom < backup_20260101.sql
```

---

## 数据库表结构

### 数据关系

```
chat_user (用户)
    ├── chat_session (会话) [一对多]
    │       └── chat_message (消息) [一对多]
    └── user_oauth (第三方登录绑定) [一对多]
```

### chat_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| user_id | VARCHAR(36) | 用户ID (UUID, UNIQUE) |
| username | VARCHAR(100) | 用户名 (UNIQUE) |
| nickname | VARCHAR(200) | 昵称 |
| avatar | VARCHAR(500) | 头像URL |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除: 0-未删除, 1-已删除 |

### chat_session（会话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| session_id | VARCHAR(36) | 会话ID (UUID, UNIQUE) |
| user_id | VARCHAR(36) | 用户ID (外键) |
| title | VARCHAR(200) | 会话标题 |
| description | TEXT | 会话描述 |
| message_count | INT | 消息数量 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除 |

### chat_message（消息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| session_id | VARCHAR(36) | 会话ID (外键) |
| user_id | VARCHAR(36) | 用户ID (外键) |
| role | VARCHAR(20) | 角色 (user/assistant) |
| content | LONGTEXT | 消息内容 |
| model | VARCHAR(500) | AI模型名称 |
| tokens | INT | Token数量 |
| cost | DOUBLE | 费用 |
| timestamp | DATETIME | 消息时间 |
| message_type | VARCHAR(50) | 消息类型 (text/image/code) |
| is_deleted | TINYINT | 逻辑删除 |

### user_oauth（第三方登录绑定表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| user_id | VARCHAR(36) | 关联 chat_user.user_id |
| provider | VARCHAR(32) | 平台标识: wechat/dingtalk/qq/alipay |
| openid | VARCHAR(128) | 平台 OpenID |
| unionid | VARCHAR(128) | 平台 UnionID |
| nickname | VARCHAR(200) | 该平台显示昵称 |
| avatar | VARCHAR(500) | 该平台头像URL |
| create_time | DATETIME | 绑定时间 |
| update_time | DATETIME | 更新时间 |
| is_deleted | TINYINT | 逻辑删除 |

**唯一约束**：`UNIQUE KEY uk_provider_openid (provider, openid)`

### 外键约束

- `chat_session.user_id` → `chat_user.user_id` (CASCADE DELETE)
- `chat_message.session_id` → `chat_session.session_id` (CASCADE DELETE)
- `chat_message.user_id` → `chat_user.user_id` (CASCADE DELETE)
- `user_oauth.user_id` → `chat_user.user_id` (CASCADE DELETE)

---

## MyBatis-Plus 配置

### application.yml

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.mouhin.brief.wisdom.persistence.model
  configuration:
    map-underscore-to-camel-case: true   # 驼峰转下划线
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL日志
  global-config:
    db-config:
      id-type: assign_uuid               # 全局默认策略（实际由实体 @TableId 覆盖）
      logic-delete-field: is_deleted     # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 主键策略

所有表均使用 `@TableId(type = IdType.AUTO)` 自增主键，`user_id` / `session_id` 为业务唯一键（UNIQUE）。

| 表 | 主键 | 业务键 |
|----|------|--------|
| chat_user | `id` BIGINT AUTO_INCREMENT | `user_id` VARCHAR(36) UNIQUE |
| chat_session | `id` BIGINT AUTO_INCREMENT | `session_id` VARCHAR(36) UNIQUE |
| chat_message | `id` BIGINT AUTO_INCREMENT | - |
| user_oauth | `id` BIGINT AUTO_INCREMENT | - |

> 按业务键查询时需使用 `LambdaQueryWrapper`，而非 `selectById()`。

### Mapper 接口

```java
// 自定义查询示例（需手动添加 is_deleted = 0 条件）
@Select("SELECT * FROM chat_session WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY update_time DESC")
List<ChatSession> selectByUserIdOrderByUpdateTimeDesc(String userId);
```

### 分页插件

已配置 MyBatis-Plus 分页插件（最大单页 1000 条），并通过 `PaginationProperties` 实现按业务维度配置分页参数：

```yaml
app:
  pagination:
    session-list:
      default-size: 20    # 会话列表每页条数
      max-size: 100
    message-history:
      default-size: 50    # 消息历史每页条数
      max-size: 200
```

前端可通过 `GET /api/ai/config/pagination` 获取分页配置，避免硬编码。

---

## 逻辑删除

所有表都包含 `is_deleted` 字段，实现软删除。

### 实体类配置

```java
@TableLogic
@TableField(value = "is_deleted")
private Integer isDeleted;  // 0-未删除, 1-已删除
```

### 工作原理

| 操作 | 实际 SQL |
|------|---------|
| 删除 | `UPDATE table SET is_deleted = 1 WHERE id = ?` |
| 查询 | `SELECT * FROM table WHERE is_deleted = 0` |

### 注意事项

- MyBatis-Plus 内置方法（`selectById`, `deleteById`）自动处理逻辑删除
- 自定义 `@Select` 需手动添加 `AND is_deleted = 0` 条件
- 逻辑删除后数据仍可恢复：`UPDATE table SET is_deleted = 0 WHERE ...`

---

## 自动填充

通过 `MetaObjectHandler` 实现时间字段自动填充。

### 配置（MybatisPlusConfig.java）

```java
@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "timestamp", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 字段映射

| Java 字段 | 数据库字段 | 填充时机 |
|-----------|-----------|---------|
| createTime | create_time | INSERT |
| updateTime | update_time | INSERT + UPDATE |
| timestamp | timestamp | INSERT（消息表） |
| isDeleted | is_deleted | INSERT（默认 0） |

> `strictInsertFill` 只在字段值为 null 时填充，不会覆盖手动设置的值。

---

## AI 智能体使用

### 当前 AI 提供商

项目当前使用 **阿里通义千问（DashScope）**，配置文件 `application-ai.yml`：

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
      chat:
        options:
          model: qwen-max   # 可选 qwen-plus, qwen-turbo
```

### 扩展其他 AI 提供商

项目基于 Spring AI，可切换为其他提供商：

**Ollama（本地模型）**

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama2
```

**OpenAI**

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      chat:
        options:
          model: gpt-3.5-turbo
```

### API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/chat` | POST | 简单聊天 |
| `/api/ai/chat-with-prompt` | POST | 带系统提示的聊天 |
| `/api/ai/ask` | POST | 智能问答 |
| `/api/ai/chat/session/{sessionId}` | POST | 带上下文的会话聊天 |

### 测试示例

```bash
# 简单聊天
curl -X POST http://localhost:8090/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 带上下文聊天
curl -X POST http://localhost:8090/api/ai/chat/session/YOUR_SESSION_ID \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

---

## 会话管理

### 会话生命周期

```
用户打开页面
    ↓
加载分页配置（/api/ai/config/pagination）
    ↓
分页加载会话列表（支持无限滚动）
    ↓
自动选中第一个会话 / 创建新会话
    ↓
发送消息 → 保存到 chat_message
    ↓
调用 AI 获取回复 → 保存到 chat_message
    ↓
更新会话统计（message_count, update_time, title）
    ↓
重新加载会话列表（更新标题和时间排序）
```

### 上下文记忆

每个会话保留最近 **10 条消息** 作为 AI 上下文，保证对话连贯性。

### 会话列表分页（无限滚动）

- 会话列表支持分页加载，前端通过滚动触底自动加载下一页
- 分页参数由后端 `PaginationProperties` 配置，前端动态获取
- 会话按 `update_time` 倒序排列（即最后一条消息时间）
- 切换会话时清除旧聊天内容，加载新会话历史
- 删除会话时自动切换到第一个会话

### 前端关键函数

| 函数 | 说明 |
|------|------|
| `loadPaginationConfig()` | 加载分页配置 |
| `loadSessions()` | 加载会话列表（第一页） |
| `loadMoreSessions()` | 加载更多会话（下一页，无限滚动） |
| `createNewSession()` | 创建新会话 |
| `selectSession(sessionId)` | 切换会话 |
| `sendMessage()` | 发送消息 |
| `loadSessionHistory(sessionId)` | 加载历史消息 |

---

## API 接口

### 会话管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/session` | POST | 创建新会话 |
| `/api/ai/session/{sessionId}` | DELETE | 删除会话（逻辑删除） |
| `/api/ai/sessions?page=1&size=20` | GET | 分页获取会话列表（返回 `records/total/hasMore` 等） |
| `/api/ai/session/{sessionId}/history` | GET | 获取会话历史消息 |
| `/api/ai/config/pagination` | GET | 获取分页配置（sessionList/messageHistory） |

### 认证

| 接口 | 方法 | 说明 |
|------|------|------|
| `/auth/wechat/login` | GET | 获取微信扫码登录 URL |
| `/auth/wechat/callback` | GET | 微信授权回调 |
| `/api/auth/status` | GET | 检查登录状态 |
| `/api/auth/user` | GET | 获取当前用户信息（需登录） |
| `/auth/logout` | POST | 退出登录 |

---

## 微信扫码登录

### 架构设计

采用 `user_oauth` 关联表存储第三方登录信息，支持多平台扩展：

| provider | 说明 |
|----------|------|
| wechat | 微信 |
| dingtalk | 钉钉（预留） |
| qq | QQ（预留） |
| alipay | 支付宝（预留） |

### 登录流程

```
用户点击登录 → 跳转微信授权页 → 用户扫码授权
    ↓
微信回调 /auth/wechat/callback（携带 code）
    ↓
后端用 code 换取 access_token
    ↓
用 access_token 获取用户信息（openid/nickname/avatar）
    ↓
查询 user_oauth 表是否已有绑定关系
    ├── 已绑定：直接登录，更新 OAuth 信息
    └── 未绑定：创建新用户 + 创建 OAuth 绑定记录
    ↓
写入 SecurityContext 到 Session → 重定向到目标页面
```

### 配置

```yaml
wechat:
  open:
    app-id: ${WECHAT_APP_ID:your-app-id}
    app-secret: ${WECHAT_APP_SECRET:your-app-secret}
    redirect-uri: ${WECHAT_REDIRECT_URI:http://localhost:8090/auth/wechat/callback}
```

### 安全配置

`SecurityConfig` 配置了 Spring Security 路由权限：
- 静态资源、AI API、认证 API 全部公开访问
- `/api/auth/user` 需要登录才能获取用户信息
- 禁用 CSRF、formLogin、httpBasic
- 同一用户最多 1 个并发 Session，新登录踢掉旧会话
- 未认证时返回 `401 JSON` 响应

---

## 常见问题排查

### 问题 1：sessionId 为 null

**症状**：后端日志显示 `sessionId: null`，消息发送失败

**排查步骤**：

1. **清除浏览器缓存**：硬刷新（Ctrl+Shift+R）或使用无痕模式
2. **检查前端 Console**：确认 `currentSessionId` 有值
3. **检查 Network 标签**：确认请求 URL 包含正确的 sessionId
4. **测试后端 API**：
   ```bash
   # 创建会话
   curl -X POST http://localhost:8090/api/ai/session
   # 应返回: {"success":true,"data":"uuid-here"}
   ```

### 问题 2：create_time cannot be null

**原因**：`MetaObjectHandler` 未正确配置

**解决**：确保 `MybatisPlusConfig` 实现了 `MetaObjectHandler` 接口，并在 `insertFill()` 方法中填充时间字段。

### 问题 3：会话删除后无法使用

**原因**：会话被逻辑删除（`is_deleted = 1`）

**解决**：
```sql
-- 查看会话状态
SELECT * FROM chat_session WHERE session_id = 'xxx';

-- 恢复会话
UPDATE chat_session SET is_deleted = 0 WHERE session_id = 'xxx';
```

### 问题 4：主键改造后查询失败

**原因**：`chat_user`/`chat_session` 改为 `id` 自增主键后，`selectById()` 查询的是 `id` 字段而非业务键

**解决**：按业务键查询需改用 `LambdaQueryWrapper`：
```java
LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
qw.eq(ChatSession::getSessionId, sessionId);
ChatSession session = sessionMapper.selectOne(qw);
```

---

## 编译与运行

```bash
# 编译整个项目
./mvnw clean compile

# 运行应用
./mvnw spring-boot:run -pl brief-wisdom-web

# 打包
./mvnw clean package
```

### 开发模式

- **热部署**：已启用 DevTools 热重启，修改 Java 代码后自动重启
- **LiveReload**：已启用 LiveReload（端口 35729），前端修改后浏览器自动刷新
- **SQL 日志**：控制台输出完整 SQL 日志，便于调试

### 验证清单

- [ ] 数据库 `brief_wisdom` 已创建，表结构完整
- [ ] 默认用户（`default-user/guest` 和 `admin/mouhin`）已插入
- [ ] 应用启动无报错
- [ ] 访问 http://localhost:8090 页面正常显示
- [ ] 创建会话、发送消息、切换会话功能正常
- [ ] 会话列表无限滚动加载正常
- [ ] AI 回复正常返回（Markdown 渲染）
- [ ] 访问 http://localhost:8090/about.html 个人简历页面正常

---

## 前端页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 主页 | `/` / `index.html` | AI 聊天助手入口，右下角悬浮按钮打开聊天窗口 |
| 个人简历 | `/about.html` | 个人简历展示页，支持深色/浅色主题切换、PDF 导出 |
| 测试页 | `/test-session.html` | 会话 API 独立测试页面 |

---

## 扩展方向

| 方向 | 说明 |
|------|------|
| 会话标签/分类 | 为会话添加标签，便于管理 |
| 消息搜索 | 全文搜索历史消息 |
| 用户注册登录 | 完整的用户认证体系 |
| 多端同步 | WebSocket 实时推送 |
| AI 模型切换 | 支持多模型对比 |
| 费用统计 | Token 用量和费用报表 |
| 第三方登录扩展 | 钉钉/QQ/支付宝登录 |

