package io.sandboxdev.gitmcp.properties;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import io.sandboxdev.gitmcp.transport.StdioTransport;
import net.jqwik.api.*;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Property-based tests for Stdio transport logging isolation.
     
     These tests verify that when Stdio transport is configured, all application logs
     are redirected to System.err while JSON-RPC messages use System.out exclusively.
     
     Feature: git-mcp-server, Property 2: Stdio Transport Logging Isolation
     Validates: Requirements 2.3
 */
class StdioTransportLoggingProperties {

    @Property
    void stdioTransportRedirectsLogsToSystemErr(@ForAll("logMessages") String logMessage) {
        // Capture System.out and System.err
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            System.setOut(new PrintStream(capturedOut));
            System.setErr(new PrintStream(capturedErr));
            
            // Create StdioTransport instance
            StdioTransport transport = new StdioTransport();
            
            // Configure logging to use the transport's configuration
            transport.configureLogging();
            
            // Log a message using SLF4J
            org.slf4j.Logger logger = LoggerFactory.getLogger("test.logger");
            logger.info(logMessage);
            
            // Verify that the log message went to System.err, not System.out
            String stdoutContent = capturedOut.toString();
            String stderrContent = capturedErr.toString();
            
            assertThat(stdoutContent).isEmpty(); // No logs should go to stdout
            assertThat(stderrContent).contains(logMessage); // Log should be in stderr
            
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Property
    void stdioTransportPreservesSystemOutForJsonRpc(@ForAll("jsonRpcMessages") String jsonRpcMessage) {
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            System.setOut(new PrintStream(capturedOut));
            System.setErr(new PrintStream(capturedErr));
            
            StdioTransport transport = new StdioTransport();
            transport.configureLogging();
            
            // Send JSON-RPC message to System.out (this should work)
            transport.sendMessage(jsonRpcMessage);
            
            // Also log something via SLF4J
            org.slf4j.Logger logger = LoggerFactory.getLogger("test.logger");
            logger.warn("This should go to stderr");
            
            String stdoutContent = capturedOut.toString();
            String stderrContent = capturedErr.toString();
            
            // JSON-RPC message should be in stdout
            assertThat(stdoutContent).contains(jsonRpcMessage);
            // SLF4J log should be in stderr
            assertThat(stderrContent).contains("This should go to stderr");
            // SLF4J log should NOT be in stdout
            assertThat(stdoutContent).doesNotContain("This should go to stderr");
            
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Property
    void allLogbackAppendersRedirectToSystemErr() {
        StdioTransport transport = new StdioTransport();
        transport.configureLogging();
        
        // Get the root logger context
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        
        // Check all appenders to ensure they target System.err
        Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        
        boolean hasConsoleAppender = false;
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if (appender instanceof ConsoleAppender) {
                hasConsoleAppender = true;
                ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) appender;
                
                // Verify that console appender targets System.err
                assertThat(consoleAppender.getTarget()).isEqualTo("System.err");
            }
        }
        
        assertThat(hasConsoleAppender).isTrue();
    }

    @Example
    void springBootStartupLogsRedirectToSystemErr() {
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        
        try {
            System.setErr(new PrintStream(capturedErr));
            
            StdioTransport transport = new StdioTransport();
            
            String stderrContent = capturedErr.toString();
            
            // Verify that StdioTransport logs go to stderr (this proves the redirection works)
            assertThat(stderrContent).contains("Stdio transport logging configured - all logs redirected to System.err");
            
        } finally {
            System.setErr(originalErr);
        }
    }

    // Generators for test data
    @Provide
    Arbitrary<String> logMessages() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(1)
            .ofMaxLength(100)
            .filter(s -> !s.trim().isEmpty());
    }

    @Provide
    Arbitrary<String> jsonRpcMessages() {
        return Arbitraries.of(
            "{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":\"1\"}",
            "{\"jsonrpc\":\"2.0\",\"result\":{\"capabilities\":{}},\"id\":\"1\"}",
            "{\"jsonrpc\":\"2.0\",\"method\":\"tools/list\",\"id\":\"2\"}",
            "{\"jsonrpc\":\"2.0\",\"method\":\"notification\",\"params\":{}}"
        );
    }
}