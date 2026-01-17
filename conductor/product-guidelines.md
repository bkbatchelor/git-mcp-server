# Product Guidelines - git-mcp-server

## Communication Style
*   **Technical and Precise:** All logs and agent-facing responses should prioritize technical accuracy. Use exact Git commands, commit SHAs, and standard Git terminology. This ensures that the state of the repository is conveyed without ambiguity to developers and sophisticated AI agents.

## Operational Principles
*   **Atomicity and Rollback:** Every MCP tool call should strive to be atomic. If a complex operation (like a multi-file stage and commit) fails, the server must attempt to return the repository to its pre-call state to prevent "dirty" or intermediate states.
*   **Structured Error Handling:** Errors must be returned as structured objects using standard MCP error codes. Include specific Git metadata (e.g., conflicting filenames, lock file paths) to enable programmatically intelligent responses from the calling agent.

## Development Standards
*   **Spring Boot Idiomatic Patterns:** Adhere strictly to Spring Boot and Java 21 conventions. This includes the use of Dependency Injection, a clear service-layer architecture, and following standard project structures.
*   **Test-Driven Development (TDD):** Reliability is paramount. All new features and Git logic must be implemented using TDD, starting with unit tests for logic and integration tests for MCP tool endpoints.

## Governance
*   **Safety First:** When in doubt, default to a safe state. Destructive operations should be gated behind validation or user-confirmation protocols as defined in the product requirements.
