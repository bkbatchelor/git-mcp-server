---
name: testing-strategy
description: Mandates the TDD cycle, testing stack (JUnit 5, AssertJ, Mockito), and quality thresholds (mutation testing) for the Kiro project. Use this steering file when generating code, designing test cases, or configuring CI/CD pipelines to ensure compliance with architectural standards.
---

#### The TDD Mandate

* **Red-Green-Refactor:** Code generation must follow the cycle: fail first, pass simple, refactor safely.

#### Unit Testing Stack

* **Framework:** **JUnit 5 (Jupiter)**.
* **Assertions:** **AssertJ** (`assertThat...`).
* **Mocking:** **Mockito** (Strict stubbing enabled). Expand strict mocking to include `ChatClient` and `EmbeddingModel`.
* **Version Management:** All testing library versions must be managed via `gradle/libs.versions.toml`.

#### AI Evaluation & Testing

* **Mechanics vs. Intelligence:** Unit tests for AI services must verify **mechanics** (e.g., prompt template rendering), not **intelligence** (e.g., creative quality).
* **Determinism:** Use `spring-ai-test` to mock `ChatModel` responses, ensuring tests remain deterministic and fast.

#### Mutation Testing

* **Tool:** **PiTest** (configured via Gradle plugin `info.solidsoft.pitest` in `build.gradle.kts`).
* **Philosophy:** Code coverage is insufficient; tests must be robust enough to detect code changes ("mutants").
* **Thresholds:**
  * Mutation Coverage: Minimum **80%**.
  * Test Strength: Minimum **85%**.
* **Scope:** logic-heavy layers (Services, Domain logic). Exclude DTOs and Configuration.

#### Integration Testing

* **Slicing:** Prefer `@WebMvcTest` or `@DataJpaTest` over full `@SpringBootTest` where possible.
* **Containers:**
  * Use **Testcontainers** for database/broker dependencies.
  * **Vector Stores:** Mandatory use of Testcontainers for Vector Databases (e.g., `pgvector`, `chroma`) to verify embedding search logic.

#### Map Test to Property and Requirement

* **Specification Mapping:** Must enclose Property and Requirement(s) with JUnit 5 `@DisplayName` for all testing methods. Format @DisplayName argument according to this example: ```DisplayName("Property 7: Git Branch Operations (Req 6.1, 6.2, 6.3, 6.4)")```
  