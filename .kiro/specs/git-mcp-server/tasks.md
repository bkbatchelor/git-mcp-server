# Implementation Plan

- [x] 1. Set up Maven project structure and dependencies
  - Create pom.xml with Java 21, Spring Boot 3.5.8, Spring AI MCP support, JGit 7.1.0, and jqwik 1.9.2
  - Configure Maven compiler plugin for Java 21
  - Configure Spring Boot Maven plugin for running the application
  - Configure Maven Surefire plugin to run both JUnit and jqwik tests
  - Create standard Maven directory structure (src/main/java, src/main/resources, src/test/java)
  - Create application.yml with Git MCP Server configuration
  - _Requirements: 9.1, 9.2, 9.3, 9.5_

- [x] 2. Implement domain models and exceptions
  - Create record classes for RepositoryInfo, RepositoryStatus, CommitInfo, BranchInfo, RemoteInfo, AuthorInfo, Credentials, DiffStats
  - Create GitMcpException class with ErrorCode enum
  - Create ErrorCode enum with all error types (REPOSITORY_NOT_FOUND, INVALID_REPOSITORY_STATE, etc.)
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 3. Implement JGit integration layer
  - Create JGitRepositoryManager class for managing Repository instances with caching
  - Implement repository lifecycle methods (open, close, cache management)
  - Create JGitCommandExecutor class to wrap JGit API calls
  - Implement exception translation from JGit exceptions to GitMcpException
  - _Requirements: 1.1, 8.1, 8.4, 8.5_

- [ ]* 3.1 Write property test for repository initialization
  - **Property 1: Repository initialization creates valid Git repository**
  - **Validates: Requirements 1.1**

- [ ]* 3.2 Write property test for duplicate initialization rejection
  - **Property 2: Duplicate initialization is rejected**
  - **Validates: Requirements 1.4**

- [x] 4. Implement GitRepositoryService
  - Create GitRepositoryService interface and implementation
  - Implement initRepository method to create new Git repositories
  - Implement cloneRepository method with credential support
  - Implement getStatus method to return repository status
  - Implement getHistory method with limit parameter
  - Implement getCommitDetails method for specific commits
  - Implement getCurrentBranch method
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 4.1 Write property test for successful clone response
  - **Property 3: Successful clone returns repository information**
  - **Validates: Requirements 1.5**

- [ ]* 4.2 Write property test for status response structure
  - **Property 4: Status response contains all required fields**
  - **Validates: Requirements 2.1**

- [ ]* 4.3 Write property test for commit metadata completeness
  - **Property 5: Commit info contains complete metadata**
  - **Validates: Requirements 2.2, 2.4**

- [ ]* 4.4 Write property test for history limit enforcement
  - **Property 6: History limit is respected**
  - **Validates: Requirements 2.3**

- [ ]* 4.5 Write property test for current branch validity
  - **Property 7: Current branch is always defined**
  - **Validates: Requirements 2.5**

- [x] 5. Implement GitCommitService
  - Create GitCommitService interface and implementation
  - Implement stageFiles method to add files to staging area
  - Implement unstageFiles method to remove files from staging
  - Implement createCommit method with author information
  - Implement getDiff method for different diff types (unstaged, staged, between commits)
  - Implement getFileContents method for retrieving file at specific commit
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 5.1 Write property test for staging files
  - **Property 8: Staging files adds them to staged list**
  - **Validates: Requirements 3.1**

- [ ]* 5.2 Write property test for unstaging preserves changes
  - **Property 9: Unstaging preserves working directory changes**
  - **Validates: Requirements 3.2**

- [ ]* 5.3 Write property test for commit creation
  - **Property 10: Commit creation clears staging area**
  - **Validates: Requirements 3.3, 3.4**

- [ ]* 5.4 Write property test for file contents consistency
  - **Property 18: File contents retrieval is consistent**
  - **Validates: Requirements 6.1**

- [ ]* 5.5 Write property test for diff format validity
  - **Property 19: Diff format is valid**
  - **Validates: Requirements 6.2, 6.3, 6.4**

- [x] 6. Implement GitBranchService
  - Create GitBranchService interface and implementation
  - Implement createBranch method to create new branches
  - Implement switchBranch method with uncommitted changes check
  - Implement listBranches method with current branch indication
  - Implement deleteBranch method with validation
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 6.1 Write property test for branch creation
  - **Property 11: Branch creation adds to branch list**
  - **Validates: Requirements 4.1**

- [ ]* 6.2 Write property test for branch switching
  - **Property 12: Branch switching updates current branch**
  - **Validates: Requirements 4.2**

- [ ]* 6.3 Write property test for branch list invariant
  - **Property 13: Branch list has exactly one current branch**
  - **Validates: Requirements 4.3**

- [ ]* 6.4 Write property test for branch deletion
  - **Property 14: Branch deletion removes from list**
  - **Validates: Requirements 4.4**

- [x] 7. Implement GitRemoteService
  - Create GitRemoteService interface and implementation
  - Implement push method with credential support
  - Implement pull method with credential support
  - Implement fetch method ensuring working directory preservation
  - Implement listRemotes method
  - Implement addRemote method
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 7.1 Write property test for fetch preserves working directory
  - **Property 15: Fetch preserves working directory**
  - **Validates: Requirements 5.3**

