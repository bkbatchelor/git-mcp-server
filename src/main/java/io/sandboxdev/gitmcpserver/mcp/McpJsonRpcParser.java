package io.sandboxdev.gitmcpserver.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;

public class McpJsonRpcParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static McpJsonRpcMessage parse(String json) {
        try {
            return objectMapper.readValue(json, McpJsonRpcMessage.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON-RPC message", e);
        }
    }
}
