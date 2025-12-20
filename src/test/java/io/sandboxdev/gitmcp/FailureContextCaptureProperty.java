package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.interceptor.ErrorContextCapturer;
import net.jqwik.api.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springaicommunity.mcp.annotation.McpTool;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for failure context capture functionality.
 * 
 * Feature: mcp-debugging, Property 11: Failure context capture
 * Validates: Requirements 4.1
 */
public class FailureContextCaptureProperty {

    private final ErrorContextCapturer capturer = new ErrorContextCapturer();

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void failureContextCaptureCompleteness(@ForAll("toolNames") String toolName,
                                          @ForAll("exceptionMessages") String exceptionMessage) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create mock join point that throws exception
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(signature.toString()).thenReturn("MockSignature." + toolName + "()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"param1", "param2"});
        
        RuntimeException testException = new RuntimeException(exceptionMessage);
        when(joinPoint.proceed()).thenThrow(testException);
        
        Instant beforeCapture = Instant.now();
        
        // When: Capturer processes the failing tool invocation
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException e) {
            // Expected - capturer should re-throw the exception
            assertThat(e).isEqualTo(testException);
        }
        
        Instant afterCapture = Instant.now();
        
        // Then: Complete error context should be captured
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify all required context components are present
        assertThat(context.executionId()).isNotNull().isNotEmpty();
        assertThat(context.toolName()).isEqualTo(toolName);
        assertThat(context.startTime()).isNotNull()
            .isBetween(beforeCapture, afterCapture);
        assertThat(context.failureTime()).isNotNull()
            .isBetween(beforeCapture, afterCapture)
            .isAfterOrEqualTo(context.startTime());
        
        // Verify execution context
        ErrorContextCapturer.ExecutionContext execContext = context.executionContext();
        assertThat(execContext.executionId()).isEqualTo(context.executionId());
        assertThat(execContext.toolName()).isEqualTo(toolName);
        assertThat(execContext.methodSignature()).contains(toolName);
        assertThat(execContext.parameters()).isNotEmpty();
        assertThat(execContext.threadName()).isNotNull().isNotEmpty();
        assertThat(execContext.systemContext()).isNotEmpty();
        
        // Verify exception information
        assertThat(context.exception()).isEqualTo(testException);
        assertThat(context.stackTrace()).isNotNull()
            .contains("RuntimeException")
            .contains(exceptionMessage);
        
        // Verify execution steps
        assertThat(context.executionSteps()).isNotEmpty();
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void multipleFailuresAreAllCaptured(@ForAll("toolNames") String toolName1,
                                       @ForAll("toolNames") String toolName2,
                                       @ForAll("exceptionMessages") String message1,
                                       @ForAll("exceptionMessages") String message2) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create mock join points for multiple failing invocations
        ProceedingJoinPoint joinPoint1 = createFailingJoinPoint(toolName1, new RuntimeException(message1));
        ProceedingJoinPoint joinPoint2 = createFailingJoinPoint(toolName2, new IllegalArgumentException(message2));
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes multiple failing tool invocations
        try {
            capturer.captureErrorContext(joinPoint1, mcpTool);
        } catch (RuntimeException ignored) {}
        
