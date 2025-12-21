#### Security Scanners

* **SAST:** SonarQube / Checkmarx.
* **SCA:** Snyk / **OWASP Dependency Check** (configured as a Plugin in `build.gradle.kts`).
* **DAST:** OWASP ZAP.

#### AI Security Gates (OWASP Top 10 for LLMs)

* **Threat Model:** Adhere to the **OWASP Top 10 for Large Language Model Applications**.
* **Input Guardrails:** All user input destined for an LLM must be sanitized to prevent **Prompt Injection** (Jailbreaking).
* **Output Guardrails:** Treat LLM output as **untrusted**. Sanitize all AI-generated text before rendering to HTML to prevent XSS.
* **Secret Management:** LLM Provider keys (e.g., OpenAI, Anthropic) must be injected strictly as runtime environment variables. They must **never** be hardcoded or committed to version control.

#### Authentication & Authorization

* **Framework:** Spring Security 6.x (Lambda DSL).
* **Protocol:** OAuth 2.1 with JWT (RS256 signed).
* **Statelessness:** `SessionCreationPolicy.STATELESS`.

#### Implementation Rules

* **Endpoint Security:** All endpoints are private by default unless explicitly permitted.
* **Method Security:** Use `@EnableMethodSecurity` and `@PreAuthorize`.
