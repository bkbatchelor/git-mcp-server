---
name: coding-standards
description: Enforces technical directives for the Kiro project, covering Java 21, Spring AI, Spring Boot, and Gradle. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
---

#### Java 21 Core Directives

* **Immutable Data:** Prefer **Java Records** (`record`) over traditional POJOs for DTOs and value objects.
* **Control Flow:** Utilize **Pattern Matching** for `switch` and `instanceof`.
* **Concurrency:** Prioritize **Virtual Threads** (Project Loom) for high-throughput I/O.
* **Null Safety:** Use `Optional<T>` explicitly for return types; never use `Optional` as a parameter.

#### Spring AI Directives

* **Structured Outputs:** Prefer `ChatClient.call().entity(MyRecord.class)` to deserialize LLM responses directly into **Java Records**. Avoid manual JSON parsing.
* **Prompt Templating:** Store large prompts as external `.st` or `.txt` resources in `src/main/resources/prompts/` rather than hardcoding strings.

#### Spring Boot Framework Guidelines

* **Dependency Injection:** Use **Constructor Injection** exclusively.
* **Configuration:** Externalize configuration using type-safe `@ConfigurationProperties`.
* **API Structure:** Implement global exception handling via `@RestControllerAdvice` conforming to RFC 7807.

#### Build Tool Configuration (Gradle Kotlin DSL)

* **Format:** Use **Kotlin DSL** (`build.gradle.kts`) exclusively.
* **Version Management:** All dependency versions must be declared in a Version Catalog (`gradle/libs.versions.toml`).