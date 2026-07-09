# Operation Checklist Skill

## Purpose

Use this skill when you need a fast, repeatable checklist before implementing or reviewing a change in Brief-Wisdom.

## When to Use This Skill

Use it for task triage, implementation planning, or pre-review validation across backend, frontend, AI, or system modules.

## Core Checklist

1. Read the relevant project guidance, especially AGENTS.md.
2. Identify the affected module and the correct layer for the change.
3. Check whether the task touches security, permissions, data persistence, or external integrations.
4. Reuse existing DTOs, services, and response patterns instead of inventing new ones.
5. Keep changes scoped and avoid unrelated refactors.
6. Add or update tests when behavior changes.
7. Verify the relevant build, test, or runtime check before finishing.

## Trigger Keywords

checklist, implementation plan, validation, review, triage

## Priority

Medium for general planning; high when the task spans multiple modules or touches security.

## Default Output Expectations

Provide a short task summary, impacted modules, implementation plan, and validation steps.

## Applicable Users

Developers, reviewers, and contributors working on any Brief-Wisdom feature.

## Avoid

Do not start implementation without identifying the correct module, layer, and validation path.

## Example Prompt

Review this task, identify the impacted modules, and produce a concise implementation checklist before coding.
