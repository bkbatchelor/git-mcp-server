---
name: security-protocols
description: Defines security standards including SAST scanner requirement, input/output guardrails, and Spring Security authentication. Use when implementing security features or configuring project infrastructure.
---

#### Security Scanners

* **SAST:** SonarQube

#### AI Security Gates 

* **Input Guardrails:** All user input destined for an LLM must be sanitized to prevent **Prompt Injection** (Jailbreaking).
* **Output Guardrails:** Treat LLM output as **untrusted**. Sanitize all AI-generated text before rendering to HTML to prevent XSS.

#### Authentication & Authorization

* **Framework:** Spring Security 6.x (Lambda DSL).
* **Protocol:** OAuth 2.1 with JWT (RS256 signed).
* **Statelessness:** `SessionCreationPolicy.STATELESS`.

#### Implementation Rules

* **Endpoint Security:** All endpoints are private by default unless explicitly permitted.
* **Method Security:** Use `@EnableMethodSecurity` and `@PreAuthorize`.