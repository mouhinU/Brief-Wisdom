---
name: Test Completion Template Skill
description: Use this skill when you need to add or improve tests for a change in Brief-Wisdom.
---

# Test Completion Template Skill

## Purpose

Use this skill when you need to add or improve tests for a change in Brief-Wisdom.

## When to Use This Skill

Use it for missing test coverage, regression tests, service-layer tests, API validation tests, or UI interaction checks.

## Recommended Workflow

1. Identify the behavior that changed and the risk area.
2. Choose the smallest relevant test level: unit, integration, or UI-level.
3. Cover the happy path, key edge cases, and failure behavior.
4. Keep tests independent, deterministic, and aligned with existing test patterns.
5. Run the relevant test suite and confirm the new coverage is meaningful.

## Core Rules

- Prefer targeted tests over broad, brittle ones.
- Test real behavior instead of implementation details.
- Cover the most likely regressions first.
- Keep test data and setup minimal and readable.
- Use existing test utilities and conventions when available.

## Trigger Keywords

test, unit test, integration test, regression test, coverage, test completion

## Priority

Medium for routine changes; high when the change affects core flows or security-sensitive behavior.

## Default Output Expectations

Provide the scope of the tested behavior, the test cases added, and the verification result.

## Applicable Users

Developers adding or improving automated tests for backend, frontend, or integration changes.

## Avoid

Do not add shallow tests that only mirror implementation details or skip the real failure path.

## Example Prompt

Add regression tests for the new resume save flow and verify the key success and error cases.
