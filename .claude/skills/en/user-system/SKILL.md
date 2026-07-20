---
name: User System Skill
description: Use this skill for user management, menu management, role management, login flows, and account-related system features.
---

# User System Skill

## Purpose

Use this skill for user management, menu management, role management, login flows, and account-related system features.

## When to Use This Skill

Use it for account changes, role/menu updates, login experience changes, or admin-only system features.

## Rules

- Keep user/account logic in the system module and reuse the existing authentication pipeline
- Validate user input and permission boundaries before mutating account or role data
- Prefer existing DTO and response patterns over introducing new ones
- Treat role and menu changes as security-sensitive operations

## Recommended Workflow

1. Review the existing system module flow before editing
2. Reuse the established permission and session patterns
3. Implement the change with explicit validation and auditability
4. Verify role and access behavior after implementation

## Trigger Keywords

user system, role, menu, account, login, session, admin

## Priority

High for role, permission, or login changes; medium for routine account management improvements.

## Default Output Expectations

Describe the affected account or admin workflow, the permission impact, and the verification scenario.

## Applicable Users

Developers working on user, role, menu, or account-management features in the system module.

## Avoid

Do not create account-management flows that bypass existing role or session conventions.

## Example Prompt

Implement a new user account action and ensure it follows the current system-module permission model.
