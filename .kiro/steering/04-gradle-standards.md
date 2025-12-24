---
name: gradle-standards
description: Enforces technical directives for the AI Assistant, covering Gradle. Use when generating, refactoring, or reviewing code to ensure architectural compliance.
version: 'Gradle 9.1.0'
temperature: 0.0
---

#### Build Tool Configuration (Gradle Kotlin DSL)

* **Format:** Use **Kotlin DSL** (`build.gradle.kts`) exclusively.
* **Version Management:** All dependency versions must be declared in a Version Catalog (`gradle/libs.versions.toml`).

#### Hallucination Prevention
* If asked about a library you are unsure is compatible with {version}, explicitly state "Compatibility Unverified."
* Do NOT suggest features from other versions unless they are in the permanent API.
* If the user asks for information outside the provided context, you MUST use the flag `isOutOfScope: true`.