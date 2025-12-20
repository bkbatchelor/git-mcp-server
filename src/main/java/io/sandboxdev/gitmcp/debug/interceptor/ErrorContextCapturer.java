package io.sandboxdev.gitmcp.debug.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AOP interceptor for capturing error context from MCP tool invocations.
 * 
 * This aspect intercepts all methods annotated with @McpTool to capture:
 * - Complete execution context for failures
 * - Exception correlation with originating tool invocations
 * - Step-by-step execution logging
 */
@Aspect
@Component
@ConditionalOnProperty(name = "mcp-debug.enabled", havingValue = "true", matchIfMissing = false)
public class ErrorContextCapturer {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorContextCapturer.class);
    
    private final List<ErrorContext> capturedErrors = new CopyOnWriteArrayList<>();
    
    /**
     * Intercepts all @McpTool annotated methods to capture error context.
     */
    @Around("@annotation(mcpTool)")
    public Object captureErrorContext(ProceedingJoinPoint joinPoint, McpTool mcpTool) throws Throwable {
        String executionId = UUID.randomUUID().toString();
        String toolName = joinPoint.getSignature().getName();
        Instant startTime = Instant.now();
        
        // Set up MDC for correlation
        MDC.put("executionId", executionId);
        MDC.put("toolName", toolName);
        
        // Capture execution context
        ExecutionContext context = captureExecutionContext(joinPoint, executionId, toolName, startTime);
        
        logger.debug("Starting tool execution with context capture: {} [{}]", toolName, executionId);
        
        try {
            // Log step-by-step execution
            logExecutionStep(executionId, "STARTED", "Tool execution started", null);
            
            // Execute the original method
            Object result = joinPoint.proceed();
            
            logExecutionStep(executionId, "COMPLETED", "Tool execution completed successfully", null);
            
            return result;
            
        } catch (Exception e) {
            // Capture complete error context
            ErrorContext errorContext = new ErrorContext(
                executionId,
                toolName,
                startTime,
                Instant.now(),
                context,
                e,
                captureStackTrace(e),
                captureExecutionSteps(executionId)
            );
            
            capturedErrors.add(errorContext);
            
            logExecutionStep(executionId, "FAILED", "Tool execution failed", e);
            
            logger.error("Captured error context for tool: {} [{}] - {}", 
                toolName, executionId, e.getMessage(), e);
            
            throw e; // Re-throw to maintain original behavior
            
        } finally {
            // Clean up MDC
            MDC.remove("executionId");
            MDC.remove("toolName");
        }
    }
    
    /**
     * Get all captured error contexts.
     */
    public List<ErrorContext> getAllErrorContexts() {
        return List.copyOf(capturedErrors);
    }
    
    /**
     * Get error contexts for a specific tool.
     */
    public List<ErrorContext> getErrorContextsForTool(String toolName) {
        return capturedErrors.stream()
            .filter(context -> context.toolName().equals(toolName))
            .toList();
    }
    
    /**
     * Get error contexts by exception type.
     */
    public List<ErrorContext> getErrorContextsByExceptionType(Class<? extends Exception> exceptionType) {
        return capturedErrors.stream()
            .filter(context -> exceptionType.isInstance(context.exception()))
            .toList();
    }
    
    /**
     * Find error context by execution ID.
     */
    public ErrorContext getErrorContextById(String executionId) {
        return capturedErrors.stream()
            .filter(context -> context.executionId().equals(executionId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Clear all captured error contexts.
     */
    public void clearErrorContexts() {
        capturedErrors.clear();
        logger.debug("Cleared all error contexts");
    }
    
    private ExecutionContext captureExecutionContext(ProceedingJoinPoint joinPoint, String executionId, 
                                                   String toolName, Instant startTime) {
        Map<String, Object> parameters = new HashMap<>();
        
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                parameters.put("arg" + i, args[i]);
            }
        }
        
        return new ExecutionContext(
            executionId,
            toolName,
            startTime,
            joinPoint.getSignature().toString(),
            parameters,
            Thread.currentThread().getName(),
            captureSystemContext()
        );
    }
    
    private Map<String, Object> captureSystemContext() {
        Map<String, Object> systemContext = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        systemContext.put("availableMemory", runtime.freeMemory());
        systemContext.put("totalMemory", runtime.totalMemory());
        systemContext.put("maxMemory", runtime.maxMemory());
        systemContext.put("activeThreads", Thread.activeCount());
        systemContext.put("timestamp", Instant.now());
        
        return systemContext;
    }
    
    private String captureStackTrace(Exception exception) {
        StringBuilder stackTrace = new StringBuilder();
        stackTrace.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            stackTrace.append("\tat ").append(element.toString()).append("\n");
        }
        
        // Include cause if present
        Throwable cause = exception.getCause();
        if (cause != null) {
            stackTrace.append("Caused by: ").append(captureStackTrace((Exception) cause));
        }
        
        return stackTrace.toString();
    }
    
    private List<ExecutionStep> captureExecutionSteps(String executionId) {
        // In a full implementation, this would retrieve logged execution steps
        // For now, return a basic step list
        return List.of(
            new ExecutionStep(executionId, Instant.now(), "STARTED", "Tool execution started", null),
            new ExecutionStep(executionId, Instant.now(), "FAILED", "Tool execution failed", null)
        );
    }
    
    private void logExecutionStep(String executionId, String stepType, String description, Exception exception) {
        String exceptionInfo = exception != null ? exception.getClass().getSimpleName() + ": " + exception.getMessage() : null;
        
        logger.debug("Execution step [{}]: {} - {} {}", 
            executionId, stepType, description, 
            exceptionInfo != null ? "(" + exceptionInfo + ")" : "");
    }
    
    /**
     * Represents the complete execution context at the time of tool invocation.
     */
    public record ExecutionContext(
        String executionId,
        String toolName,
        Instant startTime,
        String methodSignature,
        Map<String, Object> parameters,
        String threadName,
        Map<String, Object> systemContext
    ) {}
    
    /**
     * Represents a captured error context with complete failure information.
     */
    public record ErrorContext(
        String executionId,
        String toolName,
        Instant startTime,
        Instant failureTime,
        ExecutionContext executionContext,
        Exception exception,
        String stackTrace,
        List<ExecutionStep> executionSteps
    ) {}
    
    /**
     * Represents a single step in the execution process.
     */
    public record ExecutionStep(
        String executionId,
        Instant timestamp,
        String stepType,
        String description,
        String additionalInfo
    ) {}
}