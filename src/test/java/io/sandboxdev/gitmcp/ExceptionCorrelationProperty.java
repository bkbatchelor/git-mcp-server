package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.interceptor.ErrorContextCapturer;
import net.jqwik.api.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springaicommunity.mcp.annotation.McpTool;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for exception correlation accuracy.
 * 
 * Feature: mcp-debugging, Property 12: Exception correlation accuracy
 * Validates: Requirements 4.3
 */
public class ExceptionCorrelationProperty {

    private final ErrorContextCapturer capturer = new ErrorContextCapturer();

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void exceptionCorrelationAccuracy(@ForAll("toolNames") String toolName,
                                     @ForAll("exceptionMessages") String exceptionMessage) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create a specific exception instance
        RuntimeException specificException = new RuntimeException(exceptionMessage);
        
        // Create mock join point that throws the specific exception
        ProceedingJoinPoint joinPoint = createFailingJoinPoint(toolName, specificException);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes the failing tool invocation
        RuntimeException caughtException = null;
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException e) {
            caughtException = e;
        }
        
        // Then: The exception should be correctly correlated with the tool invocation
        assertThat(caughtException).isEqualTo(specificException);
        
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify exact exception correlation
        assertThat(context.exception()).isEqualTo(specificException);
        assertThat(context.exception().getMessage()).isEqualTo(exceptionMessage);
        assertThat(context.toolName()).isEqualTo(toolName);
        
        // Verify the execution context matches the tool invocation
        assertThat(context.executionContext().toolName()).isEqualTo(toolName);
        assertThat(context.executionContext().executionId()).isEqualTo(context.executionId());
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void multipleExceptionsAreCorrectlyCorrelated(@ForAll("toolNames") String toolName1,
                                                 @ForAll("toolNames") String toolName2,
                                                 @ForAll("exceptionMessages") String message1,
                                                 @ForAll("exceptionMessages") String message2) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create specific exception instances
        RuntimeException exception1 = new RuntimeException(message1);
        IllegalArgumentException exception2 = new IllegalArgumentException(message2);
        
        // Create mock join points for different tools with different exceptions
        ProceedingJoinPoint joinPoint1 = createFailingJoinPoint(toolName1, exception1);
        ProceedingJoinPoint joinPoint2 = createFailingJoinPoint(toolName2, exception2);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes multiple failing tool invocations
        RuntimeException caught1 = null, caught2 = null;
        
        try {
            capturer.captureErrorContext(joinPoint1, mcpTool);
        } catch (RuntimeException e) {
            caught1 = e;
        }
        
        try {
            capturer.captureErrorContext(joinPoint2, mcpTool);
        } catch (RuntimeException e) {
            caught2 = e;
        }
        
        // Then: Each exception should be correctly correlated with its originating tool
        assertThat(caught1).isEqualTo(exception1);
        assertThat(caught2).isEqualTo(exception2);
        
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(2);
        
        // Find contexts by tool name and verify correlation
        ErrorContextCapturer.ErrorContext context1 = errorContexts.stream()
            .filter(ctx -> ctx.toolName().equals(toolName1))
            .findFirst()
            .orElseThrow();
            
        ErrorContextCapturer.ErrorContext context2 = errorContexts.stream()
            .filter(ctx -> ctx.toolName().equals(toolName2))
            .findFirst()
            .orElseThrow();
        
        // Verify each exception is correlated with the correct tool
        assertThat(context1.exception()).isEqualTo(exception1);
        assertThat(context1.exception().getMessage()).isEqualTo(message1);
        assertThat(context1.toolName()).isEqualTo(toolName1);
        
        assertThat(context2.exception()).isEqualTo(exception2);
        assertThat(context2.exception().getMessage()).isEqualTo(message2);
        assertThat(context2.toolName()).isEqualTo(toolName2);
        
        // Verify execution contexts match their respective tools
        assertThat(context1.executionContext().toolName()).isEqualTo(toolName1);
        assertThat(context2.executionContext().toolName()).isEqualTo(toolName2);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void exceptionCorrelationWithExecutionId(@ForAll("toolNames") String toolName,
                                            @ForAll("exceptionMessages") String exceptionMessage) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        RuntimeException specificException = new RuntimeException(exceptionMessage);
        ProceedingJoinPoint joinPoint = createFailingJoinPoint(toolName, specificException);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes the failing tool invocation
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: Exception should be correlated with a unique execution ID
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify execution ID correlation
        assertThat(context.executionId()).isNotNull().isNotEmpty();
        assertThat(context.executionContext().executionId()).isEqualTo(context.executionId());
        
        // Verify we can find the context by execution ID
        ErrorContextCapturer.ErrorContext foundContext = capturer.getErrorContextById(context.executionId());
        assertThat(foundContext).isEqualTo(context);
        assertThat(foundContext.exception()).isEqualTo(specificException);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void nestedExceptionCorrelationAccuracy(@ForAll("toolNames") String toolName,
                                           @ForAll("exceptionMessages") String rootMessage,
                                           @ForAll("exceptionMessages") String causeMessage) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create nested exception chain
        IllegalStateException rootCause = new IllegalStateException(causeMessage);
        IllegalArgumentException intermediateCause = new IllegalArgumentException("Intermediate", rootCause);
        RuntimeException topException = new RuntimeException(rootMessage, intermediateCause);
        
        ProceedingJoinPoint joinPoint = createFailingJoinPoint(toolName, topException);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes the failing tool invocation with nested exceptions
        RuntimeException caughtException = null;
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException e) {
            caughtException = e;
        }
        
        // Then: The complete exception chain should be correctly correlated
        assertThat(caughtException).isEqualTo(topException);
        
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify top-level exception correlation
        assertThat(context.exception()).isEqualTo(topException);
        assertThat(context.exception().getMessage()).isEqualTo(rootMessage);
        
        // Verify cause chain is preserved
        assertThat(context.exception().getCause()).isEqualTo(intermediateCause);
        assertThat(context.exception().getCause().getCause()).isEqualTo(rootCause);
        
        // Verify stack trace includes all exception levels
        assertThat(context.stackTrace())
            .contains("RuntimeException")
            .contains(rootMessage)
            .contains("Caused by:")
            .contains("IllegalArgumentException")
            .contains("Intermediate")
            .contains("IllegalStateException")
            .contains(causeMessage);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void exceptionCorrelationWithParameterContext(@ForAll("toolNames") String toolName,
                                                  @ForAll("parameterValues") List<String> parameters,
                                                  @ForAll("exceptionMessages") String exceptionMessage) throws Throwable {
        Assume.that(parameters.size() <= 3); // Keep it reasonable
        
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        RuntimeException specificException = new RuntimeException(exceptionMessage);
        
        // Create mock join point with specific parameters
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(signature.toString()).thenReturn("MockSignature." + toolName + "()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(parameters.toArray());
        when(joinPoint.proceed()).thenThrow(specificException);
        
        // When: Capturer processes the failing tool invocation
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: Exception should be correlated with the exact parameter context
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify exception correlation
        assertThat(context.exception()).isEqualTo(specificException);
        assertThat(context.toolName()).isEqualTo(toolName);
        
        // Verify parameter correlation
        assertThat(context.executionContext().parameters()).hasSize(parameters.size());
        for (int i = 0; i < parameters.size(); i++) {
            String paramKey = "arg" + i;
            assertThat(context.executionContext().parameters())
                .containsEntry(paramKey, parameters.get(i));
        }
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void exceptionCorrelationWithTimingContext(@ForAll("toolNames") String toolName,
                                              @ForAll("exceptionMessages") String exceptionMessage) throws Throwable {
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        RuntimeException specificException = new RuntimeException(exceptionMessage);
        ProceedingJoinPoint joinPoint = createFailingJoinPoint(toolName, specificException);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes the failing tool invocation
        try {
            capturer.captureErrorContext(joinPoint, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: Exception should be correlated with accurate timing context
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(1);
        
        ErrorContextCapturer.ErrorContext context = errorContexts.get(0);
        
        // Verify exception correlation
        assertThat(context.exception()).isEqualTo(specificException);
        
        // Verify timing correlation
        assertThat(context.startTime()).isNotNull();
        assertThat(context.failureTime()).isNotNull();
        assertThat(context.failureTime()).isAfterOrEqualTo(context.startTime());
        
        // Verify execution context timing matches
        assertThat(context.executionContext().startTime()).isEqualTo(context.startTime());
        
        // Verify system context timestamp is reasonable
        Object systemTimestamp = context.executionContext().systemContext().get("timestamp");
        assertThat(systemTimestamp).isNotNull();
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 12: Exception correlation accuracy
    void sameExceptionTypeFromDifferentToolsAreDistinguished(@ForAll("toolNames") String toolName1,
                                                            @ForAll("toolNames") String toolName2,
                                                            @ForAll("exceptionMessages") String message) throws Throwable {
        Assume.that(!toolName1.equals(toolName2));
        
        // Given: Clear any existing error contexts
        capturer.clearErrorContexts();
        
        // Create same exception type with same message from different tools
        RuntimeException exception1 = new RuntimeException(message);
        RuntimeException exception2 = new RuntimeException(message);
        
        ProceedingJoinPoint joinPoint1 = createFailingJoinPoint(toolName1, exception1);
        ProceedingJoinPoint joinPoint2 = createFailingJoinPoint(toolName2, exception2);
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Capturer processes failures from different tools
        try {
            capturer.captureErrorContext(joinPoint1, mcpTool);
        } catch (RuntimeException ignored) {}
        
        try {
            capturer.captureErrorContext(joinPoint2, mcpTool);
        } catch (RuntimeException ignored) {}
        
        // Then: Exceptions should be correctly distinguished by their originating tool
        List<ErrorContextCapturer.ErrorContext> errorContexts = capturer.getAllErrorContexts();
        assertThat(errorContexts).hasSize(2);
        
        // Find contexts by tool name
        ErrorContextCapturer.ErrorContext context1 = errorContexts.stream()
            .filter(ctx -> ctx.toolName().equals(toolName1))
            .findFirst()
            .orElseThrow();
            
        ErrorContextCapturer.ErrorContext context2 = errorContexts.stream()
            .filter(ctx -> ctx.toolName().equals(toolName2))
            .findFirst()
            .orElseThrow();
        
        // Verify each exception is correlated with the correct tool despite similarity
        assertThat(context1.exception()).isEqualTo(exception1);
        assertThat(context1.toolName()).isEqualTo(toolName1);
        assertThat(context1.executionContext().toolName()).isEqualTo(toolName1);
        
        assertThat(context2.exception()).isEqualTo(exception2);
        assertThat(context2.toolName()).isEqualTo(toolName2);
        assertThat(context2.executionContext().toolName()).isEqualTo(toolName2);
        
        // Verify they have different execution IDs
        assertThat(context1.executionId()).isNotEqualTo(context2.executionId());
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
            "File not found",
            "Concurrent modification",
            "Authentication failed"
        );
    }

    @Provide
    Arbitrary<List<String>> parameterValues() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(15)
            .list().ofMinSize(0).ofMaxSize(3);
    }
}