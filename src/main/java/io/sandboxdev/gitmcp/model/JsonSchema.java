package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a JSON Schema definition for MCP Tool input validation.
 * 
 * This record provides a simplified representation of JSON Schema
 * that covers the most common use cases for MCP tool parameter validation.
 */
public record JsonSchema(
    @JsonProperty("type") String type,
    @JsonProperty("properties") Optional<Map<String, JsonSchema>> properties,
    @JsonProperty("required") Optional<String[]> required,
    @JsonProperty("description") Optional<String> description,
    @JsonProperty("additionalProperties") Optional<Boolean> additionalProperties
) {
    
    /**
     * Creates a simple string schema.
     */
    public static JsonSchema stringSchema() {
        return new JsonSchema("string", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    
    /**
     * Creates a simple string schema with description.
     */
    public static JsonSchema stringSchema(String description) {
        return new JsonSchema("string", Optional.empty(), Optional.empty(), Optional.of(description), Optional.empty());
    }
    
    /**
     * Creates a simple integer schema.
     */
    public static JsonSchema integerSchema() {
        return new JsonSchema("integer", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    
    /**
     * Creates a simple boolean schema.
     */
    public static JsonSchema booleanSchema() {
        return new JsonSchema("boolean", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    
    /**
     * Creates an object schema with properties.
     */
    public static JsonSchema objectSchema(Map<String, JsonSchema> properties) {
        return new JsonSchema("object", Optional.of(properties), Optional.empty(), Optional.empty(), Optional.of(false));
    }
    
    /**
     * Creates an object schema with properties and required fields.
     */
    public static JsonSchema objectSchema(Map<String, JsonSchema> properties, String[] required) {
        return new JsonSchema("object", Optional.of(properties), Optional.of(required), Optional.empty(), Optional.of(false));
    }
    
    /**
     * Validates that this schema is valid.
     */
    @JsonIgnore
    public boolean isValid() {
        return type != null && !type.isBlank();
    }
}