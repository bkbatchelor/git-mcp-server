# Fix Tool Schema

## Context
User still reports "No prompts, tools, or resources found".
Debug output showed `list_branches` has `inputSchema: {}`.
While valid JSON Schema, MCP tools expect `type: "object"` for the arguments object.
The client might be discarding tools with "invalid" schemas.

## Goals
1. Update `list_branches` to have a strict `type: "object"` schema.
2. Ensure `get_log` schema is also robust.

## Investigation
- `GitToolsInitializer.java` defines the schemas.
- `debug_discovery.py` can be used to verify the change.
