---
name: brief-wisdom-ai
description: >-
  Brief-Wisdom AI module development: Spring AI chat, SSE streaming, RAG
  knowledge retrieval, rate limiting, content filter, model registry, and audit.
  Use when editing brief-wisdom-ai/, AiAgentController, knowledge services,
  or AI-related configuration.
---

# Brief-Wisdom AI Module

## Key Classes

| Class | Role |
|-------|------|
| `AiAgentService` | Core chat, session CRUD, context memory, RAG injection |
| `ChatModelRegistry` | Multi-provider routing (DashScope, DeepSeek, OpenAI-compatible) |
| `KnowledgeRagService` | Retrieve docs for RAG; vector search with keyword fallback |
| `VectorStoreService` | Milvus embedding CRUD |
| `ContentFilterService` | Input keyword block + output PII regex filter |
| `RateLimitService` | Redis sliding window (`bw:ratelimit:*`) |
| `ChatSyncService` / `SseChatSyncService` / `WebSocketChatSyncService` | Multi-device sync |
| `AiAuditService` | Token/cost audit logging |
| `SystemPrompts` | Base system prompt (compliance + safety) |

## Chat Flow

```
User message
  → RateLimitService.isRateLimited(userId)
  → ContentFilterService.checkInput(message)
  → KnowledgeRagService.retrieveRelevantDocuments(message)  [optional RAG]
  → Build Prompt (system + last 10 messages + user message)
  → ChatModelRegistry.getChatModel(modelName)
  → Stream (Flux/SseEmitter) or blocking response
  → ContentFilterService.filterOutput(response)
  → Save ChatMessage (tokens, cost, model)
  → Broadcast sync event (session_created / message_added)
```

## Configuration (`application.yml`)

```yaml
app:
  ai:
    default-provider: dashscope
    providers:
      dashscope:
        type: openai-compatible
        base-url: https://dashscope.aliyuncs.com/compatible-mode
        api-key: ${DASHSCOPE_API_KEY}
  chat:
    streaming: true          # SSE vs blocking
  sync:
    transport: websocket       # websocket | sse
```

- Never hardcode API keys
- New provider: add under `app.ai.providers`, register in `ChatModelRegistry`

## SSE Streaming

- Endpoint: `GET /api/ai/chat/session/{sessionId}/stream`
- Controller uses `SseEmitter`; errors must be **JSON-serialized**, not string-concatenated:
  ```java
  // Good
  objectMapper.writeValueAsString(Map.of("error", safeMessage));
  // Bad — breaks on quotes/newlines in e.getMessage()
  "{\"error\":\"" + e.getMessage() + "\"}"
  ```
- `GlobalExceptionHandler.clearProducibleMediaTypes()` required for SSE error fallback

## RAG / Knowledge Base

- `KnowledgeServiceImpl`: CRUD for bases/documents; triggers async embedding on create/update
- `KnowledgeRagService.retrieveRelevantDocuments()`: vector first, keyword fallback
- Milvus config: `app.vector.*` in yml; optional — degrades gracefully
- Document import: validate `sourceDir` with `Path.normalize()` + allowed base directory (security)

## Context Memory

- Each session keeps **last 10 messages** as AI context
- Page context (`pageContext` on session): AI role switches per page (home, resume, settings)

## Rate Limiting

- Redis-based, distributed-safe
- Limits: ~20 req/sec window, daily cap via multiplier
- Throw `RateLimitException` → HTTP 429 from `GlobalExceptionHandler`

## Content Security

1. **System prompt** — soft guardrails in `SystemPrompts`
2. **Input filter** — keyword deny list, no token spent
3. **Output filter** — mask ID card, phone, bank card patterns

Do not bypass filters for convenience.

## Model Management

- DB table `ai_model`: enable/disable, activate (only one active), pricing per million tokens
- Admin API: `/api/ai/models/**` requires admin role + `@RequiresPermission("ai:manage")`
- Public chat uses `/api/ai/models/enabled` (permitAll)

## Adding an AI Feature

```
1. Service logic in brief-wisdom-ai/service/
2. DTO in brief-wisdom-common/common/ai/
3. Controller in brief-wisdom-web/.../ai/controller/
4. Wire rate limit + content filter if user-facing
5. Record audit log (tokens, cost) for billable calls
6. Add test in brief-wisdom-ai/src/test/java/
7. Update SystemPrompts if behavior/role changes
```

## Testing

Existing tests to follow:

- `ContentFilterServiceTest`
- `RateLimitServiceTest`
- `KnowledgeRagServiceTest`
- `AiManageServiceImplTest`
- `ChatMemoryServiceTest`

Mock external AI calls; do not hit real API in unit tests.

## Anti-Patterns (Do Not)

- Skip rate limit or content filter on new chat endpoints
- Store full API keys in logs
- N+1 queries in manage/statistics endpoints — use batch aggregation
- Return raw Entity from knowledge/chat APIs
