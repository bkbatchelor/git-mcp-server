package io.sandboxdev.gitmcpserver.mcp;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class StdioTransportTests {

    @Test
    void shouldReadProcessAndWrite() throws Exception {
        String inputJson = "{\"jsonrpc\": \"2.0\", \"method\": \"test\", \"id\": 1}\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        McpMessageHandler handler = mock(McpMessageHandler.class);
        McpJsonRpcMessage responseMsg = new McpJsonRpcMessage("2.0", null, null, 1, "success", null);
        when(handler.handle(any())).thenReturn(responseMsg);
        
        StdioTransport transport = new StdioTransport(inputStream, outputStream, handler);
        transport.processOne(); 
        
        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("success");
    }
}
