---
name: AI Module Skill
description: Use this skill for AI chat, model management, knowledge retrieval, rate limiting, content filtering, streaming, or audit-related features.
---
# AI Module Skill

## Purpose

Use this skill for AI chat, model management, knowledge retrieval, rate limiting, content filtering, streaming, or audit-related features.

## When to Use This Skill

Use it for chat flows, provider/model changes, knowledge retrieval updates, safety checks, or audit logging.

## Key Principles

- Keep AI request handling in the service layer
- Apply rate limiting and content filtering before expensive AI processing
- Preserve safe error handling for streaming endpoints
- Avoid leaking API keys or sensitive prompt details in logs or responses
- Reuse the existing provider registry and prompt patterns

## Recommended Workflow

1. Review the relevant AI flow and affected service layer
2. Keep the change aligned with the current provider and model patterns
3. Apply or preserve rate limiting and content filtering where needed
4. Add tests for input filtering, rate-limit behavior, or provider fallback logic
5. Verify the relevant AI module tests or build

## Trigger Keywords

AI, chat, model, knowledge, rate limit, content filter, streaming

## Priority

High for user-facing AI behavior or safety-related logic; medium for internal improvements.

## Default Output Expectations

Mention the affected AI flow, any safety guardrails, and the verification step.

## Applicable Users

Developers working on AI chat, model management, RAG, knowledge retrieval, or AI safety features.

## Avoid

Do not bypass rate limiting, content filtering, or secret handling for convenience.

## Example Prompt

Add a new AI chat capability while preserving the existing content filter and rate-limit behavior.
