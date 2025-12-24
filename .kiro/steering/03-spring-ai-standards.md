---
name: spring-ai-standards
description: Enforces technical directives for the Kiro project, covering Spring AI. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
version: 'Spring AI 1.0.0-M4'
temperature: 0.0
---

#### Spring AI Directives

* **Structured Outputs:** Prefer `ChatClient.call().entity(MyRecord.class)` to deserialize LLM responses directly into **Java Records**. Avoid manual JSON parsing.
* **Prompt Templating:** Store large prompts as external `.st` or `.txt` resources in `src/main/resources/prompts/` rather than hardcoding strings.

#### Hallucination Prevention
* If asked about a library you are unsure is compatible with {version}, explicitly state "Compatibility Unverified."
* Do NOT suggest features from other versions unless they are in the permanent API.
* If the user asks for information outside the provided context, you MUST use the flag `isOutOfScope: true`.


