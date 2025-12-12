# Project Structure

## Directory Organization

```
.
├── .kiro/              # Kiro AI assistant configuration
│   ├── settings/       # MCP and other settings
│   └── steering/       # AI guidance documents
└── .vscode/            # VSCode editor settings
```

## Configuration Files

- `.vscode/settings.json` - Editor-specific settings
- `.kiro/settings/mcp.json` - MCP server configurations (when created)
- `.kiro/steering/*.md` - AI assistant steering rules

## Conventions

- Use relative paths for workspace files
- MCP configurations follow JSON format with mcpServers object
- Steering documents use markdown format
