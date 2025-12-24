# Implementation Plan: Git MCP Server

## Overview

This implementation plan breaks down the Git MCP Server design into discrete coding tasks that build incrementally toward a complete, production-ready MCP server. The plan follows a bottom-up approach, starting with core infrastructure and building up to the full Git operations layer.

## Tasks

- [x] 1. Set up project structure and core dependencies
  - Create Spring Boot project structure with Java 21 and Virtual Threads
  - Configure Gradle build with version catalog (gradle/libs.versions.toml)
  - Add core dependencies: Spring Boot 3.5.x, Spring AI, JGit, Micrometer
  - **Testing Stack Setup**: Add JUnit 5 (Jupiter), AssertJ, Mockito with strict stubbing, jqwik for property testing
  - **Mutation Testing**: Configure PiTest Gradle plugin (`info.solidsoft.pitest`) with 80% mutation coverage, 85% test strength thresholds
  - **AI Testing**: Add `spring-ai-test` for mocking ChatModel responses
  - **Integration Testing**: Add Testcontainers for Git repository testing
  - Configure application.yml with basic MCP server properties
  - Set up Logback configuration to redirect logs to System.err
  - _Requirements: 15.1, 15.2, 15.5_

- [x] 2. Implement core MCP protocol foundation (TDD approach)
  - [x] 2.1 Write property test for MCP protocol serialization (RED)
    - **Property 12: Schema Serialization**
    - **Validates: Requirements 11.2, 11.3, 11.4, 11.5**
    - Write failing tests for JSON serialization/deserialization

  - [x] 2.2 Create MCP protocol data models using Java Records (GREEN)
    - Define McpMessage, McpRequest, McpResponse, McpNotification records
    - Create ToolDefinition, ToolResult, ResourceDefinition, ResourceContent records
    - Implement JSON serialization/deserialization to make tests pass
    - _Requirements: 1.1, 1.2, 11.1, 11.2_

  - [x] 2.3 Write property test for JSON-RPC protocol compliance (RED)
    - **Property 1: JSON-RPC Protocol Compliance**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.5**
    - Write failing tests for JSON-RPC message validation

  - [x] 2.4 Implement JSON-RPC dispatcher (GREEN)
    - Create McpJsonRpcDispatcher component for message routing
    - Implement request validation against MCP specification
    - Add error handling with proper JSON-RPC error codes to make tests pass
    - _Requirements: 1.1, 1.3, 1.5, 10.4_

- [-] 3. Implement transport layer (TDD approach)
  - [x] 3.1 Write property test for Stdio transport logging isolation (RED)
    - **Property 2: Stdio Transport Logging Isolation**
    - **Validates: Requirements 2.3**
    - Write failing tests for log redirection to System.err

  - [x] 3.2 Create Stdio transport implementation (GREEN)
    - Implement Stdio message reading from System.in using Virtual Threads
    - Configure JSON-RPC message writing to System.out
    - Ensure all application logs redirect to System.err to make tests pass
    - _Requirements: 2.1, 2.3, 2.4_

  - [x] 3.3 Write property test for Virtual Thread I/O handling (RED)
    - **Property 3: Virtual Thread I/O Handling**
    - **Validates: Requirements 2.4**
    - Write failing tests for Virtual Thread I/O operations

  - [x] 3.4 Create SSE transport implementation (GREEN)
    - Implement HTTP endpoint for Server-Sent Events
    - Add CORS support for cross-origin requests
    - Configure async servlet handling for SSE streams to make tests pass
    - _Requirements: 2.2, 2.5_

- [x] 4. Checkpoint - Ensure transport layer tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement Git repository management (TDD approach)
  - [ ] 5.1 Write unit tests for Git data models (RED)
    - Write failing tests for GitStatus, GitCommitInfo, GitBranchInfo records
    - Write failing tests for Tool schema records for all Git operations
    - Write failing tests for Git-specific parameter validation
    - _Requirements: 3.1, 4.1, 5.1, 6.1, 7.1_

  - [ ] 5.2 Create Git data models (GREEN)
    - Define GitStatus, GitCommitInfo, GitBranchInfo records to make tests pass
    - Create Tool schema records for all Git operations to make tests pass
    - Implement validation for Git-specific parameters to make tests pass
    - _Requirements: 3.1, 4.1, 5.1, 6.1, 7.1_

  - [ ] 5.3 Create JGit repository manager (GREEN + REFACTOR)
    - Implement JGitRepositoryManager service with thread-safe repository access
    - Add repository validation and lifecycle management
    - Create helper methods for common Git operations (status, commit, diff, log)
    - Refactor to improve code quality while keeping tests green
    - _Requirements: 3.5, 4.5, 5.1, 7.1_

