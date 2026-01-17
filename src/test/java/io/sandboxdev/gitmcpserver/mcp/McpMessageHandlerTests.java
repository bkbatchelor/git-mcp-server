package io.sandboxdev.gitmcpserver.mcp;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class McpMessageHandlerTests {

    @Test
    void shouldHandleUnknownMethodWithError() {
        McpMessageHandler handler = new McpMessageHandler(new ToolRegistry());
        McpJsonRpcMessage request = new McpJsonRpcMessage("2.0", "unknown_method", null, 1, null, null);
        
        McpJsonRpcMessage response = handler.handle(request);
        
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo(1);
        assertThat(response.error()).isNotNull();
        assertThat(response.result()).isNull();
    }
}
