package io.sandboxdev.gitmcp.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.McpNotification;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Custom SLF4J Appender that forwards log events to MCP Host via notifications/message.
 * Implements Requirements 12.4 - MCP Protocol Logging Bridge
 */
@Component
public class McpNotificationAppender extends AppenderBase<ILoggingEvent> {

    private final ObjectMapper objectMapper;
    private McpNotificationHandler notificationHandler;

    public McpNotificationAppender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.setName("McpNotificationAppender");
        this.start();
    }

    public void setNotificationHandler(McpNotificationHandler handler) {
        this.notificationHandler = handler;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (notificationHandler != null) {
            McpNotification notification = createMcpNotification(event);
            notificationHandler.sendNotification(notification);
        }
    }

    private McpNotification createMcpNotification(ILoggingEvent event) {
        try {
            JsonNode params = objectMapper.createObjectNode()
                    .put("level", mapLogLevel(event.getLevel()))
                    .put("message", event.getFormattedMessage())
                    .put("timestamp", event.getTimeStamp())
                    .put("logger", event.getLoggerName());

            return new McpNotification("2.0", "notifications/message", params);
        } catch (Exception e) {
            // Fallback to simple notification
            JsonNode params = objectMapper.createObjectNode()
                    .put("level", "ERROR")
                    .put("message", "Failed to create log notification");
            return new McpNotification("2.0", "notifications/message", params);
        }
    }

    private String mapLogLevel(Level level) {
        return switch (level.levelInt) {
            case Level.TRACE_INT -> "TRACE";
            case Level.DEBUG_INT -> "DEBUG";
            case Level.INFO_INT -> "INFO";
            case Level.WARN_INT -> "WARN";
            case Level.ERROR_INT -> "ERROR";
            default -> "INFO";
        };
    }

    /**
     * Helper method for testing - creates MCP notification without actual logging event
     */
    public McpNotification createMcpNotificationForTesting(String level, String message) {
        try {
            JsonNode params = objectMapper.createObjectNode()
                    .put("level", level)
                    .put("message", message)
                    .put("timestamp", System.currentTimeMillis())
                    .put("logger", "test");

            return new McpNotification("2.0", "notifications/message", params);
        } catch (Exception e) {
            JsonNode params = objectMapper.createObjectNode()
                    .put("level", "ERROR")
                    .put("message", "Failed to create test notification");
            return new McpNotification("2.0", "notifications/message", params);
        }
    }

    public interface McpNotificationHandler {
        void sendNotification(McpNotification notification);
    }
}
