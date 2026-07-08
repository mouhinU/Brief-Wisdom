---
name: brief-wisdom-java
description: >-
  Brief-Wisdom Java/Spring Boot backend conventions: module layering, naming,
  MyBatis-Plus, Redis cache, exceptions, and API patterns. Use when editing
  Java files, Controllers, Services, Repositories, DTOs, or Maven modules in
  this repository.
---

# Brief-Wisdom Java Backend

## Authority

Full coding rules live in [AGENTS.md](../../../AGENTS.md). This skill captures **project-specific** patterns the agent must follow.

## Module Layout

```
brief-wisdom-web       → Controller, Config, static resources (entry)
brief-wisdom-ai        → AI services (AiAgentService, RAG, rate limit)
brief-wisdom-system    → Auth, User, Role, Menu, OAuth
brief-wisdom-resume    → Resume display + manage services
brief-wisdom-persistence → Entity, Mapper, Repository
brief-wisdom-common    → DTO, Result, annotations, constants, exceptions
brief-wisdom-api      → External API definitions
brief-wisdom-service  → Legacy shared services (prefer domain modules)
```

**Dependency rule**: upper → lower only. `common` must not depend on business modules.

## Layer Responsibilities

| Layer | Location | Rules |
|-------|----------|-------|
| Controller | `brief-wisdom-web/.../controller/` | Thin: validate input, delegate to Service, return DTO/`Result` |
| Service interface | `{module}/service/` | No `Impl` suffix on interface |
| Service impl | `{module}/service/impl/` | `XxxServiceImpl implements XxxService` |
| Repository | `brief-wisdom-persistence/.../repository/` | Data access; catch → `DAOException`, no logging |
| Entity | `brief-wisdom-persistence/.../model/` | Maps to DB table; `@TableLogic` on `isDeleted` |

## Naming Quick Reference

- Entity/DO: `ChatSession`, `KnowledgeDocument` (table-mapped)
- DTO: `SessionDTO`, `UserDTO` in `brief-wisdom-common`
- Request: `XxxRequest` in `{module}/req/` or `web/req/` — **never expose Entity in `@RequestBody`**
- Boolean field: `deleted` not `isDeleted` in POJO (DB column stays `is_deleted`)
- Methods: `get` / `list` / `count` / `save` / `remove` / `update`

## Controller Checklist

When adding or modifying a Controller:

1. Place in the correct package (`web.controller`, `ai.controller`, `knowledge.controller`, `resume.controller`)
2. Use `@RestController` + `@RequestMapping` + `@RequiredArgsConstructor`
3. Add `@Tag` + `@Operation` for SpringDoc (see `UserController.java`)
4. Add `@RequiresPermission("module:action")` when not covered by `SecurityConfig` route rules
5. Accept **Request DTOs**, return **DTO / PageResult / Result** — not persistence entities
6. Do **not** add `@CrossOrigin` — CORS is centralized in `SecurityConfig`
7. Class Javadoc must include `@author Brief-Wisdom` and `@date YYYY-MM-DD`

## Exception Handling

- Business errors: throw `BizException` or module-specific (`AIException`, `AuthException`, `ResumeException`)
- Use `BizExceptionEnums` codes; do not invent ad-hoc error strings for API contracts
- Service layer: log errors with SLF4J placeholders (`logger.error("Failed x: {}", id, e)`)
- Web layer: let `GlobalExceptionHandler` convert to `Result`

## MyBatis-Plus Patterns

- Query by business key (`sessionId`, `userId`) with `LambdaQueryWrapper`, not `selectById()` on UUID fields
- No `SELECT *` in custom SQL; use `#{}` only, never `${}`
- Batch queries to avoid N+1 (see `AiManageServiceImpl` batch aggregation pattern)
- Update records must refresh `updateTime` (auto-fill via `MetaObjectHandler`)

## Cache & Redis

- Cache keys use prefix `bw:` via `CachePrefix` constants
- Use `@Cacheable` / `@CacheEvict` on read-heavy, infrequently changed data
- Distributed lock: `@DistributedLock` annotation (Redisson), not raw `synchronized`
- Rate limit keys: `bw:ratelimit:*` (see `RateLimitService`)

## New Feature Workflow

```
1. Define Request/DTO in common or web/req
2. Add Service interface + Impl in domain module
3. Add Repository method if needed (persistence module)
4. Add Controller endpoint in web module
5. Register route in SecurityConfig if not public
6. Add @RequiresPermission if finer than role-level
7. Add unit test in src/test/java
8. Annotate with @Operation for Swagger
```

## Anti-Patterns (Do Not)

- Magic numbers/strings — use constants or enums
- `Executors.newFixedThreadPool()` — use `ThreadPoolExecutor` with named threads
- `new BigDecimal(double)` — use `BigDecimal.valueOf()` or string constructor
- Controller returning `ResponseEntity` for normal CRUD (use `ResultAutoWrapperAdvice` + plain return type)
- Hardcoding API keys — use `${ENV_VAR}` in yml

## Verification

After changes:

```bash
./mvnw clean compile -pl brief-wisdom-web -am
./mvnw test -pl brief-wisdom-ai,brief-wisdom-common
```
