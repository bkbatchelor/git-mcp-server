package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

/**
 * Represents an MCP Resource definition.
 * 
 * Resources are read-only data sources that the MCP server exposes to clients.
 * Each resource has a URI, name, description, and MIME type.
 */
public record ResourceDefinition(
    @JsonProperty("uri") URI uri,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("mimeType") String mimeType
) {
    
    /**
     * Validates that this resource definition is complete and valid.
     */
    @JsonIgnore
    public boolean isValid() {
        return uri != null &&
               name != null && !name.isBlank() &&
               description != null && !description.isBlank() &&
               mimeType != null && !mimeType.isBlank();
    }
}