package io.sandboxdev.gitmcp.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MCP protocol data models to verify JSON serialization/deserialization.
 */
class McpProtocolModelsTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Test
    void mcpRequestSerializationWorks() throws JsonProcessingException {
        JsonNode params = objectMapper.readTree("{\"test\": \"value\"}");
        McpRequest request = new McpRequest("test_method", params, "123");
        
        String json = objectMapper.writeValueAsString(request);
        McpRequest deserialized = objectMapper.readValue(json, McpRequest.class);
        
        assertThat(deserialized).isEqualTo(request);
        assertThat(deserialized.jsonrpc()).isEqualTo("2.0");
        assertThat(deserialized.method()).isEqualTo("test_method");
        assertThat(deserialized.id()).isEqualTo("123");
        assertThat(deserialized.isValid()).isTrue();
    }

    @Test
    void mcpResponseSerializationWorks() throws JsonProcessingException {
        JsonNode result = objectMapper.readTree("{\"success\": true}");
        McpResponse response = McpResponse.success(result, "123");
        
        String json = objectMapper.writeValueAsString(response);
        McpResponse deserialized = objectMapper.readValue(json, McpResponse.class);
        
        assertThat(deserialized).isEqualTo(response);
        assertThat(deserialized.jsonrpc()).isEqualTo("2.0");
        assertThat(deserialized.id()).isEqualTo("123");
        assertThat(deserialized.isValid()).isTrue();
        assertThat(deserialized.isSuccess()).isTrue();
    }

    @Test
    void mcpNotificationSerializationWorks() throws JsonProcessingException {
        JsonNode params = objectMapper.readTree("{\"level\": \"info\"}");
        McpNotification notification = new McpNotification("log", params);
        
        String json = objectMapper.writeValueAsString(notification);
        McpNotification deserialized = objectMapper.readValue(json, McpNotification.class);
        
        assertThat(deserialized).isEqualTo(notification);
        assertThat(deserialized.jsonrpc()).isEqualTo("2.0");
        assertThat(deserialized.method()).isEqualTo("log");
        assertThat(deserialized.isValid()).isTrue();
    }

    @Test
    void toolDefinitionSerializationWorks() throws JsonProcessingException {
        JsonSchema schema = JsonSchema.stringSchema("Test parameter");
        ToolDefinition toolDef = new ToolDefinition("test_tool", "A test tool", schema);
        
        String json = objectMapper.writeValueAsString(toolDef);
        ToolDefinition deserialized = objectMapper.readValue(json, ToolDefinition.class);
        
        assertThat(deserialized).isEqualTo(toolDef);
        assertThat(deserialized.name()).isEqualTo("test_tool");
        assertThat(deserialized.description()).isEqualTo("A test tool");
        assertThat(deserialized.isValid()).isTrue();
    }

    @Test
    void toolResultSerializationWorks() throws JsonProcessingException {
        ToolResult result = ToolResult.success("Operation completed");
        
        String json = objectMapper.writeValueAsString(result);
        ToolResult deserialized = objectMapper.readValue(json, ToolResult.class);
        
        assertThat(deserialized).isEqualTo(result);
        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.isError()).isFalse();
    }

    @Test
    void resourceDefinitionSerializationWorks() throws JsonProcessingException {
        URI uri = URI.create("git-resource://test");
        ResourceDefinition resourceDef = new ResourceDefinition(uri, "test_resource", "A test resource", "text/plain");
        
        String json = objectMapper.writeValueAsString(resourceDef);
        ResourceDefinition deserialized = objectMapper.readValue(json, ResourceDefinition.class);
        
        assertThat(deserialized).isEqualTo(resourceDef);
        assertThat(deserialized.uri()).isEqualTo(uri);
        assertThat(deserialized.name()).isEqualTo("test_resource");
        assertThat(deserialized.isValid()).isTrue();
    }

    @Test
    void resourceContentSerializationWorks() throws JsonProcessingException {
        Map<String, String> metadata = Map.of("size", "100", "encoding", "utf-8");
        ResourceContent content = new ResourceContent("test content", "text/plain", metadata);
        
        String json = objectMapper.writeValueAsString(content);
        ResourceContent deserialized = objectMapper.readValue(json, ResourceContent.class);
        
        assertThat(deserialized).isEqualTo(content);
        assertThat(deserialized.content()).isEqualTo("test content");
        assertThat(deserialized.mimeType()).isEqualTo("text/plain");
        assertThat(deserialized.metadata()).isPresent();
        assertThat(deserialized.isValid()).isTrue();
    }
}