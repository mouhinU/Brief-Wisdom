---
name: brief-wisdom-security
description: >-
  Brief-Wisdom security checklist: RBAC, Session, CORS, OAuth, input validation,
  XSS, path traversal, secrets, and Docker hardening. Use when implementing
  auth, permissions, file import, API exposure, or reviewing security-sensitive
  changes in this repository.
---

# Brief-Wisdom Security

## Authentication & Session

- **Session key**: always `UserContextHelper.SESSION_USER_KEY` — never hardcode `"AUTH_USER"` or `"SESSION_USER"`
- **Storage**: Spring Session + Redis (`bw:session` namespace)
- **Concurrency**: max 1 session per user (new login kicks old)
- **Password**: BCrypt via `PasswordEncoder` bean
- **OAuth**: WeChat / DingTalk / Alipay — callback must set same session key as password login

## Authorization (Two Layers)

1. **Route level** — `SecurityConfig.authorizeHttpRequests`
2. **Method level** — `@RequiresPermission("module:action")` via `PermissionInterceptor`

| Path pattern | Required |
|--------------|----------|
| `/api/knowledge/**` | `admin` or `super_admin` |
| `/api/user/**`, `/api/menu/**` (manage) | `admin` or `super_admin` |
| `/api/role/**` | `super_admin` |
| `/api/ai/manage/**` | `admin` or `super_admin` |
| `/api/ai/**` (chat) | `permitAll` (guest fingerprint supported) |

`super_admin` bypasses all `@RequiresPermission` checks.

## CORS

- **Single source**: `SecurityConfig.corsConfigurationSource()`
- Config: `app.cors.allowed-origins` (comma-separated)
- Dev default: `http://localhost:8090,http://localhost:3000`
- Production: set to actual domain only
- **Never** add `@CrossOrigin(origins = "*")` on Controllers
- `allowCredentials(true)` — origins must be explicit, not `*`

## CSRF

- CSRF disabled for SPA + Session cookie model
- Mitigation: strict CORS + SameSite cookie policy in production

## Input Validation

- All `@RequestBody` use dedicated Request DTOs — block client setting `id`, `isDeleted`, `createTime`
- SQL: `#{}` parameter binding only; no string concatenation
- Path/query params: validate length, format, allowed enum values
- Bulk APIs: cap list size (≤ 1000 per AGENTS.md)

## Path Traversal

`MarkdownImportService` and any file-path API:

```java
Path base = Paths.get(allowedBaseDir).toAbsolutePath().normalize();
Path target = base.resolve(userInput).normalize();
if (!target.startsWith(base)) {
    throw new BizException(BizExceptionEnums.PARAM_ERROR, "非法路径");
}
```

Never pass user-controlled paths directly to `Paths.get()`.

## XSS

- **Backend**: escape or reject HTML in stored content when rendered server-side
- **Frontend**: all Markdown/HTML rendering via `DOMPurify.sanitize()` (see `chat.js`)
- **SSE errors**: serialize with `ObjectMapper`, never concatenate user/exception text into JSON strings

## Secrets & Config

- API keys: `${DASHSCOPE_API_KEY}`, `${DEEPSEEK_API_KEY}`, `${DB_PASSWORD}`, `${REDIS_PASSWORD}`
- Template: `.env.example` — document all required vars
- Dev profile: `application-dev.yml`; prod: `SPRING_PROFILES_ACTIVE=prod`
- SMS codes: `SecureRandom`; log at most first 2 digits; never return code in API response

## SMS / OAuth Safety

- Rate-limit SMS send endpoints
- OAuth state parameter validation on callback
- Mobile auto-register username: `user_{phone}_{uuid6}` to avoid collisions

## Content & Rate Limits

- `ContentFilterService` on all user→AI input paths
- `RateLimitService` (Redis) before AI calls
- Output PII masking before returning to client

## Docker / Deploy

- Do not expose MySQL/Redis to `0.0.0.0` in production
- Bind to `127.0.0.1` or remove port mappings (internal network only)
- Set container resource limits
- Use strong passwords via env vars, not defaults from compose file

## Security Review Checklist

When reviewing or implementing sensitive code:

```
- [ ] No secrets in source or committed yml
- [ ] New API has SecurityConfig route + @RequiresPermission if needed
- [ ] No @CrossOrigin on Controllers
- [ ] Request DTOs instead of Entity for write APIs
- [ ] File paths normalized and bounded
- [ ] User HTML sanitized before DOM insertion
- [ ] Exception messages not leaked in SSE/JSON concatenation
- [ ] Audit log for privileged actions (login, role change, data delete)
- [ ] Tests cover auth denial (401/403) paths
```

## Known Gaps (fix when touching related code)

- `GlobalExceptionHandler` unknown exceptions return HTTP 200 — prefer 500 for monitoring
- Some frontend `fetch` calls lack unified error handling — align with `apiRequest` pattern
