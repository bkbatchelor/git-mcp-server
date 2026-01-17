package io.sandboxdev.gitmcpserver.mcp;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;

@Service
public class McpMessageHandler {
    private final ToolRegistry toolRegistry;

    public McpMessageHandler(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public McpJsonRpcMessage handle(McpJsonRpcMessage request) {
        if ("tools/list".equals(request.method())) {
            List<Tool> tools = toolRegistry.getTools();
            return new McpJsonRpcMessage(
                "2.0",
                null,
                null,
                request.id(),
                Map.of("tools", tools),
                null
            );
        }

        // Basic error response for unknown methods
        return new McpJsonRpcMessage(
            "2.0",
            null,
            null,
            request.id(),
            null,
            Map.of("code", -32601, "message", "Method not found")
        );
    }
}