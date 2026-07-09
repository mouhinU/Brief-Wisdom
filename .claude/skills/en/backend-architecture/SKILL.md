# Backend Architecture Skill

## Purpose

Use this skill for core backend changes such as APIs, services, repositories, DTOs, and module boundary updates.

## When to Use This Skill

Use it for new API endpoints, shared service changes, repository logic updates, or cross-layer backend refactors.

## Core Rules

- Follow the module direction: web -> service/api -> persistence -> common
- Keep controllers thin and delegate business flow to services
- Use service interfaces and implementation classes in the matching impl package
- Prefer DTOs over exposing persistence entities directly
- Reuse existing exception, response, and cache patterns

## Recommended Workflow

1. Identify the target module and affected layers
2. Reuse existing interfaces, DTOs, and response patterns
3. Implement the change in the correct layer
4. Add or update tests for the impacted flow
5. Verify the relevant module build or tests

## Trigger Keywords

backend architecture, service layer, repository change, DTO update, module boundary

## Priority

High for shared infrastructure or cross-module changes; medium for feature-specific backend work.

## Default Output Expectations

Explain the affected layer, impacted module, and the verification step.

## Applicable Users

Backend developers and maintainers.

## Avoid

Do not introduce a new pattern when an existing service or DTO already fits the use case.

## Example Prompt

Refactor this backend flow to follow the Brief-Wisdom service-layer architecture and keep the module boundaries intact.
