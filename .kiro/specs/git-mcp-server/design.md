# Design Document: Git MCP Server

## Overview

The Git MCP Server is a Java 21 application that exposes Git repository operations through the Model Context Protocol (MCP). It serves as a bridge between AI assistants and Git repositories, enabling AI-driven version control operations. The server uses JGit for Git operations, Spring AI for MCP protocol implementation, and Maven for build management.

The architecture follows a layered approach with clear separation between the MCP protocol layer, business logic layer, and Git operations layer. This design ensures maintainability, testability, and adherence to MCP specifications.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      AI Assistant (Client)                   │
└───────────────────────────┬─────────────────────────────────┘
                            │ MCP Protocol (JSON-RPC)
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    Git MCP Server                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │           MCP Protocol Layer (Spring AI)               │ │
│  │  - Tool Registration & Discovery                       │ │
│  │  - Request/Response Handling                           │ │
│  │  - Resource Management                                 │ │
│  └──────────────────────┬─────────────────────────────────┘ │
│                         │                                   │
│  ┌──────────────────────▼─────────────────────────────────┐ │
│  │              Service Layer                             │ │
│  │  - GitRepositoryService                                │ │
│  │  - GitCommitService                                    │ │
│  │  - GitBranchService                                    │ │
│  │  - GitRemoteService                                    │ │
│  │  - CommitMessageGeneratorService                       │ │
│  └──────────────────────┬─────────────────────────────────┘ │
│                         │                                   │
│  ┌──────────────────────▼─────────────────────────────────┐ │
│  │           JGit Integration Layer                       │ │
│  │  - Repository Management                               │ │
│  │  - Git Command Execution                               │ │
│  │  - Error Translation                                   │ │
│  └────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
                   ┌────────────────┐
                   │  Git Repository│
                   │  (File System) │
                   └────────────────┘
