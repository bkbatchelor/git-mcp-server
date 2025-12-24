---
name: coding-standards
description: Enforces technical directives for the Kiro project, covering Spring Boot. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
---

#### Spring Boot Framework Guidelines

* **Dependency Injection:** Use **Constructor Injection** exclusively.
* **Configuration:** Externalize configuration using type-safe `@ConfigurationProperties`.
* **API Structure:** Implement global exception handling via `@RestControllerAdvice` conforming to RFC 7807.

