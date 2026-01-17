package io.sandboxdev.gitmcpserver.mcp;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ToolDiscoveryTests {

    @Test
    void shouldReturnRegisteredTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(new Tool("test_tool", "A test tool", Map.of(), params -> "success"));
        
        McpMessageHandler handler = new McpMessageHandler(registry);
        McpJsonRpcMessage request = new McpJsonRpcMessage("2.0", "tools/list", null, 1, null, null);
        
        McpJsonRpcMessage response = handler.handle(request);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.result();
        @SuppressWarnings("unchecked")
        List<Tool> tools = (List<Tool>) result.get("tools");
        
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).name()).isEqualTo("test_tool");
    }

    @Test
    void shouldExecuteTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(new Tool("test_tool", "desc", Map.of(), args -> "success"));
        
        McpMessageHandler handler = new McpMessageHandler(registry);
        McpJsonRpcMessage request = new McpJsonRpcMessage(
            "2.0", 
            "tools/call", 
            Map.of("name", "test_tool", "arguments", Map.of()), 
            1, 
            null, 
            null
        );
        
        McpJsonRpcMessage response = handler.handle(request);
        
        assertThat(response.result()).isEqualTo("success");
    }
}
