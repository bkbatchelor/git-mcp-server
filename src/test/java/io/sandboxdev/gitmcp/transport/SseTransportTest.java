package io.sandboxdev.gitmcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.config.GitMcpProperties;
import io.sandboxdev.gitmcp.protocol.McpJsonRpcDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
     Unit tests for SSE Transport implementation.
     
     Tests the basic functionality of the SSE transport including
     connection establishment, message processing, and CORS support.
 */
@ExtendWith(MockitoExtension.class)
class SseTransportTest {

    @Mock
    private McpJsonRpcDispatcher dispatcher;
    
    @Mock
    private GitMcpProperties properties;
    
    @Mock
    private GitMcpProperties.TransportConfig transportConfig;
    
    private ObjectMapper objectMapper;
    private SseTransport sseTransport;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Configure mock properties
        when(properties.transport()).thenReturn(transportConfig);
        when(transportConfig.ssePort()).thenReturn(8080);
        
        sseTransport = new SseTransport(objectMapper, dispatcher, properties);
    }

    @Test
    void shouldEstablishSseConnection() {
        // When establishing a connection
        SseEmitter emitter = sseTransport.establishConnection("test-session");
        
        // Then emitter should be created
        assertThat(emitter).isNotNull();
        assertThat(sseTransport.getActiveConnectionCount()).isEqualTo(1);
    }

    @Test
    void shouldEstablishSseConnectionWithGeneratedSessionId() {
        // When establishing a connection without session ID
        SseEmitter emitter = sseTransport.establishConnection(null);
        
        // Then emitter should be created with generated session ID
        assertThat(emitter).isNotNull();
        assertThat(sseTransport.getActiveConnectionCount()).isEqualTo(1);
    }

    @Test
    void shouldProcessRequestAndReturnAcceptedStatus() {
        // Given a valid JSON-RPC request
        String requestBody = "{\"jsonrpc\":\"2.0\",\"method\":\"test\",\"id\":\"1\"}";
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        
        // When processing the request
        var response = sseTransport.processRequest(requestBody, "test-session", headers);
        
        // Then should return accepted status
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("status")).isEqualTo("accepted");
        assertThat(responseBody.get("sessionId")).isEqualTo("test-session");
    }

    @Test
    void shouldReturnHealthCheckStatus() {
        // When checking health
        var response = sseTransport.healthCheck();
        
        // Then should return healthy status
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.get("status")).isEqualTo("healthy");
        assertThat(responseBody.get("transport")).isEqualTo("sse");
        assertThat(responseBody.get("activeConnections")).isEqualTo(0);
    }

    @Test
    void shouldTrackActiveConnections() {
        // When establishing multiple connections
        sseTransport.establishConnection("session-1");
        sseTransport.establishConnection("session-2");
        sseTransport.establishConnection("session-3");
        
        // Then should track all connections
        assertThat(sseTransport.getActiveConnectionCount()).isEqualTo(3);
    }

    @Test
    void shouldCloseAllConnections() {
        // Given multiple active connections
        sseTransport.establishConnection("session-1");
        sseTransport.establishConnection("session-2");
        assertThat(sseTransport.getActiveConnectionCount()).isEqualTo(2);
        
        // When closing all connections
        sseTransport.closeAllConnections();
        
        // Then all connections should be closed
        assertThat(sseTransport.getActiveConnectionCount()).isEqualTo(0);
    }
}