# Product Overview

## Git MCP Server

Build a production-grade **Model Context Protocol (MCP) Server** using **Spring Boot** and **Spring AI**, specifically designed for Git operations. This integration acts as a secure, intelligent bridge between Large Language Models (LLMs) and the developer's IDE (Kiro), enabling AI to safely execute Git commands and retrieve context.

## Key Product Pillars

### 1. Interoperability & Compliance

* **Standard Compliance:** The server must strictly adhere to the **Model Context Protocol (MCP) JSON-RPC 2.0** specification.
* **Transport Flexibility:** Support **Stdio** for local IDE integration and **SSE (Server-Sent Events)** for remote agents.
* **Structured Data:** All data exchanges (Inputs, Outputs, Tool definitions) rely on strict schemas defined via **Java Records**.

### 2. Safety & Trust (Zero Trust AI)

* **Input Guardrails:** All user input destined for an LLM is sanitized to prevent **Prompt Injection** (Jailbreaking).
* **Output Guardrails:** AI output is treated as **untrusted** and sanitized before rendering (e.g., preventing XSS).
* **Human-in-the-Loop:** High-stakes tools (file deletion, system execution) must trigger an explicit confirmation via the IDE UI.
* **Secret Management:** API keys (OpenAI, Anthropic) are never hardcoded; they are injected strictly as runtime environment variables.

### 3. Observability & Transparency

* **Cost & Usage:** Monitor token usage (`gen_ai.client.token.usage`) to track cost per request.
* **Performance:** Visualize LLM latency and request lifecycles using distributed tracing (TraceID/SpanID).
* **Privacy:** Production logs must strictly exclude prompt text and completions (`log-prompt`, `log-completion` = FALSE) to protect PII.

## Core Capabilities

* **Tools:** Expose functional capabilities (code modification, file analysis) as "Tools" that the LLM can invoke.
* **Resources:** Provide read-only context (files, database rows) via custom URI schemes (e.g., `kiro-resource://`).
* **Graceful Degradation:** When tools fail, return readable error strings to the AI rather than crashing, enabling self-correction.

## Target Users

- Developers building AI-powered Git workflows
- DevOps teams monitoring MCP server performance
- System administrators managing Git repository access through AI assistants