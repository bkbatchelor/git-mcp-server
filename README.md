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

## Cross-Platform Compatibility

The Git MCP Server is designed to work seamlessly across different operating systems:

### Supported Platforms
- **Windows** (Windows 10/11, Windows Server 2019+)
- **macOS** (macOS 10.15+)
- **Linux** (Ubuntu 18.04+, CentOS 7+, RHEL 7+, and other distributions)

### Cross-Platform Features
- **Automatic Line Ending Handling**: Git attributes configured for proper CRLF/LF handling
- **UTF-8 Encoding**: Consistent UTF-8 encoding across all platforms
- **Platform-Specific Scripts**: Both shell scripts (.sh) and batch files (.bat) provided
- **Maven Profile Activation**: Automatic platform detection for deployment
- **File Path Compatibility**: Proper handling of Windows and Unix file paths

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

### Automated Deployment

The project includes automated JAR deployment functionality that copies the built JAR to a specified directory using environment variables.

#### Basic Deployment

```bash
# Set your deployment directory
export DEPLOY_DIR="/path/to/your/deployment/directory"

# Build and automatically deploy
mvn clean package

# The JAR will be automatically copied to $DEPLOY_DIR/git-mcp-server-1.0.0.jar
```

#### Deployment Examples

**Unix/Linux/macOS:**
```bash
# Deploy to a local applications directory
export DEPLOY_DIR="/opt/git-mcp-server"
mvn clean package

# Deploy to your home directory
export DEPLOY_DIR="$HOME/applications"
mvn clean package

# Deploy to a project-specific location
export DEPLOY_DIR="./deploy"
mvn clean package

# Deploy to a shared network location
export DEPLOY_DIR="/mnt/shared/mcp-servers"
mvn clean package
```

**Windows:**
```cmd
REM Deploy to a local applications directory
set DEPLOY_DIR=C:\opt\git-mcp-server
mvn clean package

REM Deploy to your user directory
set DEPLOY_DIR=%USERPROFILE%\applications
mvn clean package

REM Deploy to a project-specific location
set DEPLOY_DIR=.\deploy
mvn clean package

REM Deploy to a shared network location
set DEPLOY_DIR=\\server\shared\mcp-servers
mvn clean package
```

#### Build Without Deployment

**Unix/Linux/macOS:**
```bash
# Simply don't set the DEPLOY_DIR variable
unset DEPLOY_DIR
mvn clean package

# JAR will only be created in target/ directory
```

**Windows:**
```cmd
REM Clear the DEPLOY_DIR variable
set DEPLOY_DIR=
mvn clean package

REM JAR will only be created in target\ directory
```

The deployment process will:
- Automatically create the destination directory if it doesn't exist
- Copy the JAR file to the specified location
- Provide console output showing the deployment status
- Continue the build process even if deployment fails (non-blocking)

### Running the Server

```bash
# Run with Maven
mvn spring-boot:run

# Or run the packaged JAR
java -Djava.awt.headless=true -jar target/git-mcp-server-1.0.0.jar

# For production environments, you may also want to add memory settings
java -Djava.awt.headless=true -Xmx512m -Xms256m -jar target/git-mcp-server-1.0.0.jar
```

The server will start and listen for MCP connections. By default, it runs on the standard MCP protocol interface.

## Configuration

### Application Configuration

The server can be configured through `src/main/resources/application.yml`:

```yaml
# Spring Boot Configuration
spring:
  main:
    web-application-type: none  # Disable web server to avoid port conflicts
    banner-mode: off           # Disable Spring Boot banner
  output:
    ansi:
      enabled: never           # Disable ANSI colors for cleaner logs

# Git MCP Server Configuration
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

# Logging Configuration
logging:
  level:
    root: WARN                              # Reduce root logging level
    "[io.sandboxdev.gitmcp]": INFO          # Application logs
    "[org.eclipse.jgit]": WARN              # JGit logs
    "[org.springframework]": WARN           # Spring logs
    "[org.springframework.boot]": ERROR     # Spring Boot startup logs
  file:
    name: logs/git-mcp-server.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 5
      total-size-cap: 50MB
  console:
    enabled: false                          # Disable console logging
```

#### Key Configuration Features:

- **Headless Operation**: Web server disabled to prevent port conflicts
- **Clean Startup**: Banner and verbose startup logs disabled
- **File-Only Logging**: All logs redirected to `logs/git-mcp-server.log`
- **Log Rotation**: Automatic log rotation with size and time-based policies
- **Optimized for MCP**: Configuration designed for MCP protocol communication

