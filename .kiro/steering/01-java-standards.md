---
name: coding-standards
description: Enforces technical directives for the Kiro project, covering Java 21, Spring AI, Spring Boot, and Gradle. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
---

#### Java 21 Core Directives

* **Immutable Data:** Prefer **Java Records** (`record`) over traditional POJOs for DTOs and value objects.
* **Control Flow:** Utilize **Pattern Matching** for `switch` and `instanceof`.
* **Concurrency:** Prioritize **Virtual Threads** (Project Loom) for high-throughput I/O.
* **Null Safety:** Use `Optional<T>` explicitly for return types; never use `Optional` as a parameter.


