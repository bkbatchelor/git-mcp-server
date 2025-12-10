# MCP Client Configuration Examples

This directory contains example MCP client configurations for the Git MCP Server in different scenarios.

## Configuration Files

### basic-config.json
A minimal configuration for getting started with the Git MCP Server. Suitable for initial testing and evaluation.

**Features:**
- Basic Java JAR execution
- Standard logging level
- No auto-approved tools (manual approval required)

### development-config.json
Configuration optimized for development and debugging scenarios.

**Features:**
- Debug logging enabled
- Development Spring profile
- Common read-only operations auto-approved
- Detailed logging for troubleshooting

### production-config.json
Production-ready configuration with performance optimizations and security considerations.

**Features:**
- Memory limits configured
- Production Spring profile
- Optimized cache settings
- Reduced logging for performance
- Read-only operations auto-approved

### kiro-config.json
Configuration for use with Kiro AI assistant using uvx package manager.

**Features:**
- Uses uvx for package management
- Optimized for Kiro integration
- Common operations auto-approved
- FastMCP logging configuration

## Usage

1. Choose the configuration that best matches your use case
2. Copy the configuration to your MCP client's configuration file
3. Update the paths and settings as needed for your environment
4. Restart your MCP client to load the new configuration

## Configuration Options

### Command and Arguments
- `command`: The executable to run (java, uvx, etc.)
- `args`: Command line arguments including JAR path and JVM options

### Environment Variables
- `LOG_LEVEL`: Controls application logging (DEBUG, INFO, WARN, ERROR)
- `SPRING_PROFILES_ACTIVE`: Spring profile to activate (dev, prod, test)
- `GIT_MCP_SERVER_*`: Override application.yml settings via environment

### Auto-Approval
The `autoApprove` array lists tools that don't require manual approval:

**Safe for auto-approval (read-only):**
- `get-status`
- `get-history`
- `get-commit-details`
- `get-current-branch`
- `list-branches`
- `list-remotes`
- `get-diff`
- `get-file-contents`
- `generate-commit-message`

**Require manual approval (write operations):**
- `init-repository`
- `clone-repository`
- `stage-files`
- `unstage-files`
- `create-commit`
- `create-branch`
- `switch-branch`
- `delete-branch`
- `push`
- `pull`
- `fetch`
- `add-remote`

## Security Considerations

1. **Auto-Approval**: Only auto-approve tools you trust to run without confirmation
2. **File Paths**: Ensure the JAR path is secure and not writable by untrusted users
3. **Environment Variables**: Avoid putting sensitive information in environment variables
4. **Network Access**: The server may need network access for remote Git operations
5. **File System Access**: The server needs read/write access to Git repositories

## Troubleshooting

### Server Won't Start
- Check that Java 21 is installed and available
- Verify the JAR file path is correct
- Check file permissions on the JAR file
- Review logs for startup errors

### Connection Issues
- Ensure the MCP client can execute the command
- Check that all required dependencies are available
- Verify environment variables are set correctly
- Review server logs for connection errors

### Performance Issues
- Adjust JVM memory settings in production config
- Increase cache settings for frequently accessed repositories
- Consider reducing history limits for large repositories
- Monitor log levels to reduce I/O overhead