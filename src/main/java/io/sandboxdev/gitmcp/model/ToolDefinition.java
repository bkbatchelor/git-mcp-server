package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an MCP Tool definition.
 * 
 * Tools are functions that the MCP server exposes to clients (LLMs).
 * Each tool has a name, description, and input schema that defines
 * the expected parameters.
 */
public record ToolDefinition(
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("inputSchema") JsonSchema inputSchema
) {
    
    /**
     * Validates that this tool definition is complete and valid.
     */
    @JsonIgnore
    public boolean isValid() {
        return name != null && !name.isBlank() &&
               description != null && !description.isBlank() &&
               inputSchema != null;
    }
}