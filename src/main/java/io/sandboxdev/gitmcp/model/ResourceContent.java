package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

/**
 * Represents the content of an MCP Resource.
 * 
 * Resource content contains the actual data from the resource,
 * along with its MIME type and optional metadata.
 */
public record ResourceContent(
    @JsonProperty("content") String content,
    @JsonProperty("mimeType") String mimeType,
    @JsonProperty("metadata") Optional<Map<String, String>> metadata
) {
    
    /**
     * Creates resource content without metadata.
     */
    public ResourceContent(String content, String mimeType) {
        this(content, mimeType, Optional.empty());
    }
    
    /**
     * Creates resource content with metadata.
     */
    public ResourceContent(String content, String mimeType, Map<String, String> metadata) {
        this(content, mimeType, Optional.of(metadata));
    }
    
    /**
     * Validates that this resource content is valid.
     */
    @JsonIgnore
    public boolean isValid() {
        return content != null &&
               mimeType != null && !mimeType.isBlank();
    }
}