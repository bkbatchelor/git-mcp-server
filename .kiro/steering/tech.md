# Technology Stack

## Core Runtime

* **Language:** **Java 21** (LTS).
* **Concurrency:** **Virtual Threads** (Project Loom) for high-throughput I/O.
* **Data Structures:** **Java Records** (`record`) preferred for DTOs, Tool Schemas, and value objects.
* **Control Flow:** Pattern Matching for `switch` and `instanceof`.

* **Repository Operation:** JGit Pure Java implementation of Git

## Frameworks

* **App Framework:** **Spring Boot 3.5.9**.
* **AI Integration:** **Spring AI 1.1.2**.
  * **Structured Output:** Use `ChatClient.call().entity(MyRecord.class)` for deserialization.
* **Security:** **Spring Security 6.x** (Lambda DSL) with OAuth 2.1 / JWT.


## Build & Quality (Gradle Kotlin DSL)

* **Build Script:** `build.gradle.kts`.
* **Version Control:** `gradle/libs.versions.toml` (Version Catalog).

## Testing Strategy

* **Philosophy:** TDD (Red-Green-Refactor).
* **Unit Testing:** **JUnit 5 (Jupiter)** + **AssertJ** + **Mockito** (Strict Stubbing).
  * **AI Testing:** Mock `ChatClient`/`EmbeddingModel` using `spring-ai-test` for determinism.
* **Integration Testing:**
  * Use `@WebMvcTest` or `@DataJpaTest` slices where possible.
  * **Testcontainers:** Mandatory for Vector Stores (pgvector, chroma) and Databases.
