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
        @JsonProperty("uri") String uri,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("text") String text,
        @JsonProperty("metadata") Optional<Map<String, String>> metadata) {

    /**
     * Creates resource content without metadata.
     */
    public ResourceContent(String uri, String mimeType, String text) {
        this(uri, mimeType, text, Optional.empty());
    }

    /**
     * Creates resource content with metadata.
     */
    public ResourceContent(String uri, String mimeType, String text, Map<String, String> metadata) {
        this(uri, mimeType, text, Optional.of(metadata));
    }

    /**
     * Validates that this resource content is valid.
     */
    @JsonIgnore
    public boolean isValid() {
        return uri != null && !uri.isBlank() &&
                mimeType != null && !mimeType.isBlank() &&
                text != null;
    }
}