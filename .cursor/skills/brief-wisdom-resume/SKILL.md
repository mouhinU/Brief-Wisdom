---
name: brief-wisdom-resume
description: >-
  Brief-Wisdom resume module development: CRUD, AI polish, online editor,
  permission checks, and frontend integration for resume management pages.
  Use when editing resume backend services, DTOs, controllers, or frontend
  resume components under the web static assets.
---

# Brief-Wisdom Resume Module

## Authority

Full coding rules live in [AGENTS.md](../../../AGENTS.md). This skill captures the resume-specific patterns the agent should follow.

## Module Scope

- Backend module: brief-wisdom-resume
- Web entry: resume management pages and related static components
- Core capabilities: resume CRUD, work experience, project experience, achievements, AI polish, and online editing

## Backend Patterns

- Keep controllers thin; validate input and delegate to service logic
- Use dedicated Request/Response DTOs instead of exposing persistence entities in request bodies
- Service interfaces should live in the resume service package; implementations in the matching impl package
- Return standardized Result/PageResult responses for API endpoints
- For AI-assisted polish, route through the resume service layer rather than handling it directly in the controller

## Data Model Rules

- Prefer DTOs for API contracts and UI payloads
- Map between DTOs and domain objects in the service layer
- Avoid returning DB entities directly to the frontend
- Boolean POJO fields should use `deleted` rather than `isDeleted`; DB columns remain `is_deleted`
- For list pages, use dedicated query DTOs and page result wrappers

## AI Polish Workflow

- Validate inputs such as field type, text length, and request context
- Keep the prompt/response handling inside the service layer
- Preserve permission checks for polish/edit operations
- Return user-friendly errors through the global exception handler instead of raw strings

## Frontend Integration

- Resume UI should follow the existing component pattern in the static assets
- For new UI actions, update both the template and logic files together
- Use the unified `apiRequest` pattern and permission-based UI controls
- Sanitize any markdown or HTML content before inserting it into the DOM

## Security & Permission

- Write/update/polish operations should require the relevant resume permission
- Do not rely on frontend-only checks as the real enforcement mechanism
- Validate path, query, and body parameters defensively
- Avoid exposing raw AI content without necessary filtering or validation

## New Feature Checklist

1. Define request/response DTOs
2. Add service interface and implementation
3. Add controller endpoint in the web layer
4. Wire the frontend action and permission handling
5. Add or update tests in the resume module

## Verification

After changes, verify with:

```bash
./mvnw -pl brief-wisdom-resume -am test
./mvnw -pl brief-wisdom-web -am -DskipTests compile
```