- [ ] 6. Implement Git Tools (TDD approach)
  - [ ] 6.1 Write property test for Git status operations (RED)
    - **Property 4: Git Status Operations**
    - **Validates: Requirements 3.1, 3.2, 3.4**
    - Write failing tests for Git status functionality

  - [ ] 6.2 Implement GitStatusTool (GREEN)
    - Create git_status tool with JGit integration
    - Handle repository validation and error cases
    - Return structured status with modified, staged, untracked files to make tests pass
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ] 6.3 Write property test for Git commit operations (RED)
    - **Property 5: Git Commit Operations**
    - **Validates: Requirements 4.1, 4.3, 4.4**
    - Write failing tests for Git commit functionality

  - [ ] 6.4 Implement GitCommitTool (GREEN)
    - Create git_commit tool with message validation
    - Validate staged changes and repository state
    - Return commit hash and summary on success to make tests pass
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ] 6.5 Write property test for Git diff operations (RED)
    - **Property 6: Git Diff Operations**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**
    - Write failing tests for Git diff functionality

  - [ ] 6.6 Implement GitDiffTool (GREEN)
    - Create git_diff tool supporting multiple diff modes
    - Handle unstaged changes, single ref vs HEAD, two refs comparison
    - Return unified diff format output to make tests pass
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 7. Implement remaining Git Tools (TDD approach)
  - [ ] 7.1 Write property test for Git branch operations (RED)
    - **Property 7: Git Branch Operations**
    - **Validates: Requirements 6.1, 6.2, 6.3**
    - Write failing tests for Git branch functionality

  - [ ] 7.2 Implement GitBranchTool (GREEN)
    - Create git_branch_list, git_branch_create, git_checkout tools
    - Handle branch validation and conflict detection
    - Manage uncommitted changes during checkout to make tests pass
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ] 7.3 Write property test for Git log operations (RED)
    - **Property 8: Git Log Operations**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
    - Write failing tests for Git log functionality

  - [ ] 7.4 Implement GitLogTool (GREEN)
    - Create git_log tool with filtering support
    - Support limit, file path, and date filtering
    - Return commits in reverse chronological order to make tests pass
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 8. Implement Tool and Resource registries (TDD approach)
  - [ ] 8.1 Write property test for resource access (RED)
    - **Property 9: Resource Access**
    - **Validates: Requirements 8.2, 8.3, 8.4, 8.5**
    - Write failing tests for resource URI resolution and content access

  - [ ] 8.2 Create GitToolRegistry (GREEN)
    - Implement tool registration and discovery
    - Add tool metadata and schema exposure
    - Handle tool execution with proper error handling to make tests pass
    - _Requirements: 1.1, 10.1, 10.2, 11.1_

  - [ ] 8.3 Create GitResourceRegistry (GREEN)
    - Implement resource URI resolution
    - Create repository-info and file content resources
    - Handle resource not found scenarios to make tests pass
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 9. Checkpoint - Ensure Git operations tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 10. Implement security and validation layer (TDD approach)
  - [ ] 10.1 Write property test for input validation and security (RED)
    - **Property 10: Input Validation and Security**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5**
    - Write failing tests for input validation and security guardrails

  - [ ] 10.2 Create GitInputValidator (GREEN)
    - Implement schema-based parameter validation
    - Add path traversal prevention
    - Create input sanitization for commit messages and branch names to make tests pass
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ] 10.3 Implement security guardrails (GREEN)
    - Create repository allowlist enforcement
    - Add rate limiting for resource-intensive operations
    - Implement output sanitization to make tests pass
    - _Requirements: 9.5, 10.1, 10.3_

