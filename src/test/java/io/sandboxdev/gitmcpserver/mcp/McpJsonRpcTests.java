package io.sandboxdev.gitmcpserver.mcp;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class McpJsonRpcTests {

    @Test
    void shouldParseValidJsonRpcRequest() {
        String json = """
            {
                "jsonrpc": "2.0",
                "method": "list_tools",
                "id": 1
            }
            """;
        
        // This class and method do not exist yet, causing a compilation error (Red phase)
        McpJsonRpcMessage message = McpJsonRpcParser.parse(json);
        
        assertThat(message.jsonrpc()).isEqualTo("2.0");
        assertThat(message.method()).isEqualTo("list_tools");
        assertThat(message.id()).isEqualTo(1);
    }
}
