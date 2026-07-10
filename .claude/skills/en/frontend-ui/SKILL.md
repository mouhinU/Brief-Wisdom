---
name: Frontend UI Skill
description: Use this skill for static frontend pages, admin UI components, modal flows, i18n, and API integration in the web resources.
---
# Frontend UI Skill

## Purpose

Use this skill for static frontend pages, admin UI components, modal flows, i18n, and API integration in the web resources.

## When to Use This Skill

Use it for page changes, component registration, modal flows, permission-driven UI updates, or static asset adjustments.

## Rules

- Follow the existing component pattern for UI modules
- Keep business logic in the JS layer rather than inline in HTML
- Use the shared API request pattern and error handling approach
- Sanitize dynamic HTML or markdown before rendering it into the DOM
- Keep permissions reflected in the UI, but do not rely on the UI as the only enforcement mechanism

## Recommended Workflow

1. Check whether the template and logic files both need updates
2. Reuse the existing component registration and API handling approach
3. Update translations and permission states alongside the UI change
4. Verify the updated UI still handles API failures gracefully

## Trigger Keywords

frontend, UI, component, modal, i18n, static page

## Priority

Medium unless the change touches permissions, sensitive data display, or critical flow paths.

## Default Output Expectations

List the affected page or component, the API interaction involved, and any translation or permission updates needed.

## Applicable Users

Frontend developers and full-stack contributors.

## Avoid

Do not place business logic inline in HTML or bypass permission-aware UI behavior.

## Example Prompt

Add a new admin tab to the resume management page and follow the existing Brief-Wisdom frontend component pattern.
