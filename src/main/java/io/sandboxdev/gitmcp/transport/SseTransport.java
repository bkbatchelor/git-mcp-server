package io.sandboxdev.gitmcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.config.GitMcpProperties;
import io.sandboxdev.gitmcp.model.McpMessage;
import io.sandboxdev.gitmcp.model.McpRequest;
import io.sandboxdev.gitmcp.protocol.McpJsonRpcDispatcher;
import io.sandboxdev.gitmcp.protocol.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server-Sent Events (SSE) transport implementation for MCP JSON-RPC communication.
 * 
 * This transport provides HTTP-based communication using Server-Sent Events,
 * enabling remote MCP clients to connect over HTTP with CORS support.
 * 
 * Uses Virtual Threads for async processing and maintains active SSE connections.
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "git.mcp.transport.sse-enabled", havingValue = "true")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SseTransport {

    private static final Logger logger = LoggerFactory.getLogger(SseTransport.class);
    
    private final ObjectMapper objectMapper;
    private final McpJsonRpcDispatcher dispatcher;
    private final GitMcpProperties properties;
    private final ExecutorService virtualThreadExecutor;
    
    // Active SSE connections mapped by session ID
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();

    public SseTransport(ObjectMapper objectMapper, 
                       McpJsonRpcDispatcher dispatcher,
                       GitMcpProperties properties) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        logger.info("SSE Transport initialized on port {}", properties.transport().ssePort());
    }

    /**
     * Establishes an SSE connection for MCP communication.
     * 
     * @param sessionId Optional session identifier for connection tracking
     * @return SseEmitter for the established connection
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter establishConnection(@RequestParam(required = false) String sessionId) {
        
        // Generate session ID if not provided
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session-" + System.currentTimeMillis();
        }
        
        logger.info("Establishing SSE connection for session: {}", sessionId);
        
        // Create SSE emitter with timeout from configuration
        long timeoutMs = properties.transport().requestTimeout().toMillis() * 10; // 10x request timeout for connection
        SseEmitter emitter = new SseEmitter(timeoutMs);
        
        // Store the connection
        activeConnections.put(sessionId, emitter);
        
        // Set up connection lifecycle handlers
        final String finalSessionId = sessionId;
        
        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for session: {}", finalSessionId);
            activeConnections.remove(finalSessionId);
        });
        
        emitter.onTimeout(() -> {
            logger.warn("SSE connection timed out for session: {}", finalSessionId);
            activeConnections.remove(finalSessionId);
        });
        
        emitter.onError((throwable) -> {
            logger.error("SSE connection error for session: {}", finalSessionId, throwable);
            activeConnections.remove(finalSessionId);
        });
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                .name("connection")
                .data("{\"status\":\"connected\",\"sessionId\":\"" + sessionId + "\"}")
                .build());
        } catch (IOException e) {
            logger.error("Failed to send connection confirmation", e);
            activeConnections.remove(sessionId);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    /**
     * Receives MCP JSON-RPC requests via HTTP POST and processes them asynchronously.
     * 
     * @param requestBody The JSON-RPC request body
     * @param sessionId Optional session identifier
     * @return ResponseEntity with processing status
     */
    @PostMapping(value = "/request", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> processRequest(
            @RequestBody String requestBody,
            @RequestParam(required = false) String sessionId,
            @RequestHeader Map<String, String> headers) {
        
        logger.debug("Received MCP request via SSE transport: sessionId={}", sessionId);
        
        // Process the request asynchronously using Virtual Threads
        virtualThreadExecutor.submit(() -> {
            try {
                processMessageAsync(requestBody, sessionId, headers);
            } catch (Exception e) {
                logger.error("Error processing async MCP request", e);
                sendErrorToSession(sessionId, "Internal server error: " + e.getMessage());
            }
        });
        
        // Return immediate acknowledgment
        return ResponseEntity.ok(Map.of(
            "status", "accepted",
            "message", "Request queued for processing",
            "sessionId", sessionId != null ? sessionId : "unknown"
        ));
    }

    /**
     * Health check endpoint for SSE transport.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "transport", "sse",
            "activeConnections", activeConnections.size(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Processes an MCP message asynchronously and sends the response via SSE.
     */
    private void processMessageAsync(String jsonMessage, String sessionId, Map<String, String> headers) {
        try {
            logger.debug("Processing async message: sessionId={}", sessionId);
            
            // Parse the JSON-RPC message
            McpRequest request = objectMapper.readValue(jsonMessage, McpRequest.class);
            
            // Create message context with headers and session
            MessageContext context = new MessageContext(
                sessionId != null ? java.util.Optional.of(sessionId) : java.util.Optional.empty(),
                java.util.Optional.empty(),
                headers != null ? Map.copyOf(headers) : Map.of(),
                System.currentTimeMillis()
            );
            
            // Dispatch the request
            var response = dispatcher.dispatch(request, context);
            
            // Send response via SSE
            if (response != null) {
                sendMessageToSession(sessionId, response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing async message: sessionId={}, message={}", sessionId, jsonMessage, e);
            
            // Send error response via SSE
            try {
                var errorResponse = dispatcher.handleMalformedJson(jsonMessage);
                sendMessageToSession(sessionId, errorResponse);
            } catch (Exception errorException) {
                logger.error("Failed to send error response via SSE", errorException);
                sendErrorToSession(sessionId, "Failed to process request: " + e.getMessage());
            }
        }
    }

    /**
     * Sends an MCP message to a specific session via SSE.
     */
    private void sendMessageToSession(String sessionId, McpMessage message) {
        if (sessionId == null) {
            logger.warn("Cannot send message - no session ID provided");
            return;
        }
        
        SseEmitter emitter = activeConnections.get(sessionId);
        if (emitter == null) {
            logger.warn("No active SSE connection found for session: {}", sessionId);
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            emitter.send(SseEmitter.event()
                .name("mcp-response")
                .data(json)
                .build());
            
            String sessionIdForLog = sessionId != null ? sessionId : "unknown";
            logger.debug("Sent MCP response via SSE to session: {}", sessionIdForLog);
            
        } catch (IOException e) {
            String sessionIdForLog = sessionId != null ? sessionId : "unknown";
            logger.error("Failed to send message via SSE to session: {}", sessionIdForLog, e);
            // Remove the failed connection
            activeConnections.remove(sessionId);
            emitter.completeWithError(e);
        }
    }

    /**
     * Sends an error message to a specific session via SSE.
     */
    private void sendErrorToSession(String sessionId, String errorMessage) {
        if (sessionId == null) {
            logger.warn("Cannot send error - no session ID provided");
            return;
        }
        
        SseEmitter emitter = activeConnections.get(sessionId);
        if (emitter == null) {
            logger.warn("No active SSE connection found for session: {}", sessionId);
            return;
        }
        
        try {
            Map<String, Object> errorData = Map.of(
                "error", true,
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );
            
            String json = objectMapper.writeValueAsString(errorData);
            emitter.send(SseEmitter.event()
                .name("error")
                .data(json)
                .build());
            
        } catch (IOException e) {
            String sessionIdForLog = sessionId != null ? sessionId : "unknown";
            logger.error("Failed to send error via SSE to session: {}", sessionIdForLog, e);
            activeConnections.remove(sessionId);
            emitter.completeWithError(e);
        }
    }

    /**
     * Broadcasts a message to all active SSE connections.
     */
    public void broadcastMessage(McpMessage message) {
        if (activeConnections.isEmpty()) {
            logger.debug("No active SSE connections for broadcast");
            return;
        }
        
        logger.debug("Broadcasting message to {} active connections", activeConnections.size());
        
        activeConnections.forEach((sessionId, emitter) -> {
            virtualThreadExecutor.submit(() -> sendMessageToSession(sessionId, message));
        });
    }

    /**
     * Returns the number of active SSE connections.
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * Closes all active SSE connections.
     */
    public void closeAllConnections() {
        logger.info("Closing {} active SSE connections", activeConnections.size());
        
        activeConnections.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.warn("Error closing SSE connection", e);
            }
        });
        
        activeConnections.clear();
        virtualThreadExecutor.shutdown();
    }
}