        try {
            capturer.captureErrorContext(joinPoint2, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: All failures should be captured
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(2);
        
        // Verify each context has required components
        for (ErrorContextCapturer.ErrorContext context : errorContexts) {
            assertThat(context.executionId()).isNotNull().isNotEmpty();
            assertThat(context.toolName()).isIn(toolName1, toolName2);
            assertThat(context.exception()).isNotNull();
            assertThat(context.stackTrace()).isNotNull().isNotEmpty();
            assertThat(context.executionContext()).isNotNull();
        }
        
        // Verify contexts have different execution IDs
        assertThat(errorContexts.get(0).executionId())
            .isNotEqualTo(errorContexts.get(1).executionId());
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void systemContextIsCompletelyCapture(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create mock join point that throws exception
        ProceedingJoinPoint joinPoint = createFailingJoinPoint(toolName, new RuntimeException("Test error"));
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes the failing tool invocation
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: System context should be completely captured
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ExecutionContext execContext = errorContexts.get(0).executionContext();
        
        // Verify system context contains expected information
        assertThat(execContext.systemContext()).containsKeys(
            "availableMemory",
            "totalMemory", 
            "maxMemory",
            "activeThreads",
            "timestamp"
        );
        
        // Verify system context values are reasonable
        assertThat((Long) execContext.systemContext().get("availableMemory")).isPositive();
        assertThat((Long) execContext.systemContext().get("totalMemory")).isPositive();
        assertThat((Long) execContext.systemContext().get("maxMemory")).isPositive();
        assertThat((Integer) execContext.systemContext().get("activeThreads")).isPositive();
        assertThat(execContext.systemContext().get("timestamp")).isInstanceOf(Instant.class);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void parametersAreCompletelyCapture(@ForAll("toolNames") String toolName,
                                       @ForAll("parameterValues") List<String> parameters) throws Throwable {
        Assume.that(parameters.size() <= 5); // Keep it reasonable
        
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create mock join point with specific parameters
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(signature.toString()).thenReturn("MockSignature." + toolName + "()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(parameters.toArray());
        
        RuntimeException testException = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(testException);
        
        // When: Capturer processes the failing tool invocation
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: All parameters should be captured
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ExecutionContext execContext = errorContexts.get(0).executionContext();
        
        // Verify all parameters are captured
        assertThat(execContext.parameters()).hasSize(parameters.size());
        
        for (int i = 0; i < parameters.size(); i++) {
            String paramKey = "arg" + i;
            assertThat(execContext.parameters()).containsKey(paramKey);
            assertThat(execContext.parameters().get(paramKey)).isEqualTo(parameters.get(i));
        }
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void nestedExceptionsAreCompletelyCapture(@ForAll("toolNames") String toolName,
                                             @ForAll("exceptionMessages") String rootMessage,
                                             @ForAll("exceptionMessages") String causeMessage) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create nested exception
        IllegalArgumentException cause = new IllegalArgumentException(causeMessage);
        RuntimeException rootException = new RuntimeException(rootMessage, cause);
        
        ProceedingJoinPoint joinPoint = createFailingJoinPoint(toolName, rootException);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes the failing tool invocation with nested exception
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: Complete exception chain should be captured
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify root exception is captured
        assertThat(context.exception()).isEqualTo(rootException);
        
        // Verify stack trace includes both root and cause
        assertThat(context.stackTrace())
            .contains("RuntimeException")
            .contains(rootMessage)
            .contains("Caused by:")
            .contains("IllegalArgumentException")
            .contains(causeMessage);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void errorContextFilteringByTool(@ForAll("toolNames") String targetTool,
                                    @ForAll("toolNames") String otherTool) throws Throwable {
        Assume.that(!targetTool.equals(otherTool));
        
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create failures for different tools
        ProceedingJoinPoint joinPoint1 = createFailingJoinPoint(targetTool, new RuntimeException("Error 1"));
        ProceedingJoinPoint joinPoint2 = createFailingJoinPoint(otherTool, new RuntimeException("Error 2"));
        McpTool mcpTool = mock(McpTool.class);
        
        try {
            capturer.captureErrorContext(joinPoint1, mcpTool);
        } catch (RuntimeException ignored) {}
        
        try {
            capturer.captureErrorContext(joinPoint2, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // When: Get error contexts for specific tool
        List<ErrorContextCapturer.ErrorContext> targetToolErrors = capturer.getErrorContextsForTool(targetTool);
        List<ErrorContextCapturer.ErrorContext> otherToolErrors = capturer.getErrorContextsForTool(otherTool);
        
        // Then: Only errors for the specified tool should be returned
        assertThat(targetToolErrors).hasSize(1);
        assertThat(targetToolErrors.get(0).toolName()).isEqualTo(targetTool);
        
        assertThat(otherToolErrors).hasSize(1);
        assertThat(otherToolErrors.get(0).toolName()).isEqualTo(otherTool);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 11: Failure context capture
    void errorContextFilteringByExceptionType(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create failures with different exception types
        ProceedingJoinPoint joinPoint1 = createFailingJoinPoint(toolName, new RuntimeException("Runtime error"));
        ProceedingJoinPoint joinPoint2 = createFailingJoinPoint(toolName, new IllegalArgumentException("Argument error"));
        McpTool mcpTool = mock(McpTool.class);
        
        try {
            capturer.captureErrorContext(joinPoint1, mcpTool);
        } catch (RuntimeException ignored) {}
        
        try {
            capturer.captureErrorContext(joinPoint2, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // When: Get error contexts by exception type
        List<ErrorContextCapturer.ErrorContext> runtimeErrors = 
            capturer.getErrorContextsByExceptionType(RuntimeException.class);
        List<ErrorContextCapturer.ErrorContext> argumentErrors = 
            capturer.getErrorContextsByExceptionType(IllegalArgumentException.class);
        
        // Then: Errors should be correctly filtered by exception type
        assertThat(runtimeErrors).hasSize(2); // Both are RuntimeExceptions (IllegalArgumentException extends RuntimeException)
        assertThat(argumentErrors).hasSize(1); // Only the IllegalArgumentException
        assertThat(argumentErrors.get(0).exception()).isInstanceOf(IllegalArgumentException.class);
    }

    private ProceedingJoinPoint createFailingJoinPoint(String toolName, Exception exception) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(signature.toString()).thenReturn("MockSignature." + toolName + "()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"param1", "param2"});
        when(joinPoint.proceed()).thenThrow(exception);
        
        return joinPoint;
    }

    @Provide
    Arbitrary<String> toolNames() {
        return Arbitraries.of(
            "getStatus",
            "getCurrentBranch",
            "listBranches",
            "getHistory",
            "createCommit",
            "stageFiles"
        );
    }

    @Provide
    Arbitrary<String> exceptionMessages() {
        return Arbitraries.of(
            "Repository not found",
            "Invalid branch name",
            "Permission denied",
            "Network timeout",
            "Invalid parameters",
            "File not found"
        );
    }

    @Provide
    Arbitrary<List<String>> parameterValues() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
            .list().ofMinSize(0).ofMaxSize(5);
    }
}