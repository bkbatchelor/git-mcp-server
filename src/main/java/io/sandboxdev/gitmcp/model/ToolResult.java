package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Represents the result of executing an MCP Tool.
 * 
 * Tool results contain the output content from the tool execution,
 * along with metadata indicating success or failure.
 */
public record ToolResult(
    @JsonProperty("content") JsonNode content,
    @JsonProperty("isError") boolean isError,
    @JsonProperty("errorMessage") Optional<String> errorMessage
) {
    
    /**
     * Creates a successful tool result.
     */
    public static ToolResult success(JsonNode content) {
        return new ToolResult(content, false, Optional.empty());
    }
    
    /**
     * Creates a successful tool result with string content.
     */
    public static ToolResult success(String content) {
        return success(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.textNode(content));
    }
    
    /**
     * Creates an error tool result.
     */
    public static ToolResult error(String errorMessage) {
        return new ToolResult(
            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.textNode(errorMessage),
            true,
            Optional.of(errorMessage)
        );
    }
    
    /**
     * Creates an error tool result with additional content.
     */
    public static ToolResult error(String errorMessage, JsonNode content) {
        return new ToolResult(content, true, Optional.of(errorMessage));
    }
    
    /**
     * Returns true if this result represents a successful operation.
     */
    @JsonIgnore
    public boolean isSuccess() {
        return !isError;
    }
}