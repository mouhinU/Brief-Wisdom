---
name: Code Review Template Skill
description: Use this skill when you need to review a change in Brief-Wisdom for correctness, maintainability, security, and consistency.
---

# Code Review Template Skill

## Purpose

Use this skill when you need to review a change in Brief-Wisdom for correctness, maintainability, security, and
consistency.

## When to Use This Skill

Use it for PR review, change inspection, architecture validation, or pre-merge quality checks.

## Recommended Review Workflow

1. Read the change scope and understand the intended behavior.
2. Check whether the implementation matches the project architecture and module boundaries.
3. Review correctness, edge cases, error handling, and validation.
4. Look for security issues, permission gaps, or sensitive-data handling problems.
5. Evaluate test coverage and whether the change is sufficiently verified.
6. Summarize findings with clear severity and actionable suggestions.

## Review Focus Areas

- Correctness and logic flow
- Naming, structure, and readability
- Reuse of existing patterns and services
- Security and permission boundaries
- Test coverage and regression risk
- Performance and maintainability

## Trigger Keywords

code review, PR review, review checklist, quality check, change inspection

## Priority

High for changes affecting security, data integrity, shared services, or core flows.

## Default Output Expectations

Provide a concise review summary, list findings by severity, and include suggested fixes or follow-up actions.

## Applicable Users

Developers, maintainers, and reviewers working on PRs or code changes.

## Avoid

Do not approve changes that ignore security boundaries, lack validation, or introduce unnecessary complexity.

## Example Prompt

Review this patch for correctness, architecture alignment, and any security or regression risks.