### MCP Client Configuration

To use the Git MCP Server with an MCP-compatible AI assistant, add the following configuration:

```json
{
  "mcpServers": {
    "git": {
      "command": "java",
      "args": [
        "-Djava.awt.headless=true",
        "-jar", 
        "/path/to/git-mcp-server-1.0.0.jar"
      ],
      "env": {
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

#### Using with Deployed JAR

If you've used the automated deployment feature, you can reference the deployed JAR directly:

```json
{
  "mcpServers": {
    "git": {
      "command": "java",
      "args": [
        "-Djava.awt.headless=true",
        "-jar", 
        "/opt/git-mcp-server/git-mcp-server-1.0.0.jar"
      ],
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

The server uses SLF4J with Logback for comprehensive logging management:

### Log Configuration

- **File-Only Logging**: All logs are written to `logs/git-mcp-server.log`
- **Console Logging Disabled**: Prevents JSON parsing errors in MCP communication
- **Automatic Rotation**: Logs rotate based on size (10MB) and time (daily)
- **Retention Policy**: Keeps 5 historical files, max 50MB total

### Log Levels

- `ERROR` - Critical errors and Spring Boot startup issues
- `WARN` - Warning conditions and JGit operations
- `INFO` - Application operations and Git MCP Server activities
- `DEBUG` - Detailed debugging information (disabled by default)

### Log File Management

```bash
# View current logs
tail -f logs/git-mcp-server.log

# View logs with specific level
grep "ERROR" logs/git-mcp-server.log

# Monitor log file size
ls -lh logs/

# Clean old logs (if needed)
find logs/ -name "*.log.gz" -mtime +30 -delete
```

### Environment-Specific Logging

You can override log levels using environment variables:

```bash
# Enable debug logging for development
export LOGGING_LEVEL_IO_SANDBOXDEV_GITMCP=DEBUG

# Change log file location
export LOGGING_FILE_NAME=/var/log/git-mcp-server.log
```
The server uses SLF4J with Logback for comprehensive logging management:

### Log Configuration

- **File-Only Logging**: All logs are written to `logs/git-mcp-server.log`
- **Console Logging Disabled**: Prevents JSON parsing errors in MCP communication
- **Automatic Rotation**: Logs rotate based on size (10MB) and time (daily)
- **Retention Policy**: Keeps 5 historical files, max 50MB total

### Log Levels

- `ERROR` - Critical errors and Spring Boot startup issues
- `WARN` - Warning conditions and JGit operations
- `INFO` - Application operations and Git MCP Server activities
- `DEBUG` - Detailed debugging information (disabled by default)

### Log File Management

```bash
# View current logs
tail -f logs/git-mcp-server.log

# View logs with specific level
grep "ERROR" logs/git-mcp-server.log

# Monitor log file size
ls -lh logs/

# Clean old logs (if needed)
find logs/ -name "*.log.gz" -mtime +30 -delete
```

### Environment-Specific Logging

You can override log levels using environment variables:

```bash
# Enable debug logging for development
export LOGGING_LEVEL_IO_SANDBOXDEV_GITMCP=DEBUG

# Change log file location
export LOGGING_FILE_NAME=/var/log/git-mcp-server.log
```

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

## Development Tools

### Testing Deployment

The project includes cross-platform test scripts to verify the deployment functionality:

**Unix/Linux/macOS:**
```bash
# Make the test script executable
chmod +x test-deploy.sh

# Run the deployment test
./test-deploy.sh
```

**Windows:**
```cmd
REM Run the deployment test
test-deploy.bat
```

These scripts will:
1. Test building without deployment
2. Test building with deployment to a temporary directory
3. Verify that the JAR was copied correctly
4. Show the results

### Log Management

Cross-platform log management scripts are available:

**Unix/Linux/macOS:**
```bash
chmod +x log-management.sh
./log-management.sh tail    # Follow logs in real-time
./log-management.sh errors  # View only errors
./log-management.sh size    # Check log file sizes
```

**Windows:**
```cmd
log-management.bat tail     REM Follow logs in real-time
log-management.bat errors   REM View only errors
log-management.bat size     REM Check log file sizes
```

### Manual Testing

For manual testing of the deployment feature, see `manual-test.md` for detailed instructions.

## Changelog

### Version 1.0.0
- Initial release
- Full MCP protocol support
- Complete Git operations suite
- Property-based testing implementation
- Comprehensive error handling and logging
- Automated JAR deployment with environment variable configuration
- Automated JAR deployment with environment variable configuration