```

### Component Interaction Flow

1. **AI Assistant** sends MCP tool invocation request
2. **MCP Protocol Layer** validates request and routes to appropriate service
3. **Service Layer** orchestrates business logic and calls JGit operations
4. **JGit Integration Layer** executes Git commands and returns results
5. **Service Layer** formats results for MCP response
6. **MCP Protocol Layer** sends response back to AI Assistant

## Components and Interfaces

### 1. MCP Protocol Layer

**GitMcpToolProvider**
- Registers all Git operation tools with Spring AI MCP framework
- Defines tool schemas with parameter validation
- Routes tool invocations to service layer

**GitMcpResourceProvider**
- Exposes repository information as MCP resources
- Provides read access to repository state and history

**McpErrorHandler**
- Translates exceptions to MCP error format
- Ensures consistent error responses

### 2. Service Layer

**GitRepositoryService**
```java
public interface GitRepositoryService {
    RepositoryInfo initRepository(Path path);
    RepositoryInfo cloneRepository(String url, Path targetPath, Credentials credentials);
    RepositoryStatus getStatus(Path repositoryPath);
    List<CommitInfo> getHistory(Path repositoryPath, int limit);
    CommitInfo getCommitDetails(Path repositoryPath, String commitHash);
    String getCurrentBranch(Path repositoryPath);
}
```

**GitCommitService**
```java
public interface GitCommitService {
    void stageFiles(Path repositoryPath, List<String> filePaths);
    void unstageFiles(Path repositoryPath, List<String> filePaths);
    CommitInfo createCommit(Path repositoryPath, String message, AuthorInfo author);
    String getDiff(Path repositoryPath, DiffType type, String... refs);
    String getFileContents(Path repositoryPath, String commitHash, String filePath);
}
```

**GitBranchService**
```java
public interface GitBranchService {
    BranchInfo createBranch(Path repositoryPath, String branchName);
    void switchBranch(Path repositoryPath, String branchName);
    List<BranchInfo> listBranches(Path repositoryPath);
    void deleteBranch(Path repositoryPath, String branchName);
}
```

**GitRemoteService**
```java
public interface GitRemoteService {
    void push(Path repositoryPath, String remote, String branch, Credentials credentials);
    void pull(Path repositoryPath, String remote, String branch, Credentials credentials);
    void fetch(Path repositoryPath, String remote, Credentials credentials);
    List<RemoteInfo> listRemotes(Path repositoryPath);
    void addRemote(Path repositoryPath, String name, String url);
}
```

**CommitMessageGeneratorService**
```java
public interface CommitMessageGeneratorService {
    String generateCommitMessage(Path repositoryPath, String summary, String description);
    Optional<String> extractProjectId(String branchName);
    CommitType determineCommitType(List<String> stagedFiles, String diff);
}
```

### 3. JGit Integration Layer

**JGitRepositoryManager**
- Manages JGit Repository instances
- Handles repository lifecycle (open, close, cache)
- Thread-safe repository access

**JGitCommandExecutor**
- Wraps JGit API calls
- Provides consistent error handling
- Translates JGit exceptions to domain exceptions

### 4. Domain Models

**RepositoryInfo**
```java
public record RepositoryInfo(
    Path path,
    String defaultBranch,
    boolean isBare
) {}
```

**RepositoryStatus**
```java
public record RepositoryStatus(
    String currentBranch,
    List<String> stagedFiles,
    List<String> unstagedFiles,
    List<String> untrackedFiles,
    boolean hasUncommittedChanges
) {}
```

**CommitInfo**
```java
public record CommitInfo(
    String hash,
    String shortHash,
    String message,
    AuthorInfo author,
    Instant timestamp,
    List<String> changedFiles,
    DiffStats stats
) {}
```

**BranchInfo**
```java
public record BranchInfo(
    String name,
    String commitHash,
    boolean isCurrent
) {}
```

**RemoteInfo**
```java
public record RemoteInfo(
    String name,
    String url,
    RemoteType type
) {}
```

## Data Models

### MCP Tool Schemas

Each Git operation is exposed as an MCP tool with a JSON schema defining its parameters:

**init-repository**
```json
{
  "name": "init-repository",
  "description": "Initialize a new Git repository",
  "inputSchema": {
    "type": "object",
    "properties": {
      "path": {
        "type": "string",
        "description": "Path where the repository should be initialized"
      }
    },
    "required": ["path"]
  }
}
```

**clone-repository**
```json
{
  "name": "clone-repository",
  "description": "Clone a Git repository from a remote URL",
  "inputSchema": {
    "type": "object",
    "properties": {
      "url": {
        "type": "string",
        "description": "Remote repository URL"
      },
      "targetPath": {
        "type": "string",
        "description": "Local path for the cloned repository"
      },
      "username": {
        "type": "string",
        "description": "Username for authentication (optional)"
      },
      "password": {
        "type": "string",
        "description": "Password or token for authentication (optional)"
      }
    },
    "required": ["url", "targetPath"]
  }
}
```

**get-status**
```json
{
  "name": "get-status",
  "description": "Get the current status of a Git repository",
  "inputSchema": {
    "type": "object",
    "properties": {
      "repositoryPath": {
        "type": "string",
        "description": "Path to the Git repository"
      }
    },
    "required": ["repositoryPath"]
  }
}
```

**get-history**
```json
{
  "name": "get-history",
  "description": "Get commit history from a repository",
  "inputSchema": {
    "type": "object",
    "properties": {
      "repositoryPath": {
        "type": "string",
        "description": "Path to the Git repository"
      },
      "limit": {
        "type": "integer",
        "description": "Maximum number of commits to return",
        "default": 10
      }
    },
    "required": ["repositoryPath"]
  }
}
```

**create-commit**
```json
{
  "name": "create-commit",
  "description": "Create a new commit with staged changes",
  "inputSchema": {
    "type": "object",
    "properties": {
      "repositoryPath": {
        "type": "string",
        "description": "Path to the Git repository"
      },
      "message": {
        "type": "string",
        "description": "Commit message"
      },
      "authorName": {
        "type": "string",
        "description": "Author name (optional, uses git config if not provided)"
      },
      "authorEmail": {
        "type": "string",
        "description": "Author email (optional, uses git config if not provided)"
      }
    },
    "required": ["repositoryPath", "message"]
  }
}
```

**generate-commit-message**
```json
{
  "name": "generate-commit-message",
  "description": "Generate a standardized commit message based on staged changes",
  "inputSchema": {
    "type": "object",
    "properties": {
      "repositoryPath": {
        "type": "string",
        "description": "Path to the Git repository"
      },
      "summary": {
        "type": "string",
        "description": "Brief summary of the changes"
      },
      "description": {
        "type": "string",
        "description": "Detailed description of the changes"
      }
    },
    "required": ["repositoryPath", "summary"]
  }
}
```

### Configuration Model

**GitMcpServerConfig**
```java
@Configuration
public class GitMcpServerConfig {
    private int maxCachedRepositories = 10;
    private Duration repositoryCacheTimeout = Duration.ofMinutes(30);
    private int maxHistoryLimit = 1000;
    private boolean enableAuthentication = true;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Repository Initialization and Cloning

**Property 1: Repository initialization creates valid Git repository**
*For any* valid file system path, initializing a repository should create a .git directory with proper Git configuration files.
**Validates: Requirements 1.1**

**Property 2: Duplicate initialization is rejected**
*For any* path containing an existing Git repository, attempting to initialize again should return an error and leave the existing repository unchanged.
**Validates: Requirements 1.4**

**Property 3: Successful clone returns repository information**
*For any* successful clone operation, the response should contain the local path and default branch name.
**Validates: Requirements 1.5**

### Repository Status and History

**Property 4: Status response contains all required fields**
*For any* repository, the status response should include current branch, staged files, unstaged files, and untracked files lists.
**Validates: Requirements 2.1**

**Property 5: Commit info contains complete metadata**
*For any* commit in the repository history, the commit information should include hash, author, timestamp, message, and changed files.
**Validates: Requirements 2.2, 2.4**

**Property 6: History limit is respected**
*For any* repository and any positive limit value n, requesting history with limit n should return at most n commits.
**Validates: Requirements 2.3**

**Property 7: Current branch is always defined**
*For any* repository, requesting the current branch should return a non-empty string matching an existing branch.
**Validates: Requirements 2.5**

### Commit Operations

**Property 8: Staging files adds them to staged list**
*For any* repository with modified files, staging those files should result in them appearing in the staged changes list from status.
**Validates: Requirements 3.1**

**Property 9: Unstaging preserves working directory changes**
*For any* staged files, unstaging them should remove them from the staged list but keep them in the unstaged list.
**Validates: Requirements 3.2**

**Property 10: Commit creation clears staging area**
*For any* repository with staged changes and a commit message, creating a commit should result in a new commit with that message and an empty staging area.
**Validates: Requirements 3.3, 3.4**

### Branch Operations

**Property 11: Branch creation adds to branch list**
*For any* repository and any valid branch name, creating a branch should result in that branch appearing in the branch list pointing to the current HEAD commit.
**Validates: Requirements 4.1**

**Property 12: Branch switching updates current branch**
*For any* two different branches, switching from one to the other should update the current branch indicator in status.
**Validates: Requirements 4.2**

**Property 13: Branch list has exactly one current branch**
*For any* repository, the branch list should contain at least one branch and exactly one branch should be marked as current.
**Validates: Requirements 4.3**

**Property 14: Branch deletion removes from list**
*For any* non-current branch, deleting it should remove it from the branch list.
**Validates: Requirements 4.4**

### Remote Operations

**Property 15: Fetch preserves working directory**
*For any* fetch operation, the working directory contents and status should remain unchanged before and after the fetch.
**Validates: Requirements 5.3**

**Property 16: Remote list contains all configured remotes**
*For any* repository, the remotes list should include all configured remotes with their names and URLs.
**Validates: Requirements 5.4**

**Property 17: Adding remote makes it appear in list**
*For any* repository and any valid remote name and URL, adding a remote should result in it appearing in the remotes list.
**Validates: Requirements 5.5**

### File Contents and Diffs

**Property 18: File contents retrieval is consistent**
*For any* file committed to a repository, retrieving its contents at that commit should return the same content regardless of current working directory state.
**Validates: Requirements 6.1**

**Property 19: Diff format is valid**
*For any* diff operation (between commits, staged, or unstaged), the returned diff should be in valid unified diff format with proper headers.
**Validates: Requirements 6.2, 6.3, 6.4**

### MCP Protocol Compliance

**Property 20: Tool list contains all Git operations**
*For any* tools list request, the response should contain all defined Git operation tools (init, clone, status, commit, branch, etc.) with valid JSON schemas.
**Validates: Requirements 7.2**

**Property 21: Valid tool invocations return MCP-formatted responses**
*For any* tool invocation with valid parameters, the response should conform to MCP response format with either a result or error field.
**Validates: Requirements 7.3**

**Property 22: Invalid parameters return MCP error format**
*For any* tool invocation with invalid parameters, the response should be an MCP error with code, message, and optional data fields.
**Validates: Requirements 7.4**

**Property 23: Resources expose repository information**
*For any* resources request, the response should contain MCP-formatted resources representing repository state.
**Validates: Requirements 7.5**

### Error Handling

**Property 24: Operations on invalid repositories return errors**
*For any* Git operation on a non-existent or invalid repository path, the operation should return an error without throwing uncaught exceptions.
**Validates: Requirements 8.1**

**Property 25: Exceptions are handled gracefully**
*For any* operation that encounters an unexpected exception, the server should log the error and return an error response without terminating.
**Validates: Requirements 8.4**

**Property 26: Concurrent operations maintain repository integrity**
*For any* set of concurrent Git operations on the same repository, the repository should remain in a valid state with no corruption.
**Validates: Requirements 8.5**

### Commit Message Generation

**Property 27: Project ID extraction from branch names**
*For any* branch name matching the pattern `<prefix>/<PROJECT-ID>-<number>`, extracting the project ID should return `<PROJECT-ID>-<number>`.
**Validates: Requirements 10.1**

**Property 28: Commit type is valid**
*For any* commit message generation request, the determined commit type should be one of: FEAT, FIX, CHORE, REFACTOR, DOCS, STYLE, or TEST.
**Validates: Requirements 10.2**

**Property 29: Generated commit message follows template**
*For any* generated commit message, it should match the format `[<ID>]:<Commit Type> <Summary>` followed by a blank line and body, where ID is present if extracted from branch.
**Validates: Requirements 10.3, 10.4, 10.5**

### Logging

**Property 30: Operations are logged appropriately**
*For any* Git operation invocation, there should be corresponding log entries at appropriate levels (debug for invocation, info for success, error for failures).
**Validates: Requirements 11.2, 11.3, 11.4**

## Error Handling

### Exception Hierarchy

```java
public class GitMcpException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;
}

public enum ErrorCode {
    REPOSITORY_NOT_FOUND,
    REPOSITORY_ALREADY_EXISTS,
    INVALID_REPOSITORY_STATE,
    AUTHENTICATION_FAILED,
    NETWORK_ERROR,
    INVALID_PARAMETERS,
    OPERATION_FAILED,
    CONCURRENT_MODIFICATION,
    FILE_NOT_FOUND,
    BRANCH_NOT_FOUND,
    UNCOMMITTED_CHANGES,
    NOTHING_TO_COMMIT
}
```

### Error Handling Strategy

1. **JGit Exception Translation**: All JGit exceptions are caught and translated to domain-specific `GitMcpException` with appropriate error codes
2. **MCP Error Mapping**: `GitMcpException` instances are mapped to MCP error responses with proper error codes and messages
3. **Logging**: All errors are logged with full context before being returned to clients
4. **No Silent Failures**: All error conditions result in explicit error responses
5. **Thread Safety**: Repository operations use proper locking to prevent concurrent modification issues

### Error Response Format

```json
{
  "error": {
    "code": "REPOSITORY_NOT_FOUND",
    "message": "Repository not found at path: /path/to/repo",
    "data": {
      "path": "/path/to/repo",
      "operation": "get-status"
    }
  }
}
```

## Testing Strategy

### Unit Testing

The Git MCP Server will use **JUnit 5** for unit testing with the following approach:

**Core Logic Tests:**
- Service layer methods with mocked JGit dependencies
- Commit message generation logic with various branch name patterns
- Error handling and exception translation
- MCP tool schema validation

**Example Unit Tests:**
- Test commit message generation with branch name `feature/PROJ-123` produces correct format
- Test project ID extraction from various branch name patterns
- Test commit type determination for different file change patterns
- Test error translation from JGit exceptions to GitMcpException
- Test MCP error response formatting

**Test Organization:**
- Co-locate tests with source files using `*Test.java` naming convention
- Use `@SpringBootTest` for integration tests requiring Spring context
- Use `@MockBean` for mocking JGit components in service tests

### Property-Based Testing

The Git MCP Server will use **jqwik** (a property-based testing framework for Java) for property-based testing with the following approach:

**Property Test Configuration:**
- Each property test will run a minimum of 100 iterations
- Each property test will be tagged with a comment referencing the design document property
- Tag format: `// Feature: git-mcp-server, Property {number}: {property_text}`

**Property Test Coverage:**
- Repository initialization with random valid paths
- History limit enforcement with random limit values
- Branch operations with random branch names
- Commit message format validation with random inputs
- Status response structure validation
- Concurrent operation safety with random operation sequences

**Generator Strategy:**
- Create custom generators for Git domain objects (paths, branch names, commit messages)
- Use constrained generators to produce valid Git inputs
- Generate edge cases (empty strings, special characters, long names)

**Example Property Tests:**
```java
@Property
// Feature: git-mcp-server, Property 6: History limit is respected
void historyLimitIsRespected(@ForAll @IntRange(min = 1, max = 100) int limit) {
    // Test that history never exceeds requested limit
}

@Property
// Feature: git-mcp-server, Property 27: Project ID extraction from branch names
void projectIdExtractionFromBranchNames(@ForAll("branchNamesWithIds") String branchName) {
    // Test that project IDs are correctly extracted
}
```

### Integration Testing

**MCP Protocol Integration:**
- Test full MCP handshake and tool invocation flow
- Test with actual Spring AI MCP framework
- Verify tool schemas are properly registered

**JGit Integration:**
- Test with real Git repositories (using temporary directories)
- Test actual Git operations (init, commit, branch, etc.)
- Test with various repository states

**Build Verification:**
- Maven build must complete successfully with `mvn clean install`
- All tests must pass before packaging
- Integration tests run in separate Maven profile

### Test Data Management

- Use JUnit 5 `@TempDir` for creating temporary Git repositories
- Clean up test repositories after each test
- Use test fixtures for common repository states
- Mock external dependencies (remote repositories, authentication)

## Dependencies and Technology Stack

### Core Dependencies

**pom.xml excerpt:**
```xml
<properties>
    <java.version>21</java.version>
    <spring.boot.version>3.5.8</spring.boot.version>
    <spring.ai.version>1.0.0-M1</spring.ai.version>
    <jgit.version>7.1.0.202411261347-r</jgit.version>
    <jqwik.version>1.9.2</jqwik.version>
</properties>

<dependencies>
    <!-- Spring Boot and Spring AI -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-mcp</artifactId>
        <version>${spring.ai.version}</version>
    </dependency>
    
    <!-- JGit -->
    <dependency>
        <groupId>org.eclipse.jgit</groupId>
        <artifactId>org.eclipse.jgit</artifactId>
        <version>${jgit.version}</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>net.jqwik</groupId>
        <artifactId>jqwik</artifactId>
        <version>${jqwik.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Build Plugins

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>21</source>
                <target>21</target>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                    <include>**/*Property.java</include>
                </includes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Deployment and Configuration

### Application Configuration

**application.yml:**
```yaml
git-mcp-server:
  repository:
    cache:
      max-size: 10
      timeout: 30m
  history:
    max-limit: 1000
  authentication:
    enabled: true
  mcp:
    server-name: "Git MCP Server"
    server-version: "1.0.0"

logging:
  level:
    com.example.gitmcp: DEBUG
    org.eclipse.jgit: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### Running the Server

```bash
# Build the project
mvn clean install

# Run the server
mvn spring-boot:run

# Or run the packaged JAR
java -jar target/git-mcp-server-1.0.0.jar
```

### MCP Client Configuration

Example MCP client configuration to connect to the Git MCP Server:

```json
{
  "mcpServers": {
    "git": {
      "command": "java",
      "args": ["-jar", "/path/to/git-mcp-server-1.0.0.jar"],
      "env": {
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

## Security Considerations

1. **Credential Handling**: Credentials for remote operations should never be logged or stored persistently
2. **Path Validation**: All file system paths should be validated to prevent directory traversal attacks
3. **Resource Limits**: Repository cache size and history limits prevent resource exhaustion
4. **Concurrent Access**: Proper locking prevents race conditions and repository corruption
5. **Error Messages**: Error messages should not expose sensitive system information

## Performance Considerations

1. **Repository Caching**: Frequently accessed repositories are cached to avoid repeated disk I/O
2. **Lazy Loading**: Commit history and diffs are loaded on-demand rather than eagerly
3. **Streaming**: Large diffs and file contents use streaming to minimize memory usage
4. **Connection Pooling**: Remote operations reuse connections when possible
5. **Async Operations**: Long-running operations (clone, fetch, push) can be made asynchronous

## Future Enhancements

1. **Merge Operations**: Support for merging branches and resolving conflicts
2. **Stash Operations**: Support for stashing and applying changes
3. **Tag Management**: Support for creating and managing Git tags
4. **Submodule Support**: Operations on repositories with submodules
5. **Git LFS**: Support for Git Large File Storage
6. **Webhooks**: Notifications for repository events
7. **Multi-Repository**: Managing multiple repositories simultaneously
8. **Advanced History**: Blame, bisect, and other history analysis tools
