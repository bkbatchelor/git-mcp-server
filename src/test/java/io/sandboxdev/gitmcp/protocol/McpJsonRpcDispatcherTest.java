package io.sandboxdev.gitmcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Unit tests for McpJsonRpcDispatcher to verify JSON-RPC protocol compliance.
 */
class McpJsonRpcDispatcherTest {

    private McpJsonRpcDispatcher dispatcher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dispatcher = new McpJsonRpcDispatcher(objectMapper);
    }

    @Test
    void validRequestReturnsSuccessResponse() throws Exception {
        JsonNode params = objectMapper.readTree("{\"test\": \"value\"}");
        McpRequest request = new McpRequest("tools/list", params, "123");
        
        McpResponse response = dispatcher.dispatch(request, null);
        
        assertThat(response).isNotNull();
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo("123");
        assertThat(response.isValid()).isTrue();
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void invalidJsonRpcVersionReturnsError() throws Exception {
        JsonNode params = objectMapper.readTree("{\"test\": \"value\"}");
        McpRequest request = new McpRequest("1.0", "tools/list", params, "123");
        
        McpResponse response = dispatcher.dispatch(request, null);
        
        assertThat(response).isNotNull();
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.id()).isEqualTo("123");
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.INVALID_REQUEST);
    }

    @Test
    void emptyMethodNameReturnsError() throws Exception {
        JsonNode params = objectMapper.readTree("{\"test\": \"value\"}");
        McpRequest request = new McpRequest("", params, "123");
        
        McpResponse response = dispatcher.dispatch(request, null);
        
        assertThat(response).isNotNull();
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.INVALID_REQUEST);
    }

    @Test
    void unknownMethodReturnsMethodNotFoundError() throws Exception {
        JsonNode params = objectMapper.readTree("{\"test\": \"value\"}");
        McpRequest request = new McpRequest("unknown/method", params, "123");
        
        McpResponse response = dispatcher.dispatch(request, null);
        
        assertThat(response).isNotNull();
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.METHOD_NOT_FOUND);
        assertThat(response.error().message()).contains("unknown/method");
    }

    @Test
    void malformedJsonReturnsParseError() {
        String malformedJson = "{invalid json";
        
        McpResponse response = dispatcher.handleMalformedJson(malformedJson);
        
        assertThat(response).isNotNull();
        assertThat(response.jsonrpc()).isEqualTo("2.0");
        assertThat(response.isError()).isTrue();
        assertThat(response.error().code()).isEqualTo(McpError.PARSE_ERROR);
    }

    @Test
    void responsesMaintainRequestIdCorrelation() throws Exception {
        JsonNode params = objectMapper.readTree("{\"test\": \"value\"}");
        McpRequest request = new McpRequest("tools/list", params, "correlation-test-123");
        
        McpResponse response = dispatcher.dispatch(request, null);
        
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("correlation-test-123");
    }

    @Test
    void validNotificationIsProcessedWithoutError() throws Exception {
        JsonNode params = objectMapper.readTree("{\"level\": \"info\", \"message\": \"test log\"}");
        McpNotification notification = new McpNotification("notifications/message", params);
        
        // Should not throw exception
        dispatcher.handleNotification(notification, null);
        
        // Notification processing is fire-and-forget, so we just verify no exception
        assertThat(notification.isValid()).isTrue();
    }

    @Test
    void invalidNotificationIsIgnored() {
        McpNotification notification = new McpNotification("", null);
        
        // Should not throw exception even with invalid notification
        dispatcher.handleNotification(notification, null);
        
        assertThat(notification.isValid()).isFalse();
    }
}