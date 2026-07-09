# Pre-Release Check Template Skill

## Purpose

Use this skill before release or deployment to verify that a change is safe, complete, and ready for production.

## When to Use This Skill

Use it before merging, deploying, or handing off a feature that affects user-facing workflows, permissions, data, or infrastructure.

## Recommended Workflow

1. Review the change scope and identify affected modules.
2. Check that tests, validations, and security boundaries are in place.
3. Confirm configuration changes, environment variables, and migration steps are documented.
4. Verify the main happy path and critical edge cases.
5. Prepare a short release summary and any rollback notes.

## Core Rules

- Do not treat the change as ready until the main verification steps are completed.
- Confirm that sensitive changes have appropriate permissions and auditability.
- Review deployment and rollback impact before release.
- Keep the release checklist concrete and actionable.

## Trigger Keywords

pre-release, release check, deployment readiness, go-live, release validation

## Priority

High for production-facing, permission-sensitive, or data-impacting changes.

## Default Output Expectations

Provide a pre-release assessment, key checks completed, remaining risks, and rollout notes.

## Applicable Users

Developers, maintainers, and release owners preparing a change for deployment.

## Avoid

Do not skip validation for security, data, configuration, or rollback concerns.

## Example Prompt

Perform a pre-release check for this feature and summarize the readiness, risks, and rollout notes.
