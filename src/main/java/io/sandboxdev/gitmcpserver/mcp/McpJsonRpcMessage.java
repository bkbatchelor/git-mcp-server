package io.sandboxdev.gitmcpserver.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpJsonRpcMessage(
    String jsonrpc,
    String method,
    Object params,
    Object id,
    Object result,
    Object error
) {}