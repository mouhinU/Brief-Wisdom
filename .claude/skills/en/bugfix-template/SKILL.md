# Bug Fix Template Skill

## Purpose

Use this skill when you need to fix a bug in Brief-Wisdom in a reproducible and low-risk way.

## When to Use This Skill

Use it for runtime errors, incorrect behavior, regressions, null issues, permission bugs, API failures, or UI inconsistencies.

## Recommended Workflow

1. Reproduce the bug and identify the exact scenario.
2. Trace the affected module and locate the root cause.
3. Check whether the issue comes from validation, service logic, persistence, permissions, or UI integration.
4. Apply the smallest fix that addresses the root cause.
5. Add or update regression tests if possible.
6. Verify the fix with the relevant test, build, or manual check.

## Core Rules

- Do not patch symptoms without understanding the root cause.
- Prefer focused changes over broad refactors.
- Preserve existing behavior for unaffected paths.
- Add defensive checks where the issue is caused by missing validation or edge cases.
- Document the fix clearly if the bug involved an unusual edge case.

## Trigger Keywords

bug fix, regression, defect, runtime error, null pointer, failing flow

## Priority

High for broken user-facing behavior, permissions, or data integrity issues.

## Default Output Expectations

Provide the root cause, the implementation approach, the files involved, and the verification result.

## Applicable Users

Developers working on defect fixes, regressions, or reliability issues.

## Avoid

Do not introduce speculative changes or unrelated refactors while fixing a bug.

## Example Prompt

Investigate the login failure, identify the root cause, and implement a minimal safe fix with verification.
