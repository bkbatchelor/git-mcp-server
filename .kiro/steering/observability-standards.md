---
name: observability-standards
description: Defines standards for distributed tracing, metrics, and logging for the Kiro project. Use this when implementing Micrometer, configuring Spring AI token tracking, setting up SLF4J logging, or defining Actuator health endpoints.
---

#### Distributed Tracing & Metrics
* **Framework:** Implement **Micrometer Tracing** and **Micrometer Observation** for unified metrics and distributed tracing.
* **Correlation:** Ensure all logs include **TraceID** and **SpanID** to facilitate request lifecycle tracking.

#### AI Observability
* **Token Tracking:** Enable Micrometer metrics for `gen_ai.client.token.usage` to monitor cost per request.
* **Tracing:** Ensure `spring-ai` traces are propagated to the distributed tracing system to visualize LLM latency.
* **Privacy:** By default, `spring.ai.chat.client.observations.log-prompt` and `log-completion` must be **FALSE** in production to prevent logging PII found in prompts.

#### Logging Standards
* **Facade:** Use **SLF4J** exclusively. Direct usage of `System.out` or implementation-specific loggers is prohibited.
* **Structure:** Log messages must use parameterized placeholders (e.g., `log.info("Order: {}", id)`).
* **Sanitization:** Strictly prohibit the logging of PII, passwords, tokens, or secrets.

#### Health Checks
* **Endpoints:** Expose standard `actuator/health` and `actuator/info` endpoints.
* **Security:** Sensitive actuator endpoints must be secured behind strict authorization rules.