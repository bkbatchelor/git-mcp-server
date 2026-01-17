package io.sandboxdev.gitmcpserver.mcp;

import java.util.Map;

import java.util.function.Function;

public record Tool(
    String name,
    String description,
    Map<String, Object> inputSchema,
    @com.fasterxml.jackson.annotation.JsonIgnore
    Function<Map<String, Object>, Object> handler
) {}
