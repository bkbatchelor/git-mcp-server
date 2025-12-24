package io.sandboxdev.gitmcp.transport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.McpMessage;
import io.sandboxdev.gitmcp.model.McpRequest;
import io.sandboxdev.gitmcp.protocol.McpJsonRpcDispatcher;
import io.sandboxdev.gitmcp.protocol.MessageContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stdio transport implementation for MCP JSON-RPC communication.
 * 
 * This transport reads JSON-RPC messages from System.in and writes responses to System.out,
 * while ensuring all application logs are redirected to System.err to prevent protocol corruption.
 * 
 * Uses Virtual Threads for non-blocking I/O operations.
 */
@Component
public class StdioTransport {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StdioTransport.class);
    
    private final ObjectMapper objectMapper;
    private final McpJsonRpcDispatcher dispatcher;
    private final ExecutorService virtualThreadExecutor;
    private final PrintWriter outputWriter;
    private volatile boolean running = false;

    @Autowired
    public StdioTransport(ObjectMapper objectMapper, McpJsonRpcDispatcher dispatcher) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.outputWriter = new PrintWriter(System.out, true);
        
        // Configure logging immediately upon construction
        configureLogging();
    }

    // Constructor for testing
    public StdioTransport() {
        this.objectMapper = new ObjectMapper();
        this.dispatcher = new McpJsonRpcDispatcher();
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.outputWriter = new PrintWriter(System.out, true);
        
        // Configure logging immediately upon construction
        configureLogging();
    }

    /**
     * Configures Logback to redirect all logs to System.err.
     * This is critical for Stdio transport to prevent protocol corruption.
     */
    public void configureLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Clear existing appenders
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        
        // Create console appender that targets System.err
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("STDERR");
        consoleAppender.setTarget("System.err");
        
        // Create pattern encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();
        
        // Attach appender to root logger
        rootLogger.addAppender(consoleAppender);
        
        logger.info("Stdio transport logging configured - all logs redirected to System.err");
    }

    /**
     * Starts the Stdio transport, reading messages from System.in using Virtual Threads.
     */
    public CompletableFuture<Void> start() {
        if (running) {
            throw new IllegalStateException("Stdio transport is already running");
        }
        
        running = true;
        logger.info("Starting Stdio transport with Virtual Threads");
        
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    final String message = line.trim();
                    if (!message.isEmpty()) {
                        // Process each message in a separate Virtual Thread
                        virtualThreadExecutor.submit(() -> processMessage(message));
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading from System.in", e);
                throw new RuntimeException("Stdio transport failed", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Stops the Stdio transport.
     */
    public void stop() {
        logger.info("Stopping Stdio transport");
        running = false;
        virtualThreadExecutor.shutdown();
    }

    /**
     * Sends a JSON-RPC message to System.out.
     * This method is used for sending responses and notifications.
     */
    public void sendMessage(String jsonMessage) {
        synchronized (outputWriter) {
            outputWriter.println(jsonMessage);
            outputWriter.flush();
        }
    }

    /**
     * Sends an MCP message object to System.out after serializing to JSON.
     */
    public void sendMessage(McpMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sendMessage(json);
        } catch (Exception e) {
            logger.error("Failed to serialize MCP message", e);
        }
    }

    /**
     * Processes an incoming JSON-RPC message using Virtual Threads.
     */
    private void processMessage(String jsonMessage) {
        try {
            logger.debug("Processing message: {}", jsonMessage);
            
            // Parse the JSON-RPC message
            McpRequest request = objectMapper.readValue(jsonMessage, McpRequest.class);
            
            // Create message context
            MessageContext context = MessageContext.create();
            
            // Dispatch the request
            var response = dispatcher.dispatch(request, context);
            
            // Send response back
            if (response != null) {
                sendMessage(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", jsonMessage, e);
            
            // Send error response for malformed JSON
            try {
                var errorResponse = dispatcher.handleMalformedJson(jsonMessage);
                sendMessage(errorResponse);
            } catch (Exception errorException) {
                logger.error("Failed to send error response", errorException);
            }
        }
    }

    /**
     * Returns true if the transport is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns true if the executor is using Virtual Threads.
     */
    public boolean isUsingVirtualThreads() {
        String className = virtualThreadExecutor.getClass().getSimpleName();
        return className.equals("ThreadPerTaskExecutor");
    }

    /**
     * Returns the executor service for introspection.
     */
    public ExecutorService getExecutor() {
        return virtualThreadExecutor;
    }
}