- [ ] 11. Implement error handling and observability (TDD approach)
  - [ ] 11.1 Write property test for error handling (RED)
    - **Property 11: Error Handling**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5**
    - Write failing tests for error translation and graceful degradation

  - [ ] 11.2 Create comprehensive error handling (GREEN)
    - Implement graceful error translation to JSON-RPC errors
    - Add descriptive error messages with corrective actions
    - Handle timeouts and resource constraints to make tests pass
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 11.3 Write property test for logging behavior (RED)
    - **Property 13: Logging Behavior**
    - **Validates: Requirements 12.2, 12.3, 12.4, 12.5**
    - Write failing tests for logging correlation and MCP notifications

  - [ ] 11.4 Implement logging and tracing (GREEN)
    - Configure SLF4J with TraceID/SpanID correlation
    - Create custom MCP notification appender
    - Add dynamic log level adjustment tool to make tests pass
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ] 11.5 Write property test for observability (RED)
    - **Property 14: Observability**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.5**
    - Write failing tests for metrics and distributed tracing

  - [ ] 11.6 Configure observability and metrics (GREEN)
    - Set up Micrometer Tracing and Observation
    - Add Git operation metrics and health endpoints
    - Configure production-safe logging (no PII) to make tests pass
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ] 12. Implement configuration management and headless deployment
  - [ ] 12.1 Create configuration properties
    - Define GitMcpProperties with @ConfigurationProperties
    - Add TransportConfig, SecurityConfig, RepositoryConfig, HeadlessConfig records
    - Implement configuration validation with fail-fast behavior
    - _Requirements: 15.1, 15.2, 15.4, 15.5, 16.8_

  - [ ] 12.2 Add environment variable support
    - Configure API key injection from environment variables
    - Support profile-specific configurations
    - Add configuration validation at startup
    - _Requirements: 15.3, 15.4, 15.5_

  - [ ] 12.3 Write property test for headless deployment (RED)
    - **Property 17: Headless Deployment**
    - **Validates: Requirements 16.1, 16.3, 16.4, 16.5, 16.7**
    - Write failing tests for headless operation capabilities

  - [ ] 12.4 Implement headless deployment features (GREEN)
    - Add graceful shutdown handling via SIGTERM/SIGINT signals
    - Configure daemon mode operation for background processes
    - Implement structured JSON logging for log aggregation systems
    - Add health check endpoints for container orchestration
    - Ensure zero GUI dependencies in runtime classpath to make tests pass
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.7_

  - [ ] 12.5 Add batch processing support (GREEN)
    - Implement batch processing mode for sequential Git operations
    - Add configuration for batch operation timeouts
    - Support Docker deployment with mounted configuration files
    - _Requirements: 16.6, 16.8_

- [ ] 13. Ensure stateless operation (TDD approach)
  - [ ] 13.1 Write property test for stateless operation (RED)
    - **Property 15: Stateless Operation**
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.5**
    - Write failing tests for stateless behavior and concurrent request independence

  - [ ] 13.2 Validate stateless architecture (GREEN)
    - Review all components for stateless operation
    - Configure Spring Security with STATELESS session policy
    - Ensure concurrent request independence to make tests pass
    - _Requirements: 14.1, 14.2, 14.4, 14.5_

- [ ] 14. Integration and final wiring
  - [ ] 14.1 Wire all components together
    - Create main Spring Boot application class
    - Configure component scanning and auto-configuration
    - Add startup validation and health checks
    - _Requirements: 1.4, 15.1_

  - [ ] 14.2 Create application configuration
    - Finalize application.yml with all required properties
    - Add profile-specific configurations (dev, prod)
    - Configure actuator endpoints with security
    - _Requirements: 15.2, 15.5, 13.4_

  - [ ] 14.3 Write integration tests (RED then GREEN)
    - **TDD Approach**: Write failing integration tests first
    - **Test Slicing**: Use `@WebMvcTest` for transport layer, avoid full `@SpringBootTest` where possible
    - **Testcontainers**: Use Testcontainers for real Git repository testing (mandatory for Git operations)
    - Test end-to-end MCP protocol flows with mocked external dependencies
    - Test transport layer integration with proper Spring test slices
    - **REFACTOR**: Optimize implementation to make all integration tests pass
    - _Requirements: All requirements_

- [ ] 15. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- **TDD Mandate**: All tasks follow strict Test-Driven Development (Red-Green-Refactor cycle)
  - **RED**: Write failing tests first to define expected behavior
  - **GREEN**: Write minimal implementation to make tests pass  
  - **REFACTOR**: Improve code quality while keeping tests green
- **Testing Stack**: JUnit 5 (Jupiter) + AssertJ + Mockito (strict stubbing)
- **Version Management**: All testing library versions managed via `gradle/libs.versions.toml`
- **Mutation Testing**: PiTest configured via Gradle plugin with 80% mutation coverage, 85% test strength
- **Integration Testing**: Prefer `@WebMvcTest`/`@DataJpaTest` over `@SpringBootTest`, use Testcontainers for Git repositories
- **AI Testing**: Use `spring-ai-test` to mock `ChatModel` responses for deterministic tests
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using jqwik framework