- [ ]* 7.2 Write property test for remote list completeness
  - **Property 16: Remote list contains all configured remotes**
  - **Validates: Requirements 5.4**

- [ ]* 7.3 Write property test for adding remotes
  - **Property 17: Adding remote makes it appear in list**
  - **Validates: Requirements 5.5**

- [x] 8. Implement CommitMessageGeneratorService
  - Create CommitMessageGeneratorService interface and implementation
  - Implement extractProjectId method to parse branch names for project IDs
  - Implement determineCommitType method to analyze staged changes
  - Implement generateCommitMessage method to format messages according to template
  - Support branch name patterns like feature/PROJ-123, bugfix/PROJ-456
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ]* 8.1 Write property test for project ID extraction
  - **Property 27: Project ID extraction from branch names**
  - **Validates: Requirements 10.1**

- [ ]* 8.2 Write property test for commit type validity
  - **Property 28: Commit type is valid**
  - **Validates: Requirements 10.2**

- [ ]* 8.3 Write property test for commit message format
  - **Property 29: Generated commit message follows template**
  - **Validates: Requirements 10.3, 10.4, 10.5**

- [ ] 9. Implement MCP protocol layer with Spring AI
  - Create GitMcpToolProvider class annotated with Spring AI MCP annotations
  - Register init-repository tool with JSON schema
  - Register clone-repository tool with JSON schema
  - Register get-status tool with JSON schema
  - Register get-history tool with JSON schema
  - Register get-commit-details tool with JSON schema
  - Register stage-files tool with JSON schema
  - Register unstage-files tool with JSON schema
  - Register create-commit tool with JSON schema
  - Register create-branch tool with JSON schema
  - Register switch-branch tool with JSON schema
  - Register list-branches tool with JSON schema
  - Register delete-branch tool with JSON schema
  - Register push tool with JSON schema
  - Register pull tool with JSON schema
  - Register fetch tool with JSON schema
  - Register list-remotes tool with JSON schema
  - Register add-remote tool with JSON schema
  - Register get-diff tool with JSON schema
  - Register get-file-contents tool with JSON schema
  - Register generate-commit-message tool with JSON schema
  - Implement tool invocation handlers that delegate to service layer
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ]* 9.1 Write property test for tool list completeness
  - **Property 20: Tool list contains all Git operations**
  - **Validates: Requirements 7.2**

- [ ]* 9.2 Write property test for valid tool invocation responses
  - **Property 21: Valid tool invocations return MCP-formatted responses**
  - **Validates: Requirements 7.3**

- [ ]* 9.3 Write property test for invalid parameter error handling
  - **Property 22: Invalid parameters return MCP error format**
  - **Validates: Requirements 7.4**

- [ ] 10. Implement MCP resource provider
  - Create GitMcpResourceProvider class for exposing repository information
  - Implement resource endpoints for repository status
  - Implement resource endpoints for branch information
  - Implement resource endpoints for commit history
  - Format resources according to MCP specification
  - _Requirements: 7.5_

- [ ]* 10.1 Write property test for resource exposure
  - **Property 23: Resources expose repository information**
  - **Validates: Requirements 7.5**

- [ ] 11. Implement error handling and logging
  - Create McpErrorHandler class to translate exceptions to MCP error format
  - Implement error response formatting with error codes and messages
  - Configure SLF4J with Logback for logging
  - Add logging to all service methods (debug for invocation, info for success, error for failures)
  - Implement startup logging with version and configuration information
  - Ensure all exceptions are logged with full stack traces
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ]* 11.1 Write property test for error handling robustness
  - **Property 24: Operations on invalid repositories return errors**
  - **Validates: Requirements 8.1**

- [ ]* 11.2 Write property test for exception handling
  - **Property 25: Exceptions are handled gracefully**
  - **Validates: Requirements 8.4**

- [ ]* 11.3 Write property test for logging behavior
  - **Property 30: Operations are logged appropriately**
  - **Validates: Requirements 11.2, 11.3, 11.4**

- [ ] 12. Implement thread safety and concurrent access handling
  - Add synchronization to JGitRepositoryManager for cache access
  - Implement repository-level locking for operations that modify state
  - Add concurrent access tests to verify repository integrity
  - _Requirements: 8.5_

- [ ]* 12.1 Write property test for concurrent operation safety
  - **Property 26: Concurrent operations maintain repository integrity**
  - **Validates: Requirements 8.5**

- [ ] 13. Create Spring Boot application main class
  - Create GitMcpServerApplication class with @SpringBootApplication annotation
  - Configure component scanning for all service and MCP classes
  - Add application startup logging
  - _Requirements: 9.4, 11.1_

- [ ] 14. Write example-based integration tests
  - Write integration test for MCP handshake and initialization
  - Write integration test for cloning a repository with authentication
  - Write integration test for push/pull operations
  - Write integration test for network error handling
  - Write integration test for authentication failure handling
  - Write integration test for Maven build verification
  - _Requirements: 1.2, 1.3, 5.1, 5.2, 7.1, 8.2, 8.3, 9.4_

- [ ] 15. Create documentation and configuration files
  - Create README.md with project overview, build instructions, and usage examples
  - Create logback.xml configuration file for logging
  - Add JavaDoc comments to all public interfaces and classes
  - Create example MCP client configuration
  - _Requirements: 11.5_

- [ ] 16. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
