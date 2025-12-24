package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for MCP protocol serialization and deserialization.
 *
 * These tests verify that all MCP protocol data models can be correctly
 * serialized to JSON and deserialized back to equivalent objects.
 */
@Tag("git-mcp-server")
@Tag("property-12")
@Tag("schema-serialization")
class McpProtocolSerializationProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void mcpRequestSerializationRoundTrip(@ForAll("mcpRequests") McpRequest request) {
        // This test should fail initially as McpRequest doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(request);
            McpRequest deserialized = objectMapper.readValue(json, McpRequest.class);

            assertThat(deserialized).isEqualTo(request);
            assertThat(deserialized.jsonrpc()).isEqualTo("2.0");
            assertThat(deserialized.method()).isNotBlank();
            assertThat(deserialized.id()).isNotNull();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void mcpResponseSerializationRoundTrip(@ForAll("mcpResponses") McpResponse response) {
        // This test should fail initially as McpResponse doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(response);
            McpResponse deserialized = objectMapper.readValue(json, McpResponse.class);

            assertThat(deserialized).isEqualTo(response);
            assertThat(deserialized.jsonrpc()).isEqualTo("2.0");
            assertThat(deserialized.id()).isNotNull();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void mcpNotificationSerializationRoundTrip(@ForAll("mcpNotifications") McpNotification notification) {
        // This test should fail initially as McpNotification doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(notification);
            McpNotification deserialized = objectMapper.readValue(json, McpNotification.class);

            assertThat(deserialized).isEqualTo(notification);
            assertThat(deserialized.jsonrpc()).isEqualTo("2.0");
            assertThat(deserialized.method()).isNotBlank();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void toolDefinitionSerializationRoundTrip(@ForAll("toolDefinitions") ToolDefinition toolDef) {
        // This test should fail initially as ToolDefinition doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(toolDef);
            ToolDefinition deserialized = objectMapper.readValue(json, ToolDefinition.class);

            assertThat(deserialized).isEqualTo(toolDef);
            assertThat(deserialized.name()).isNotBlank();
            assertThat(deserialized.description()).isNotBlank();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void toolResultSerializationRoundTrip(@ForAll("toolResults") ToolResult toolResult) {
        // This test should fail initially as ToolResult doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(toolResult);
            ToolResult deserialized = objectMapper.readValue(json, ToolResult.class);

            assertThat(deserialized).isEqualTo(toolResult);
            assertThat(deserialized.content()).isNotNull();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void resourceDefinitionSerializationRoundTrip(@ForAll("resourceDefinitions") ResourceDefinition resourceDef) {
        // This test should fail initially as ResourceDefinition doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(resourceDef);
            ResourceDefinition deserialized = objectMapper.readValue(json, ResourceDefinition.class);

            assertThat(deserialized).isEqualTo(resourceDef);
            assertThat(deserialized.name()).isNotBlank();
            assertThat(deserialized.description()).isNotBlank();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @Property
    @Tag("property-12")
    @Tag("schema-serialization")
    void resourceContentSerializationRoundTrip(@ForAll("resourceContents") ResourceContent resourceContent) {
        // This test should fail initially as ResourceContent doesn't exist yet
        try {
            String json = objectMapper.writeValueAsString(resourceContent);
            ResourceContent deserialized = objectMapper.readValue(json, ResourceContent.class);

            assertThat(deserialized).isEqualTo(resourceContent);
            assertThat(deserialized.content()).isNotNull();
            assertThat(deserialized.mimeType()).isNotBlank();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    // Generators for test data
    @Provide
    Arbitrary<McpRequest> mcpRequests() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1),
            jsonNodes(),
            Arbitraries.strings().alpha().ofMinLength(1)
        ).as((method, params, id) -> new McpRequest("2.0", method, params, id));
    }

    @Provide
    Arbitrary<McpResponse> mcpResponses() {
        return Combinators.combine(
            jsonNodes().optional(),
            mcpErrors().optional(),
            Arbitraries.strings().alpha().ofMinLength(1)
        ).as((result, error, id) -> new McpResponse("2.0", result.orElse(null), error.orElse(null), id));
    }

    @Provide
    Arbitrary<McpNotification> mcpNotifications() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1),
            jsonNodes()
        ).as((method, params) -> new McpNotification("2.0", method, params));
    }

    @Provide
    Arbitrary<ToolDefinition> toolDefinitions() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1),
            Arbitraries.strings().alpha().ofMinLength(1),
            jsonSchemas()
        ).as(ToolDefinition::new);
    }

    @Provide
    Arbitrary<ToolResult> toolResults() {
        return Combinators.combine(
            jsonNodes(),
            Arbitraries.of(Boolean.TRUE, Boolean.FALSE),
            Arbitraries.strings().alpha().optional()
        ).as((content, isError, errorMessage) -> new ToolResult(content, isError.booleanValue(), errorMessage));
    }

    @Provide
    Arbitrary<ResourceDefinition> resourceDefinitions() {
        return Combinators.combine(
            uris(),
            Arbitraries.strings().alpha().ofMinLength(1),
            Arbitraries.strings().alpha().ofMinLength(1),
            Arbitraries.strings().alpha().ofMinLength(1)
        ).as(ResourceDefinition::new);
    }

    @Provide
    Arbitrary<ResourceContent> resourceContents() {
        return Combinators.combine(
            Arbitraries.strings().alpha(),
            Arbitraries.strings().alpha().ofMinLength(1),
            stringMaps().optional()
        ).as(ResourceContent::new);
    }

    @Provide
    Arbitrary<McpError> mcpErrors() {
        return Combinators.combine(
            Arbitraries.integers(),
            Arbitraries.strings().alpha().ofMinLength(1),
            jsonNodes().optional()
        ).as((code, message, data) -> new McpError(code, message, data.orElse(null)));
    }

    @Provide
    Arbitrary<JsonNode> jsonNodes() {
        return Arbitraries.strings().alpha().map(s -> {
            try {
                return objectMapper.readTree("\"" + s + "\"");
            } catch (JsonProcessingException e) {
                return objectMapper.createObjectNode().put("value", s);
            }
        });
    }

    @Provide
    Arbitrary<JsonSchema> jsonSchemas() {
        return Arbitraries.strings().alpha().map(s -> {
            try {
                return objectMapper.readValue("{\"type\":\"string\"}", JsonSchema.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Provide
    Arbitrary<java.net.URI> uris() {
        return Arbitraries.strings().alpha().ofMinLength(1)
            .map(s -> java.net.URI.create("git-resource://" + s));
    }

    @Provide
    Arbitrary<Map<String, String>> stringMaps() {
        return Arbitraries.maps(
            Arbitraries.strings().alpha().ofMinLength(1),
            Arbitraries.strings().alpha()
        );
    }
}