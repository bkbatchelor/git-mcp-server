package io.sandboxdev.gitmcpserver.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StdioTransport {
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final McpMessageHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StdioTransport(InputStream in, OutputStream out, McpMessageHandler handler) {
        this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        this.handler = handler;
    }

    public void processOne() throws IOException {
        String line = reader.readLine();
        if (line != null && !line.isBlank()) {
            try {
                McpJsonRpcMessage request = McpJsonRpcParser.parse(line);
                McpJsonRpcMessage response = handler.handle(request);
                String responseJson = objectMapper.writeValueAsString(response);
                writer.write(responseJson);
                writer.newLine();
                writer.flush();
            } catch (Exception e) {
                // TODO: Handle parse errors with a proper JSON-RPC error response
            }
        }
    }
}
