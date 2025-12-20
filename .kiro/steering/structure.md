# Project Structure

## Architectural Patterns

### 1. The "Stdio" Constraint (Critical)

The Kiro IDE integration relies on standard input/output streams. Pollution of `System.out` will corrupt the JSON-RPC protocol.

* **Stdout:** Reserved **exclusively** for JSON-RPC protocol messages.
* **Stderr:** All application logs (Spring Boot startup, SLF4J, etc.) must be redirected here.
* **Logging Bridge:** A custom SLF4J Appender must forward log events to the MCP Host (IDE) via `notifications/message`.

### 2. Spring Boot & AI Design

* **Dependency Injection:** Use **Constructor Injection** exclusively.
* **Statelessness:** The server is stateless; context is passed via protocol primitives, not stored in memory.
* **API Structure:** Global exception handling via `@RestControllerAdvice` (RFC 7807).
* **Prompt Management:** Store large prompts externally in `src/main/resources/prompts/` (using `.st` or `.txt`).

## Project Directory Layout

The project follows a standard Spring Boot layout, enhanced for MCP and AI requirements:

```text
src/
├── main/
│   ├── java/io.sandboxdev/gitmcp/
│   │   ├── config/          # @ConfigurationProperties, MCP Server Config
│   │   ├── tools/           # MCP Tool implementations (Service layer)
│   │   ├── resources/       # MCP Resource providers
│   │   ├── model/           # Java Records (DTOs, Tool Schemas)
│   │   └── security/        # Auth (Spring Security) & Input Guardrails
│   └── resources/
│       ├── prompts/         # Externalized AI prompts (.st, .txt)
│       ├── application.yml  # Main config
│       └── logback-spring.xml # MANDATORY: Redirects logs to System.err
├── test/
│   └── java/                # JUnit 5 tests & Testcontainer config
├── gradle/
│   └── libs.versions.toml   # Centralized version management
└── build.gradle.kts         # Kotlin DSL build script
