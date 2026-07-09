# DevOps and Deployment Skill

## Purpose

Use this skill for Docker, Maven build, environment configuration, deployment behavior, and runtime-related changes.

## When to Use This Skill

Use it for Docker config changes, Maven build updates, environment variables, deployment steps, or runtime setup adjustments.

## Rules

- Keep configuration externalized through environment variables or profile-based configuration
- Avoid hardcoded secrets, local paths, or environment-specific values in source files
- Preserve compatibility with the existing Docker and compose setup
- Prefer incremental and reversible deployment changes

## Recommended Workflow

1. Review whether a new environment variable or profile is needed
2. Keep the change backward compatible and environment-safe
3. Update Docker or runtime configuration only when necessary
4. Verify build, startup, and basic runtime behavior

## Trigger Keywords

deployment, Docker, compose, Maven, environment variable, runtime config

## Priority

High for deployment-affecting changes; medium for local configuration consistency updates.

## Default Output Expectations

Describe the changed environment or deployment surface, any config impact, and the verification command or startup check.

## Applicable Users

Developers and operators working on deployment, Docker, environment configuration, or build behavior.

## Avoid

Do not hardcode runtime values or break compatibility with the existing container or Maven workflow.

## Example Prompt

Update the deployment configuration so it remains compatible with the current Docker and Maven setup.
