package io.sandboxdev.gitmcpserver.mcp;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("unchecked")
public class McpMessageHandler {
    private final ToolRegistry toolRegistry;

    public McpMessageHandler(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public McpJsonRpcMessage handle(McpJsonRpcMessage request) {
        if ("initialize".equals(request.method())) {
            return new McpJsonRpcMessage(
                "2.0",
                null,
                null,
                request.id(),
                Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of(
                        "tools", Map.of(),
                        "resources", Map.of(),
                        "prompts", Map.of()
                    ),
                    "serverInfo", Map.of(
                        "name", "git-mcp-server",
                        "version", "0.0.1"
                    )
                ),
                null
            );
        }

        if ("notifications/initialized".equals(request.method())) {
            return null;
        }

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

        if ("resources/list".equals(request.method())) {
            return new McpJsonRpcMessage(
                "2.0",
                null,
                null,
                request.id(),
                Map.of("resources", List.of()),
                null
            );
        }

        if ("prompts/list".equals(request.method())) {
            return new McpJsonRpcMessage(
                "2.0",
                null,
                null,
                request.id(),
                Map.of("prompts", List.of()),
                null
            );
        }

        if ("tools/call".equals(request.method())) {
            Map<String, Object> params = (Map<String, Object>) request.params();
            if (params == null) {
                return errorResponse(request.id(), -32602, "Invalid params");
            }
            
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

            Optional<Tool> tool = toolRegistry.getTool(toolName);
            if (tool.isPresent()) {
                try {
                    Object result = tool.get().handler().apply(arguments);
                    return new McpJsonRpcMessage(
                        "2.0",
                        null,
                        null,
                        request.id(),
                        result,
                        null
                    );
                } catch (Exception e) {
                    return errorResponse(request.id(), -32000, "Tool execution failed: " + e.getMessage());
                }
            } else {
                return errorResponse(request.id(), -32601, "Tool not found: " + toolName);
            }
        }

        if (request.id() == null) {
            return null; // Don't respond to unknown notifications
        }

        return errorResponse(request.id(), -32601, "Method not found: " + request.method());
    }

    private McpJsonRpcMessage errorResponse(Object id, int code, String message) {
        return new McpJsonRpcMessage(
            "2.0",
            null,
            null,
            id,
            null,
            Map.of("code", code, "message", message)
        );
    }
}
