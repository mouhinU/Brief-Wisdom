---
name: Knowledge Base Skill
description: Use this skill for knowledge base CRUD, document import, retrieval, vector integration, or AI knowledge features.
---

# Knowledge Base Skill

## Purpose

Use this skill for knowledge base CRUD, document import, retrieval, vector integration, or AI knowledge features.

## When to Use This Skill

Use it for document import, knowledge ingestion, retrieval logic, or changes that connect knowledge data with AI
features.

## Rules

- Keep knowledge operations aligned with the AI module patterns and existing service contracts
- Validate file paths and imported content carefully to prevent unsafe access
- Reuse existing document and retrieval service abstractions where possible
- Keep knowledge ingestion and retrieval logic testable and isolated from UI details

## Recommended Workflow

1. Confirm whether the change needs embedding, indexing, or retrieval support
2. Validate imported file paths and storage boundaries
3. Implement the ingestion or retrieval change with existing service patterns
4. Add tests for import validation and retrieval behavior
5. Re-check permissions and performance after the change

## Trigger Keywords

knowledge base, document import, retrieval, vector, ingestion, RAG

## Priority

Medium to high if the change impacts AI retrieval, admin workflows, or file ingestion safety.

## Default Output Expectations

Mention the ingestion or retrieval flow, any safety or permission checks, and the validation scenario.

## Applicable Users

Developers working on knowledge base ingestion, document retrieval, or AI knowledge integration.

## Avoid

Do not allow unsafe file paths, weak permission checks, or unvalidated import behavior.

## Example Prompt

Add a new knowledge base import flow and ensure it is safe, permission-checked, and aligned with the existing retrieval
service.
