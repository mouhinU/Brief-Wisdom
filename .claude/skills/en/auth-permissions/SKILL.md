---
name: Authentication and Permissions Skill
description: Use this skill for login, role management, permission checks, OAuth integration, session handling, and security-sensitive endpoints.
---
# Authentication and Permissions Skill

## Purpose

Use this skill for login, role management, permission checks, OAuth integration, session handling, and security-sensitive endpoints.

## When to Use This Skill

Use it when changing login behavior, permissions, roles, menu access, OAuth flows, or any endpoint that affects security boundaries.

## Rules

- Respect the existing security configuration and route-level authorization rules
- Prefer permission-based checks over ad-hoc access logic
- Do not bypass authentication or authorization for convenience
- Reuse the existing session and user-context patterns
- Keep sensitive operations restricted to admin or super-admin paths as appropriate

## Recommended Workflow

1. Review the current security configuration and affected routes
2. Decide whether the endpoint is public, authenticated, or admin-only
3. Implement the access-control change with least-privilege behavior
4. Validate the backend enforcement and UI-facing behavior
5. Re-test permission-sensitive scenarios

## Trigger Keywords

auth, permission, role, login, OAuth, session, security

## Priority

Highest for any change affecting access control or security-sensitive behavior.

## Default Output Expectations

Describe the access scope, impacted roles or endpoints, and the security verification needed.

## Applicable Users

Developers working on authentication, authorization, roles, menu access, OAuth, or session handling.

## Avoid

Do not bypass auth checks or weaken least-privilege rules for convenience.

## Example Prompt

Add a new admin-only API and ensure the permission enforcement follows the existing Brief-Wisdom security pattern.
