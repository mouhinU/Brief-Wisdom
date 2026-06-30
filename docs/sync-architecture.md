# 实时同步架构设计文档

> 本文档记录 Brief-Wisdom 项目多端实时同步的架构设计，涵盖 SSE 与 WebSocket 双模式的实现机制与对比。

---

## 一、架构概览

系统通过 `ChatSyncService` 接口抽象传输层，`AiAgentService` 仅依赖接口进行事件推送，与具体传输方式完全解耦。配置项 `app.sync.transport` 控制激活哪种实现，默认 `sse`。

```
AiAgentService
      │
      ▼
ChatSyncService (接口)
      │
      ├── SseChatSyncService      ← app.sync.transport=sse (默认)
      │
      └── WebSocketChatSyncService ← app.sync.transport=websocket
```

前端打开聊天窗口时，先调用 `GET /api/ai/sync/transport` 获取传输方式，再自动建立对应连接。

---

## 二、核心机制对比

| 维度 | SSE (`SseChatSyncService`) | WebSocket (`WebSocketChatSyncService`) |
|------|---|---|
| **通信方向** | 单向（服务端 → 客户端） | 双向（服务端 ↔ 客户端） |
| **连接管理结构** | `Map<String, List<SseEmitter>> userEmitters` | `Map<String, List<WebSocketSession>> userSessions` |
| **同一用户多端** | `CopyOnWriteArrayList<SseEmitter>` 支持多设备 | `CopyOnWriteArrayList<WebSocketSession>` 支持多设备 |
| **向指定用户推送** | `notifyUser()` 遍历该用户所有 Emitter | `notifyUser()` 遍历该用户所有 Session |
| **广播给全部用户** | `broadcastToAll()` 遍历所有用户所有 Emitter | `broadcastToAll()` 遍历所有用户所有 Session |
| **设备上线注册** | `createConnection()` 创建 Emitter 并添加到列表 | `registerSession()` 在 `afterConnectionEstablished` 时调用 |
| **设备下线清理** | `onCompletion` / `onTimeout` / `onError` 回调自动移除 | `afterConnectionClosed` 回调自动移除 |
| **线程安全** | `CopyOnWriteArrayList` + `ConcurrentHashMap` | `CopyOnWriteArrayList` + `ConcurrentHashMap` |
| **心跳保活** | 浏览器 `EventSource` 内置自动重连 | 前端 30 秒 ping/pong 心跳 + 断线 3 秒重连 |
| **连接端点** | `GET /api/ai/sync/events` (EventSource) | `ws://host/ws/sync` (WebSocket) |
| **用户身份识别** | HTTP Cookie Session（EventSource 自动携带） | 握手拦截器从 HTTP Session 提取 userId 注入 WebSocket 属性 |

---

## 三、多端同步能力

两种模式对多端同步的支持完全对等：

1. **同一用户多设备同时在线**：数据结构均为 `userId → List<连接>`，手机、电脑、平板可同时连接。
2. **事件推送覆盖所有设备**：`notifyUser(userId, ...)` 遍历该用户全部连接逐一推送，不遗漏。
3. **广播场景**：删除会话时无法确定目标 userId，`broadcastToAll(...)` 向所有在线用户广播。
4. **设备下线自动清理**：连接关闭时通过回调自动从列表移除，无资源泄漏。
5. **断线自动重连**：SSE 依赖浏览器内置机制 + 手动兜底；WebSocket 通过 `onclose` 事件 + 3 秒定时器重连。

---

## 四、事件协议

两种传输方式使用统一的 JSON 事件格式，前端 `handleSyncEvent()` 共用同一处理逻辑：

```json
{
  "type": "session_created | session_deleted | message_added",
  "sessionId": "可选，相关会话ID"
}
```

| 事件类型 | 触发场景 | 前端行为 |
|----------|----------|----------|
| `session_created` | 用户创建新会话 | 刷新会话列表 |
| `session_deleted` | 用户删除会话 | 刷新会话列表；若删除的是当前会话则清空聊天区 |
| `message_added` | 会话中收到新消息 | 刷新会话列表 + 刷新当前会话消息历史 |

---

## 五、关键文件清单

| 文件 | 模块 | 职责 |
|------|------|------|
| `ChatSyncService.java` | ai | 同步服务接口，定义 `notifyUser` / `broadcastToAll` |
| `SseChatSyncService.java` | ai | SSE 实现，管理 SseEmitter 连接池 |
| `WebSocketChatSyncService.java` | ai | WebSocket 实现，管理 WebSocketSession 连接池 |
| `WebSocketSyncHandler.java` | ai | WebSocket 消息处理器，连接/关闭/异常/心跳 |
| `WebSocketSyncHandshakeInterceptor.java` | web | 握手拦截器，提取 userId 注入会话属性 |
| `WebSocketConfig.java` | web | 注册 WebSocket 处理器到 `/ws/sync` 端点 |
| `SseSyncController.java` | web | SSE 专用控制器（仅 SSE 模式激活） |
| `AiAgentController.java` | web | 主控制器，含 `GET /api/ai/sync/transport` |
| `chat.js` (前端) | web/static | 统一 `connectSync()` 入口，自动探测并建立连接 |

---

## 六、配置说明

```yaml
# application.yml
app:
  sync:
    transport: sse        # sse（默认）或 websocket
```

切换后重启服务，前端刷新页面自动适配。
