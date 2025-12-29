package io.sandboxdev.gitmcp.properties;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.logging.LoggingService;
import io.sandboxdev.gitmcp.logging.McpNotificationAppender;
import io.sandboxdev.gitmcp.model.McpNotification;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for logging behavior and MCP notifications.
 * Tests Property 13: Logging Behavior (Requirements 12.2, 12.3, 12.4, 12.5)
 */
class LoggingBehaviorProperties {

    private final LoggingService loggingService = new LoggingService(new McpNotificationAppender(new ObjectMapper()));

    /**
     Property 13: Logging Behavior (Req 12.2)
     All logs redirect to System.err when using Stdio transport
     */
    @Property
    void logsRedirectToSystemErrWithStdioTransport(@ForAll("logMessages") LogMessage logMessage) {
        // This test should fail initially - we need to implement Stdio log redirection
        boolean isRedirectedToStderr = checkLogRedirectionToStderr(logMessage);
        
        assertThat(isRedirectedToStderr).isTrue();
    }

    /**
     Property 13: Logging Behavior (Req 12.3)
     All log entries include TraceID and SpanID for request correlation
     */
    @Property
    void logEntriesIncludeTraceAndSpanIds(@ForAll("requestContexts") RequestContext context) {
        // This test should fail initially - we need to implement trace correlation
        setupRequestContext(context);
        
        logTestMessage("Test message for trace correlation");
        
        ILoggingEvent logEvent = getLastLogEvent();
        assertThat(logEvent).isNotNull();
        assertThat(logEvent.getMDCPropertyMap()).containsKey("traceId");
        assertThat(logEvent.getMDCPropertyMap()).containsKey("spanId");
        assertThat(logEvent.getMDCPropertyMap().get("traceId")).isEqualTo(context.traceId());
        assertThat(logEvent.getMDCPropertyMap().get("spanId")).isEqualTo(context.spanId());
    }

    /**
     Property 13: Logging Behavior (Req 12.4)
     Log events are forwarded as MCP notifications to the IDE
     */
    @Property
    void logEventsForwardedAsMcpNotifications(@ForAll("logLevels") String logLevel, 
                                            @ForAll("logMessages") LogMessage logMessage) {
        // This test should fail initially - we need to implement MCP notification forwarding
        McpNotification notification = forwardLogEventAsMcpNotification(logLevel, logMessage);
        
        assertThat(notification).isNotNull();
        assertThat(notification.method()).isEqualTo("notifications/message");
        assertThat(notification.params()).isNotNull();
        assertThat(notification.params().has("level")).isTrue();
        assertThat(notification.params().has("message")).isTrue();
        assertThat(notification.params().get("level").asText()).isEqualTo(logLevel);
    }

    /**
     Property 13: Logging Behavior (Req 12.5)
     Dynamic log level adjustment tool allows runtime configuration
     */
    @Property
    void dynamicLogLevelAdjustmentWorks(@ForAll("logLevels") String newLevel) {
        // This test should fail initially - we need to implement dynamic log level tool
        String originalLevel = getCurrentLogLevel();
        
        boolean adjustmentSuccessful = adjustLogLevelDynamically(newLevel);
        
        assertThat(adjustmentSuccessful).isTrue();
        assertThat(getCurrentLogLevel()).isEqualTo(newLevel);
        
        // Restore original level
        adjustLogLevelDynamically(originalLevel);
    }

    // Generators for property-based testing

    @Provide
    Arbitrary<LogMessage> logMessages() {
        return Arbitraries.oneOf(
                Arbitraries.just(new LogMessage("INFO", "Git operation completed successfully")),
                Arbitraries.just(new LogMessage("WARN", "Repository path not in allowlist")),
                Arbitraries.just(new LogMessage("ERROR", "Failed to execute git command")),
                Arbitraries.just(new LogMessage("DEBUG", "Processing MCP request")),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(10).ofMaxLength(100)
                        .map(msg -> new LogMessage("INFO", msg))
        );
    }

    @Provide
    Arbitrary<RequestContext> requestContexts() {
        return Arbitraries.oneOf(
                Arbitraries.just(new RequestContext("trace-123", "span-456")),
                Arbitraries.just(new RequestContext("trace-789", "span-012")),
                Arbitraries.strings().withCharRange('a', 'f').withCharRange('0', '9').ofLength(16)
                        .flatMap(traceId -> 
                                Arbitraries.strings().withCharRange('a', 'f').withCharRange('0', '9').ofLength(8)
                                        .map(spanId -> new RequestContext(traceId, spanId)))
        );
    }

    @Provide
    Arbitrary<String> logLevels() {
        return Arbitraries.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    }

    // Helper methods that now use LoggingService (GREEN phase)

    private boolean checkLogRedirectionToStderr(LogMessage logMessage) {
        return loggingService.isLogRedirectedToStderr();
    }

    private void setupRequestContext(RequestContext context) {
        loggingService.setupRequestContext(context.traceId(), context.spanId());
    }

    private void logTestMessage(String message) {
        org.slf4j.LoggerFactory.getLogger(this.getClass()).info(message);
    }

    private ILoggingEvent getLastLogEvent() {
        return loggingService.getLastLogEvent();
    }

    private McpNotification forwardLogEventAsMcpNotification(String logLevel, LogMessage logMessage) {
        return loggingService.forwardLogEventAsMcpNotification(logLevel, logMessage.message());
    }

    private String getCurrentLogLevel() {
        return loggingService.getCurrentLogLevel();
    }

    private boolean adjustLogLevelDynamically(String newLevel) {
        return loggingService.adjustLogLevelDynamically(newLevel);
    }

    // Test data records
    public record LogMessage(String level, String message) {}
    public record RequestContext(String traceId, String spanId) {}
}
