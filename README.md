# Git MCP Server

A Java 21 implementation of a Git Model Context Protocol (MCP) server that exposes Git repository operations to AI assistants through standardized MCP tools and resources.

## Overview

The Git MCP Server enables AI assistants to interact with Git repositories by providing a comprehensive set of tools for repository management, version control operations, and commit history analysis. Built with Spring Boot and JGit, it offers a robust and scalable solution for AI-driven Git operations.

## Features

- **Repository Management**: Initialize, clone, and manage Git repositories
- **Version Control Operations**: Stage, commit, branch, merge, and remote operations
- **History Analysis**: Browse commit history, view diffs, and analyze changes
- **Commit Message Generation**: Automatically generate standardized commit messages
- **MCP Protocol Compliance**: Full compatibility with Model Context Protocol specification
- **Thread-Safe Operations**: Concurrent access handling with repository-level locking
- **Comprehensive Error Handling**: Graceful error handling with detailed error messages
- **Extensive Logging**: Configurable logging for debugging and monitoring

## Requirements

- Java 21 or higher
- Maven 3.6 or higher
- Git (for repository operations)

## Quick Start

### Building the Project

```bash
# Clone the repository
git clone <repository-url>
cd git-mcp-server

# Build the project
mvn clean install

# Run tests
mvn test
```

### Running the Server

```bash
# Run with Maven
mvn spring-boot:run

# Or run the packaged JAR
java -jar target/git-mcp-server-1.0.0.jar
```

The server will start and listen for MCP connections. By default, it runs on the standard MCP protocol interface.

## Configuration

### Application Configuration

The server can be configured through `src/main/resources/application.yml`:

```yaml
git-mcp-server:
  repository:
    cache:
      max-size: 10          # Maximum number of cached repositories
      timeout: 30m          # Cache timeout duration
  history:
    max-limit: 1000         # Maximum commits returned in history
  authentication:
    enabled: true           # Enable authentication for remote operations
  mcp:
    server-name: "Git MCP Server"
    server-version: "1.0.0"

logging:
  level:
    "[io.sandboxdev.gitmcp]": DEBUG
    "[org.eclipse.jgit]": INFO
```

### MCP Client Configuration

To use the Git MCP Server with an MCP-compatible AI assistant, add the following configuration:

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

## Available Tools

The Git MCP Server provides the following tools:

### Repository Management
- `init-repository` - Initialize a new Git repository
- `clone-repository` - Clone a repository from a remote URL
- `get-status` - Get current repository status
- `get-history` - Retrieve commit history
- `get-commit-details` - Get detailed information about a specific commit
- `get-current-branch` - Get the name of the current branch

### File Operations
- `stage-files` - Add files to the staging area
- `unstage-files` - Remove files from the staging area
- `get-file-contents` - Retrieve file contents at a specific commit
- `get-diff` - Get differences between commits, staged, or unstaged changes

### Commit Operations
- `create-commit` - Create a new commit with staged changes
- `generate-commit-message` - Generate standardized commit messages

### Branch Management
- `create-branch` - Create a new branch
- `switch-branch` - Switch to a different branch
- `list-branches` - List all branches
- `delete-branch` - Delete a branch

### Remote Operations
- `push` - Push commits to a remote repository
- `pull` - Pull changes from a remote repository
- `fetch` - Fetch changes without merging
- `list-remotes` - List configured remotes
- `add-remote` - Add a new remote repository

## Usage Examples

### Basic Repository Operations

```bash
# Initialize a new repository
{
  "tool": "init-repository",
  "arguments": {
    "path": "/path/to/new/repo"
  }
}

# Clone an existing repository
{
  "tool": "clone-repository",
  "arguments": {
    "url": "https://github.com/user/repo.git",
    "targetPath": "/path/to/local/repo",
    "username": "your-username",
    "password": "your-token"
  }
}

# Get repository status
{
  "tool": "get-status",
  "arguments": {
    "repositoryPath": "/path/to/repo"
  }
}
```

### Commit Operations

```bash
# Stage files
{
  "tool": "stage-files",
  "arguments": {
    "repositoryPath": "/path/to/repo",
    "filePaths": ["src/main/java/Example.java", "README.md"]
  }
}

# Generate commit message
{
  "tool": "generate-commit-message",
  "arguments": {
    "repositoryPath": "/path/to/repo",
    "summary": "Add new feature",
    "description": "Implemented user authentication with JWT tokens"
  }
}

# Create commit
{
  "tool": "create-commit",
  "arguments": {
    "repositoryPath": "/path/to/repo",
    "message": "[PROJ-123]:FEAT Add user authentication",
    "authorName": "John Doe",
    "authorEmail": "john@example.com"
  }
}
```

### Branch Operations

```bash
# Create and switch to a new branch
{
  "tool": "create-branch",
  "arguments": {
    "repositoryPath": "/path/to/repo",
    "branchName": "feature/PROJ-123"
  }
}

{
  "tool": "switch-branch",
  "arguments": {
    "repositoryPath": "/path/to/repo",
    "branchName": "feature/PROJ-123"
  }
}
```

## Architecture

The Git MCP Server follows a layered architecture:

- **MCP Protocol Layer**: Handles MCP communication and tool registration
- **Service Layer**: Implements business logic for Git operations
- **JGit Integration Layer**: Manages JGit operations and repository access
- **Domain Models**: Defines data structures for Git entities

## Testing

The project includes comprehensive testing with both unit tests and property-based tests:

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="*Test"

# Run only property-based tests
mvn test -Dtest="*Property"
```

### Property-Based Testing

The server uses jqwik for property-based testing to ensure correctness across a wide range of inputs. Property tests validate:

- Repository initialization and cloning behavior
- Commit message generation formats
- Branch operations consistency
- History limit enforcement
- Error handling robustness

## Error Handling

The server provides comprehensive error handling with specific error codes:

- `REPOSITORY_NOT_FOUND` - Repository does not exist at specified path
- `REPOSITORY_ALREADY_EXISTS` - Repository already exists during initialization
- `INVALID_REPOSITORY_STATE` - Repository is in an invalid state for the operation
- `AUTHENTICATION_FAILED` - Invalid credentials for remote operations
- `NETWORK_ERROR` - Network connectivity issues
- `INVALID_PARAMETERS` - Invalid parameters provided to tools
- `OPERATION_FAILED` - General operation failure
- `CONCURRENT_MODIFICATION` - Concurrent access conflict
- `FILE_NOT_FOUND` - Requested file does not exist
- `BRANCH_NOT_FOUND` - Specified branch does not exist
- `UNCOMMITTED_CHANGES` - Uncommitted changes prevent operation
- `NOTHING_TO_COMMIT` - No staged changes to commit

## Logging

The server uses SLF4J with Logback for logging. Log levels can be configured in `application.yml`:

- `DEBUG` - Detailed operation information
- `INFO` - General operation status
- `WARN` - Warning conditions
- `ERROR` - Error conditions with stack traces

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:

1. Check the [Issues](../../issues) page for existing problems
2. Create a new issue with detailed information
3. Include logs and configuration details when reporting bugs

## Changelog

### Version 1.0.0
- Initial release
- Full MCP protocol support
- Complete Git operations suite
- Property-based testing implementation
- Comprehensive error handling and logging