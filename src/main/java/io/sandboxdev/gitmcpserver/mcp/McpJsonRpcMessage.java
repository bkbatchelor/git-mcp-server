package io.sandboxdev.gitmcpserver.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record McpJsonRpcMessage(
    String jsonrpc,
    String method,
    Object params,
    Object id,
    Object result,
    Object error
) {}