# Git MCP Server Design Document

## Overview

The Git MCP Server is a Java 21 application that bridges AI assistants with Git repositories through the Model Context Protocol (MCP). The server exposes Git operations as MCP tools, enabling AI-driven version control workflows while maintaining security and data integrity.

The architecture follows a layered approach with clear separation between MCP protocol handling, Git operations, and data serialization. The server uses JGit for Git operations, Spring AI for MCP protocol implementation, and follows modern Java practices with Gradle Kotlin DSL for build management.

## Architecture

### High-Level Architecture

```
┌─────────────────┐    MCP Protocol    ┌─────────────────┐
│   AI Assistant  │ ◄─────────────────► │  Git MCP Server │
│    (Client)     │    JSON-RPC 2.0    │                 │
└─────────────────┘                    └─────────────────┘
                                                │
                                                │ JGit API
                                                ▼
                                       ┌─────────────────┐
                                       │ Git Repository  │
                                       │  (File System)  │
                                       └─────────────────┘
```

### Layer Architecture

1. **MCP Protocol Layer**: Handles JSON-RPC 2.0 communication, session management, and protocol compliance
2. **Service Layer**: Orchestrates Git operations and manages business logic
3. **Repository Layer**: Abstracts Git operations using JGit
4. **Data Layer**: Handles serialization/deserialization and data validation

## Components and Interfaces

### Core Components

#### MCPServerController
- **Purpose**: Entry point for MCP protocol messages
- **Responsibilities**: 
  - Handle JSON-RPC 2.0 requests/responses
  - Manage client sessions and authentication
  - Route requests to appropriate services
- **Key Methods**:
  - `handleToolCall(ToolCallRequest): ToolCallResponse`
  - `initializeSession(InitRequest): InitResponse`
  - `listTools(): ToolListResponse`

#### GitOperationService
- **Purpose**: Orchestrates Git operations and business logic
- **Responsibilities**:
  - Coordinate complex Git workflows
  - Handle error scenarios and validation
  - Manage transaction boundaries
- **Key Methods**:
  - `executeGitOperation(GitOperationRequest): GitOperationResult`
  - `validateRepository(String path): ValidationResult`

#### GitRepository (Interface)
- **Purpose**: Abstract Git operations for testability
- **Implementations**: JGitRepository
- **Key Methods**:
  - `init(String path): Repository`
  - `clone(String url, String path): Repository`
  - `commit(String message, List<String> files): CommitResult`
  - `createBranch(String name): BranchResult`
  - `merge(String branchName): MergeResult`

#### DataSerializer
- **Purpose**: Handle JSON serialization/deserialization for MCP protocol
- **Responsibilities**:
  - Convert Git objects to/from JSON
  - Validate data integrity
  - Handle round-trip consistency
- **Key Methods**:
  - `serialize(GitObject): JsonNode`
  - `deserialize(JsonNode, Class<T>): T`

### Interface Definitions

```java
public interface GitRepository {
    Repository init(String path) throws GitException;
    Repository clone(String url, String path, CredentialsProvider credentials) throws GitException;
    Status getStatus(Repository repo) throws GitException;
    void add(Repository repo, List<String> filePatterns) throws GitException;
    RevCommit commit(Repository repo, String message) throws GitException;
    Ref createBranch(Repository repo, String branchName) throws GitException;
    void checkout(Repository repo, String branchName) throws GitException;
    List<Ref> listBranches(Repository repo) throws GitException;
    MergeResult merge(Repository repo, String branchName) throws GitException;
    void push(Repository repo, CredentialsProvider credentials) throws GitException;
    void pull(Repository repo, CredentialsProvider credentials) throws GitException;
}

public interface MCPProtocolHandler {
    void handleRequest(JsonRpcRequest request, JsonRpcResponse response);
    void initializeSession(String clientId, Map<String, Object> capabilities);
    List<ToolDefinition> getAvailableTools();
}
```

## Data Models

### Core Data Models

