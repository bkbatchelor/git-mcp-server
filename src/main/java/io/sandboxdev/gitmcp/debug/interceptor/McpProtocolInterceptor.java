package io.sandboxdev.gitmcp.debug.interceptor;

import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import io.sandboxdev.gitmcp.debug.processor.TraceProcessor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * AOP interceptor for capturing MCP protocol interactions.
 * 
 * This aspect intercepts all methods annotated with @McpTool to capture:
 * - Message content, timestamps, and processing duration
 * - Request parameters and response data
 * - Error information when tool invocations fail
 * 
 * Provides filtering capabilities by message type and client ID.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "mcp-debug.protocol-tracing.enabled", havingValue = "true", matchIfMissing = false)
public class McpProtocolInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(McpProtocolInterceptor.class);
    
    private final TraceProcessor traceProcessor;
    private final Map<String, TraceFilter> activeFilters = new ConcurrentHashMap<>();
    private final List<ProtocolTrace> capturedTraces = new CopyOnWriteArrayList<>();
    
    public McpProtocolInterceptor(TraceProcessor traceProcessor) {
        this.traceProcessor = traceProcessor;
    }
    
    /**
     * Intercepts all @McpTool annotated methods to capture protocol traces.
     */
    @Around("@annotation(mcpTool)")
    public Object interceptToolInvocation(ProceedingJoinPoint joinPoint, McpTool mcpTool) throws Throwable {
        String traceId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        String toolName = getToolName(joinPoint, mcpTool);
        String clientId = extractClientId(joinPoint);
        
        // Capture request parameters
        Map<String, Object> parameters = captureParameters(joinPoint);
        
        logger.debug("Intercepting MCP tool invocation: {} with traceId: {}", toolName, traceId);
        
        Object result = null;
        Optional<String> errorMessage = Optional.empty();
        
        try {
            // Execute the original method
            result = joinPoint.proceed();
            
        } catch (Exception e) {
            errorMessage = Optional.of(e.getMessage());
            logger.debug("MCP tool invocation failed: {} - {}", toolName, e.getMessage());
            throw e; // Re-throw to maintain original behavior
            
        } finally {
            // Calculate processing time
            Duration processingTime = Duration.between(startTime, Instant.now());
            
            // Capture response (null if exception occurred)
            Map<String, Object> response = captureResponse(result);
            
            // Create protocol trace
            ProtocolTrace trace = new ProtocolTrace(
                traceId,
                startTime,
                clientId,
                toolName,
                parameters,
                response,
                processingTime,
                errorMessage
            );
            
            // Store trace if it passes filters
            if (shouldCaptureTrace(trace)) {
                capturedTraces.add(trace);
                traceProcessor.processProtocolTrace(trace);
                logger.debug("Captured protocol trace for tool: {} ({}ms)", toolName, processingTime.toMillis());
            }
        }
        
        return result;
    }
    
    /**
     * Add a filter for trace capture.
     */
    public void addFilter(String filterId, TraceFilter filter) {
        activeFilters.put(filterId, filter);
        logger.debug("Added trace filter: {}", filterId);
    }
    
    /**
     * Remove a filter.
     */
    public void removeFilter(String filterId) {
        activeFilters.remove(filterId);
        logger.debug("Removed trace filter: {}", filterId);
    }
    
    /**
     * Clear all filters.
     */
    public void clearFilters() {
        activeFilters.clear();
        logger.debug("Cleared all trace filters");
    }
    
    /**
     * Get all captured traces that match the given filter.
     */
    public List<ProtocolTrace> getTraces(TraceFilter filter) {
        return capturedTraces.stream()
            .filter(trace -> matchesFilter(trace, filter))
            .toList();
    }
    
    /**
     * Get all captured traces.
     */
    public List<ProtocolTrace> getAllTraces() {
        return List.copyOf(capturedTraces);
    }
    
    /**
     * Clear all captured traces.
     */
    public void clearTraces() {
        capturedTraces.clear();
        logger.debug("Cleared all captured traces");
    }
    
    private String getToolName(ProceedingJoinPoint joinPoint, McpTool mcpTool) {
        // Use method name as tool name (MCP convention)
        return joinPoint.getSignature().getName();
    }
    
    private String extractClientId(ProceedingJoinPoint joinPoint) {
        // For now, return a default client ID
        // In a real implementation, this would extract from request context
        return "default-client";
    }
    
    private Map<String, Object> captureParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> parameters = new HashMap<>();
        
        Object[] args = joinPoint.getArgs();
        Method method = getMethod(joinPoint);
        
        if (method != null && args != null) {
            String[] paramNames = getParameterNames(method);
            
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                parameters.put(paramNames[i], args[i]);
            }
        }
        
        return parameters;
    }
    
    private Map<String, Object> captureResponse(Object result) {
        Map<String, Object> response = new HashMap<>();
        
        if (result != null) {
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                response.putAll(resultMap);
            } else {
                response.put("result", result);
            }
        }
        
        return response;
    }
    
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.getTarget().getClass()
                .getMethod(joinPoint.getSignature().getName(), 
                    Arrays.stream(joinPoint.getArgs())
                        .map(Object::getClass)
                        .toArray(Class[]::new));
        } catch (NoSuchMethodException e) {
            logger.warn("Could not get method for parameter extraction: {}", e.getMessage());
            return null;
        }
    }
    
    private String[] getParameterNames(Method method) {
        // Simple parameter naming - in production, could use reflection or annotations
        return Arrays.stream(method.getParameters())
            .map(param -> param.getName())
            .toArray(String[]::new);
    }
    
    private boolean shouldCaptureTrace(ProtocolTrace trace) {
        // If no filters are active, capture all traces
        if (activeFilters.isEmpty()) {
            return true;
        }
        
        // Check if trace matches any active filter
        return activeFilters.values().stream()
            .anyMatch(filter -> matchesFilter(trace, filter));
    }
    
    private boolean matchesFilter(ProtocolTrace trace, TraceFilter filter) {
        // Check message type filter
        if (filter.messageType() != null && 
            !filter.messageType().equals(trace.messageType())) {
            return false;
        }
        
        // Check client ID filter
        if (filter.clientId() != null && 
            !filter.clientId().equals(trace.clientId())) {
            return false;
        }
        
        // Check time range filter
        if (filter.timeRange() != null) {
            if (filter.timeRange().start() != null && 
                trace.timestamp().isBefore(filter.timeRange().start())) {
                return false;
            }
            if (filter.timeRange().end() != null && 
                trace.timestamp().isAfter(filter.timeRange().end())) {
                return false;
            }
        }
        
        // Check error filter
        if (filter.errorsOnly()) {
            boolean traceHasError = trace.errorMessage().isPresent();
            if (!traceHasError) {
                return false;
            }
        }
        
        return true;
    }
}