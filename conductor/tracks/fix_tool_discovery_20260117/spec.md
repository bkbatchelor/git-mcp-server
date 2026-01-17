# Fix Tool Discovery

## Context
The user reports "No prompts, tools, or resources found on the server" in Gemini CLI.
This likely means the server is not correctly advertising its capabilities or the tools list is empty.

## Goals
1. Verify `tools/list` response format.
2. Check `initialize` response capabilities.
3. Ensure tools are actually registered.

## Investigation
- `McpMessageHandler.java` handles `tools/list`.
- `GitToolsInitializer.java` registers tools.
- `debug_server.py` showed tools being returned, but maybe the format is slightly off for Gemini?
