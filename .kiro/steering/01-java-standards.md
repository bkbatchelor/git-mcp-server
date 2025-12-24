---
name: java-standards
description: Enforces technical directives for the Kiro project, covering Java. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
version: 'Java 21'
temperature: 0.0
---

#### Java Core Directives

* **Immutable Data:** Prefer **Java Records** (`record`) over traditional POJOs for DTOs and value objects.
* **Control Flow:** Utilize **Pattern Matching** for `switch` and `instanceof`.
* **Concurrency:** Prioritize **Virtual Threads** (Project Loom) for high-throughput I/O.
* **Null Safety:** Use `Optional<T>` explicitly for return types; never use `Optional` as a parameter.
* **Class Name:** Java file name MUST be same as the class name

#### Hallucination Prevention
* If asked about a library you are unsure is compatible with {version}, explicitly state "Compatibility Unverified."
* Do NOT suggest features from other versions unless they are in the permanent API.
* If the user asks for information outside the provided context, you MUST use the flag `isOutOfScope: true`.


