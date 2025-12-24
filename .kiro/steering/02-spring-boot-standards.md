---
name: spring-boot-standards
description: Enforces technical directives for the Kiro project, covering Spring Boot. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
version: 'Spring Boot 3.5.9'
temperature: 0.0
---

#### Spring Boot Framework Guidelines

* **Dependency Injection:** Use **Constructor Injection** exclusively.
* **Configuration:** Externalize configuration using type-safe `@ConfigurationProperties`.
* **API Structure:** Implement global exception handling via `@RestControllerAdvice` conforming to RFC 7807.
* 
#### Hallucination Prevention
* If asked about a library you are unsure is compatible with {version}, explicitly state "Compatibility Unverified."
* Do NOT suggest features from other versions unless they are in the permanent API.
* If the user asks for information outside the provided context, you MUST use the flag `isOutOfScope: true`.

