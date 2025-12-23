package io.sandboxdev.gitmcp.logging;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Custom Logback appender that forwards log events to the MCP Host (IDE) 
 * via MCP notifications/message capability.
 * 
 * This appender allows the IDE to display agent logs in its UI by converting
 * SLF4J log events into MCP notification messages.
 * 
 * Implementation will be completed in subsequent tasks when the MCP protocol
 * infrastructure is available.
 */
public class McpNotificationAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        // TODO: Implementation will be added in task 11.4
        // This will forward log events to the MCP Host via notifications/message
        
        // For now, this is a placeholder to prevent logback configuration errors
        // The actual implementation will:
        // 1. Format the log event according to MCP notification schema
        // 2. Send the notification to the MCP Host
        // 3. Handle any errors gracefully to prevent logging loops
    }
}