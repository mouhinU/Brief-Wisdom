---
name: System Development Skill
description: Use this skill for system-level development work in Brief-Wisdom, including backend modules, authentication, permissions, shared services, and deployment-related changes.
---

# System Development Skill

## Purpose

Use this skill for system-level development work in Brief-Wisdom, including backend modules, authentication,
permissions, shared services, and deployment-related changes.

## When to Use This Skill

Use this skill for cross-module features, shared service changes, access-control updates, or deployment-related tasks.

## Core Rules

- Read AGENTS.md first and inspect the relevant module structure
- Prefer small, focused changes over broad rewrites
- Keep controllers thin and delegate to the service layer
- Use DTOs for API contracts and avoid exposing persistence entities directly
- Follow the module boundaries: web -> service/api -> persistence -> common
- Avoid magic values and hardcoded environment-specific settings

## Recommended Workflow

1. Identify the affected module and related layers
2. Reuse existing services, DTOs, exceptions, and response patterns
3. Implement the change in the correct layer with minimal impact
4. Add or update relevant tests
5. Verify with the appropriate build or test command

## Trigger Keywords

system development, architecture change, module refactor, cross-module feature, deployment update

## Priority

High for security, permission, shared services, or deployment-related changes; medium for routine feature work.

## Default Output Expectations

Summarize the change plan, list the affected modules, and mention the verification command.

## Applicable Users

Developers working on backend, system, AI, or deployment tasks.

## Avoid

Do not bypass existing security checks, introduce duplicated patterns, or make repo-wide changes without checking the
relevant module first.

## Example Prompt

Implement a new permission-controlled API for the user system and follow the Brief-Wisdom backend conventions.
