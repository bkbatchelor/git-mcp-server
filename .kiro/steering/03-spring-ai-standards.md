---
name: coding-standards
description: Enforces technical directives for the Kiro project, covering Spring AI. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
---

#### Spring AI Directives

* **Structured Outputs:** Prefer `ChatClient.call().entity(MyRecord.class)` to deserialize LLM responses directly into **Java Records**. Avoid manual JSON parsing.
* **Prompt Templating:** Store large prompts as external `.st` or `.txt` resources in `src/main/resources/prompts/` rather than hardcoding strings.


