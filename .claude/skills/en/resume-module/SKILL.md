# Resume Module Skill

## Purpose

Use this skill for resume CRUD, AI polish, editor features, and resume-related UI integration.

## When to Use This Skill

Use it for new resume fields, AI polish flows, resume editor updates, or permission changes for resume data.

## Rules

- Keep controllers thin and delegate business logic to the resume service layer
- Use dedicated DTOs for API payloads and UI data exchange
- Preserve permission checks for sensitive resume operations
- Avoid exposing persistence entities directly to the frontend
- Keep AI polish and editing logic in the service layer

## Recommended Workflow

1. Define the request/response DTOs before implementation
2. Validate user input and content before persistence or AI processing
3. Implement the feature through the service layer and relevant UI touchpoints
4. Ensure the frontend uses the same API contract and permission model
5. Add tests for service logic and validation behavior

## Trigger Keywords

resume, polish, editor, work experience, project experience, achievement

## Priority

Medium unless the change affects permissions, AI polish, or core resume data flow.

## Default Output Expectations

Explain the affected resume data flow, the UI/API touchpoints, and the validation or permission impact.

## Applicable Users

Developers working on resume CRUD, AI polish, editor features, or resume-related UI integration.

## Avoid

Do not expose resume entities directly to the client or skip permission validation for editing actions.

## Example Prompt

Add a new resume field and wire it through the service layer, DTOs, and resume management UI.
