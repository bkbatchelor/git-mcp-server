package io.sandboxdev.gitmcpserver.mcp;

import java.util.Map;

public record Tool(
    String name,
    String description,
    Map<String, Object> inputSchema
) {}
