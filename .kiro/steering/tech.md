# Technology Stack

## Environment

- Platform: Linux
- Shell: bash

## MCP Configuration

- MCP settings are managed in `.kiro/settings/mcp.json`
- MCP configuration is currently disabled in VSCode settings

## Common Commands

### MCP Server Management

```bash
# Run MCP server with uvx (requires uv package manager)
uvx <package-name>

# Install uv (if needed)
# Follow: https://docs.astral.sh/uv/getting-started/installation/
```

### Development Workflow

When working with MCP servers:
- Test MCP tools by making sample calls before checking configuration
- MCP servers reconnect automatically on config changes
- Use the MCP Server view in Kiro feature panel for management