#### GitOperationRequest
```java
public class GitOperationRequest {
    private String operation;           // "init", "clone", "commit", etc.
    private String repositoryPath;      // Local repository path
    private Map<String, Object> parameters; // Operation-specific parameters
    private CredentialsProvider credentials; // Authentication info
}
```

#### GitOperationResult
```java
public class GitOperationResult {
    private boolean success;
    private String message;
    private Map<String, Object> data;   // Operation-specific result data
    private List<String> errors;       // Error details if any
}
```

#### RepositoryStatus
```java
public class RepositoryStatus {
    private List<String> stagedFiles;
    private List<String> unstagedFiles;
    private List<String> untrackedFiles;
    private String currentBranch;
    private boolean hasUncommittedChanges;
}
```

#### CommitInfo
```java
public class CommitInfo {
    private String hash;
    private String message;
    private String author;
    private String email;
    private Instant timestamp;
    private List<String> parentHashes;
}
```

### MCP Protocol Models

#### ToolDefinition
```java
public class ToolDefinition {
    private String name;
    private String description;
    private JsonSchema inputSchema;     // JSON Schema for parameters
}
```

#### ToolCallRequest
```java
public class ToolCallRequest {
    private String toolName;
    private Map<String, Object> arguments;
    private String requestId;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, I need to perform property reflection to eliminate redundancy before writing the final properties:

**Property Reflection:**
- Properties 1.1-1.5 (MCP connection handling) can be consolidated into comprehensive session management properties
- Properties 2.1-2.5 (basic Git operations) are distinct and should remain separate
- Properties 3.1-3.5 (branch operations) are distinct and should remain separate  
- Properties 4.1-4.5 (remote operations) are distinct and should remain separate
- Properties 5.1, 5.3, and 5.5 can be consolidated into a comprehensive serialization round-trip property
- Properties 5.2 and 5.4 (parsing validation) can be combined into one validation property
- Properties 6.1-6.5 (error handling) can be consolidated into comprehensive error response properties

### Session Management Properties

Property 1: MCP session lifecycle management
*For any* valid AI assistant client, establishing an MCP connection should result in a valid authenticated session that can handle protocol messages and clean up resources upon disconnection
**Validates: Requirements 1.1, 1.2, 1.4, 1.5**

Property 2: Authentication failure handling
*For any* invalid client credentials, connection attempts should be rejected with appropriate error messages
**Validates: Requirements 1.3**

### Git Operation Properties

Property 3: Repository initialization
*For any* valid file system path, initializing a Git repository should create a functional repository at that location
**Validates: Requirements 2.1**

Property 4: Repository cloning
*For any* valid Git URL and local path, cloning should create a complete local copy of the remote repository
**Validates: Requirements 2.2**

Property 5: Repository status reporting
*For any* Git repository state, status requests should return accurate information about staged, unstaged, and untracked files
**Validates: Requirements 2.3**

Property 6: File staging
*For any* valid file paths in a repository, adding them should stage the files for commit
**Validates: Requirements 2.4**

Property 7: Commit creation
*For any* valid commit message and staged changes, creating a commit should produce a new commit with those changes
**Validates: Requirements 2.5**

### Branch Management Properties

Property 8: Branch creation
*For any* valid branch name, creating a branch should result in a new branch that can be checked out
**Validates: Requirements 3.1**

Property 9: Branch checkout
*For any* existing branch, checking it out should switch the working directory to that branch
**Validates: Requirements 3.2**

Property 10: Branch listing
*For any* repository, listing branches should return all local and remote branches with accurate status information
**Validates: Requirements 3.3**

Property 11: Commit history retrieval
*For any* repository and log configuration, retrieving commit history should return commits according to the specified depth and format
**Validates: Requirements 3.4**

Property 12: Branch merging
*For any* valid source and target branch combination, merging should integrate changes from source into target branch
**Validates: Requirements 3.5**

### Remote Operation Properties

Property 13: Push operations
*For any* repository with local commits, pushing should transfer commits to the configured remote repository
**Validates: Requirements 4.1**

Property 14: Pull operations
*For any* repository with remote changes, pulling should fetch and merge remote changes into the local branch
**Validates: Requirements 4.2**

Property 15: Fetch operations
*For any* repository with remote changes, fetching should retrieve remote changes without modifying the working directory
**Validates: Requirements 4.3**

Property 16: Credential handling
*For any* remote operation requiring authentication, credentials should be handled securely without exposure
**Validates: Requirements 4.4**

Property 17: Conflict reporting
*For any* remote operation that encounters conflicts, detailed conflict information should be returned to enable resolution
**Validates: Requirements 4.5**

### Serialization Properties

Property 18: Git data serialization round-trip
*For any* Git object, serializing to JSON and then deserializing should produce an equivalent object with all essential metadata preserved
**Validates: Requirements 5.1, 5.3, 5.5**

Property 19: Git data validation
*For any* MCP request containing Git data, parsing should validate the data against Git specifications and return specific error messages for invalid data
**Validates: Requirements 5.2, 5.4**

### Error Handling Properties

Property 20: Structured error responses
*For any* Git operation failure, the response should contain structured error information with specific error codes and descriptive messages
**Validates: Requirements 6.1, 6.3**

Property 21: Error logging and classification
*For any* system error or repository access failure, detailed error information should be logged and errors should be properly classified by type
**Validates: Requirements 6.2, 6.4**

Property 22: Graceful failure handling
*For any* unexpected system state, the server should fail gracefully while maintaining system stability
**Validates: Requirements 6.5**

### Configuration Properties

Property 23: External configuration support
*For any* valid configuration file or environment variable, the server should properly load and apply the configuration settings
**Validates: Requirements 7.4**

## Error Handling

### Error Classification

The system implements a hierarchical error classification:

1. **Protocol Errors**: MCP protocol violations, malformed requests
2. **Authentication Errors**: Invalid credentials, authorization failures  
3. **Git Errors**: Repository not found, merge conflicts, invalid operations
4. **System Errors**: File system issues, network failures, resource exhaustion

### Error Response Format

```java
public class ErrorResponse {
    private String errorCode;        // Standardized error code
    private String message;          // Human-readable description
    private Map<String, Object> details; // Context-specific error details
    private String timestamp;        // ISO 8601 timestamp
    private String requestId;        // Original request identifier
}
```

### Error Handling Strategies

- **Validation Errors**: Return immediately with specific validation messages
- **Git Operation Errors**: Wrap JGit exceptions with contextual information
- **Network Errors**: Implement retry logic with exponential backoff
- **Resource Errors**: Graceful degradation and resource cleanup

## Testing Strategy

### Dual Testing Approach

The Git MCP Server requires both unit testing and property-based testing for comprehensive coverage:

- **Unit tests** verify specific examples, edge cases, and integration points
- **Property tests** verify universal properties across all valid inputs
- Together they provide complete coverage: unit tests catch concrete bugs, property tests verify general correctness

### Property-Based Testing

**Framework**: The system will use **QuickCheck for Java** (net.java.quickcheck) for property-based testing.

**Configuration**: Each property-based test must run a minimum of 100 iterations to ensure adequate random input coverage.

**Test Tagging**: Each property-based test must include a comment with this exact format:
`**Feature: git-mcp-server, Property {number}: {property_text}**`

**Implementation Requirements**:
- Each correctness property must be implemented by a single property-based test
- Tests must generate appropriate random inputs for Git operations
- Tests must validate the universal properties defined in this document
- Property tests should focus on core logic without excessive mocking

### Unit Testing

**Coverage Areas**:
- Specific examples demonstrating correct behavior
- Edge cases like empty repositories, invalid paths, network timeouts
- Integration points between MCP protocol and Git operations
- Error conditions and exception handling

**Testing Framework**: JUnit 5 with Mockito for mocking external dependencies

### Test Data Management

- **Repository Fixtures**: Temporary test repositories with known states
- **Mock Remote Repositories**: Local Git repositories simulating remote operations
- **Credential Mocking**: Test credentials that don't require real authentication
- **Network Simulation**: Mock network conditions for remote operation testing