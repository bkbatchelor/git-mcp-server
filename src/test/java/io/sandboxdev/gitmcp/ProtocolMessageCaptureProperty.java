package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.interceptor.McpProtocolInterceptor;
import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import io.sandboxdev.gitmcp.debug.processor.TraceProcessor;
import io.sandboxdev.gitmcp.debug.processor.TraceProcessorImpl;
import net.jqwik.api.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springaicommunity.mcp.annotation.McpTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for protocol message capture functionality.
 * 
 * Feature: mcp-debugging, Property 1: Complete message capture
 * Validates: Requirements 1.1, 1.2
 */
public class ProtocolMessageCaptureProperty {

    private final TraceProcessor traceProcessor = new TraceProcessorImpl();
    private final McpProtocolInterceptor interceptor = new McpProtocolInterceptor(traceProcessor);

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 1: Complete message capture
    void protocolMessageCaptureCompleteness(@ForAll("toolNames") String toolName,
                                           @ForAll("repositoryPaths") String repositoryPath) throws Throwable {
        // Given: Clear any existing traces
        interceptor.clearTraces();
        
        // Create mock join point for MCP tool invocation
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{repositoryPath});
        
        // Mock successful execution
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        mockResult.put("data", "test-data");
        when(joinPoint.proceed()).thenReturn(mockResult);
        
        // When: Interceptor processes the tool invocation
        Object result = interceptor.interceptToolInvocation(joinPoint, mcpTool);
        
        // Then: A protocol trace should be captured
        List<ProtocolTrace> traces = interceptor.getAllTraces();
        assertThat(traces).hasSize(1);
        
        ProtocolTrace trace = traces.get(0);
        
        // Verify all required trace components are present
        assertThat(trace.traceId()).isNotNull().isNotEmpty();
        assertThat(trace.timestamp()).isNotNull();
        assertThat(trace.clientId()).isNotNull().isNotEmpty();
        assertThat(trace.messageType()).isEqualTo(toolName);
        assertThat(trace.processingTime()).isNotNull().isPositive();
        
        // Response should be captured
        assertThat(trace.response()).isNotNull();
        assertThat(trace.response()).containsEntry("success", true);
        assertThat(trace.response()).containsEntry("data", "test-data");
        
        // No error should be present for successful execution
        assertThat(trace.errorMessage()).isEmpty();
        
        // Result should be returned unchanged
        assertThat(result).isEqualTo(mockResult);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 1: Complete message capture
    void multipleToolInvocationsAreAllCaptured(@ForAll("toolNames") String toolName1,
                                               @ForAll("toolNames") String toolName2) throws Throwable {
        // Given: Clear any existing traces
        interceptor.clearTraces();
        
        // Create mock join points for multiple tool invocations
        ProceedingJoinPoint joinPoint1 = createMockJoinPoint(toolName1, "path1");
        ProceedingJoinPoint joinPoint2 = createMockJoinPoint(toolName2, "path2");
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Interceptor processes multiple tool invocations
        interceptor.interceptToolInvocation(joinPoint1, mcpTool);
        interceptor.interceptToolInvocation(joinPoint2, mcpTool);
        
        // Then: All invocations should be captured
        List<ProtocolTrace> traces = interceptor.getAllTraces();
        assertThat(traces).hasSize(2);
        
        // Verify each trace has required components
        for (ProtocolTrace trace : traces) {
            assertThat(trace.traceId()).isNotNull().isNotEmpty();
            assertThat(trace.timestamp()).isNotNull();
            assertThat(trace.clientId()).isNotNull().isNotEmpty();
            assertThat(trace.messageType()).isIn(toolName1, toolName2);
            assertThat(trace.processingTime()).isNotNull().isPositive();
            assertThat(trace.response()).isNotNull();
        }
        
        // Verify traces have different IDs
        assertThat(traces.get(0).traceId()).isNotEqualTo(traces.get(1).traceId());
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 1: Complete message capture
    void traceProcessorReceivesAllCapturedTraces(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear any existing traces
        interceptor.clearTraces();
        
        // Create mock join point
        ProceedingJoinPoint joinPoint = createMockJoinPoint(toolName, "test-path");
        McpTool mcpTool = mock(McpTool.class);
        
        // When: Interceptor processes the tool invocation
        interceptor.interceptToolInvocation(joinPoint, mcpTool);
        
        // Then: TraceProcessor should have received the trace
        List<ProtocolTrace> interceptorTraces = interceptor.getAllTraces();
        List<ProtocolTrace> processorTraces = traceProcessor.getTraces(TraceFilter.all());
        
        assertThat(interceptorTraces).hasSize(1);
        assertThat(processorTraces).hasSizeGreaterThanOrEqualTo(1);
        
        // Find our trace in the processor (there might be others from previous tests)
        ProtocolTrace ourTrace = interceptorTraces.get(0);
        boolean foundInProcessor = processorTraces.stream()
            .anyMatch(trace -> trace.traceId().equals(ourTrace.traceId()));
        
        assertThat(foundInProcessor).isTrue();
    }
    
    @Property(tries = 100)
    // Feature: mcp-debugging, Property 1: Complete message capture
    void errorTracesAreCapturedWithErrorMessage(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear any existing traces
        interceptor.clearTraces();
        
        // Create mock join point that throws exception
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        RuntimeException testException = new RuntimeException("Test error message");
        when(joinPoint.proceed()).thenThrow(testException);
        
        // When: Interceptor processes the failing tool invocation
        try {
            interceptor.interceptToolInvocation(joinPoint, mcpTool);
        } catch (RuntimeException e) {
            // Expected - interceptor should re-throw the exception
            assertThat(e).isEqualTo(testException);
        }
        
        // Then: A protocol trace should be captured with error information
        List<ProtocolTrace> traces = interceptor.getAllTraces();
        assertThat(traces).hasSize(1);
        
        ProtocolTrace trace = traces.get(0);
        assertThat(trace.traceId()).isNotNull().isNotEmpty();
        assertThat(trace.timestamp()).isNotNull();
        assertThat(trace.messageType()).isEqualTo(toolName);
        assertThat(trace.processingTime()).isNotNull().isPositive();
        assertThat(trace.errorMessage()).isPresent();
        assertThat(trace.errorMessage().get()).isEqualTo("Test error message");
    }
    
    private ProceedingJoinPoint createMockJoinPoint(String toolName, String repositoryPath) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{repositoryPath});
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        when(joinPoint.proceed()).thenReturn(mockResult);
        
        return joinPoint;
    }

    @Provide
    Arbitrary<String> repositoryPaths() {
        return Arbitraries.of(
            "/tmp/test-repo",
            "/nonexistent/path",
            ".",
            "/tmp/empty-dir",
            "/tmp/not-a-git-repo"
        );
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
}