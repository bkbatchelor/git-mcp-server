# Git MCP Server

A production-grade Model Context Protocol (MCP) Server that enables Large Language Models (LLMs) to safely interact with Git repositories through a secure, intelligent bridge.

## Features

- **MCP Protocol Compliance**: Strict adherence to MCP JSON-RPC 2.0 specification
- **Dual Transport Support**: Stdio for local IDE integration and SSE for remote communication
- **Java 21 & Virtual Threads**: High-throughput I/O operations with minimal resource overhead
- **JGit Integration**: Pure Java Git operations without external dependencies
- **Spring AI Integration**: Structured LLM interactions with OpenAI and Anthropic support
- **Comprehensive Security**: Input validation, path traversal prevention, and access control
- **Observability**: Distributed tracing, metrics, and structured logging
- **Property-Based Testing**: Comprehensive test coverage with mutation testing

## Prerequisites

- Java 21 or higher
- Git repository access

## Quick Start

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

3. **Run tests**:
   ```bash
   ./gradlew test
   ```

4. **Run mutation tests**:
   ```bash
   ./gradlew pitest
   ```

## Configuration

The application can be configured via `application.yml` or environment variables:

### Required Environment Variables

- `OPENAI_API_KEY`: OpenAI API key for AI integration
- `ANTHROPIC_API_KEY`: Anthropic API key for AI integration

### Key Configuration Properties

```yaml
git:
  mcp:
    transport:
      stdio-enabled: true
      sse-enabled: false
    security:
      allowed-repositories:
        - /path/to/allowed/repos
    observability:
      tracing-enabled: true
      metrics-enabled: true
```

## Architecture

The server follows a layered architecture:

- **Transport Layer**: Handles Stdio and SSE communication
- **Protocol Layer**: Implements MCP JSON-RPC 2.0 specification
- **Tool Layer**: Git operations exposed as MCP Tools
- **Resource Layer**: Repository context exposed as MCP Resources
- **Security Layer**: Input validation and access control
- **Observability Layer**: Logging, tracing, and metrics

## Development

### Project Structure

```
src/
├── main/java/io/sandboxdev/gitmcp/
│   ├── config/          # Configuration classes
│   ├── tools/           # MCP Tool implementations
│   ├── resources/       # MCP Resource providers
│   ├── model/           # Java Records (DTOs, Schemas)
│   └── security/        # Security and validation
└── test/java/           # Test classes
```

### Testing Strategy

- **Unit Tests**: JUnit 5 + AssertJ + Mockito
- **Property-Based Tests**: jqwik for universal correctness properties
- **Integration Tests**: Testcontainers for Git repository testing
- **Mutation Testing**: PiTest with 80% coverage threshold

### Build Tools

- **Gradle Kotlin DSL**: Modern build configuration
- **Version Catalog**: Centralized dependency management
- **OWASP Dependency Check**: Security vulnerability scanning
- **PiTest**: Mutation testing for test quality validation

## License

This project is licensed under the Apache License 2.0.