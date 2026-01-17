package io.sandboxdev.gitmcpserver.mcp;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class McpMessageHandler {

    public McpJsonRpcMessage handle(McpJsonRpcMessage request) {
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
