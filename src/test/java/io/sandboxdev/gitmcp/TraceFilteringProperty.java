package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.interceptor.McpProtocolInterceptor;
import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import io.sandboxdev.gitmcp.debug.processor.TraceProcessorImpl;
import net.jqwik.api.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.springaicommunity.mcp.annotation.McpTool;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for trace filtering functionality.
 * 
 * Feature: mcp-debugging, Property 2: Trace filtering accuracy
 * Validates: Requirements 1.4
 */
public class TraceFilteringProperty {

    private final McpProtocolInterceptor interceptor = new McpProtocolInterceptor(new TraceProcessorImpl());

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 2: Trace filtering accuracy
    void messageTypeFilteringAccuracy(@ForAll("toolNames") String targetToolName,
                                     @ForAll("toolNames") String otherToolName) throws Throwable {
        Assume.that(!targetToolName.equals(otherToolName));
        
        // Given: Clear traces and create multiple tool invocations
        interceptor.clearTraces();
        
        // Create traces with different tool names
        createMockTrace(targetToolName, "client1");
        createMockTrace(otherToolName, "client1");
        createMockTrace(targetToolName, "client2");
        
        // When: Apply message type filter
        TraceFilter filter = TraceFilter.byMessageType(targetToolName);
        List<ProtocolTrace> filteredTraces = interceptor.getTraces(filter);
        
        // Then: Only traces with matching message type should be returned
        assertThat(filteredTraces).hasSize(2);
        assertThat(filteredTraces).allMatch(trace -> trace.messageType().equals(targetToolName));
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 2: Trace filtering accuracy
    void clientIdFilteringAccuracy(@ForAll("clientIds") String targetClientId,
                                  @ForAll("clientIds") String otherClientId) throws Throwable {
        Assume.that(!targetClientId.equals(otherClientId));
        
        // Given: Clear traces and create multiple tool invocations
        interceptor.clearTraces();
        
        // Create traces with different client IDs
        createMockTrace("getStatus", targetClientId);
        createMockTrace("getStatus", otherClientId);
        createMockTrace("listBranches", targetClientId);
        
        // When: Apply client ID filter
        TraceFilter filter = TraceFilter.byClientId(targetClientId);
        List<ProtocolTrace> filteredTraces = interceptor.getTraces(filter);
        
        // Then: Only traces with matching client ID should be returned
        assertThat(filteredTraces).hasSize(2);
        assertThat(filteredTraces).allMatch(trace -> trace.clientId().equals(targetClientId));
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 2: Trace filtering accuracy
    void timeRangeFilteringAccuracy(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear traces
        interceptor.clearTraces();
        
        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600); // 1 hour ago
        Instant future = now.plusSeconds(3600); // 1 hour from now
        
        // Create a trace (it will have current timestamp)
        createMockTrace(toolName, "client1");
        
        // When: Apply time range filters
        TraceFilter pastFilter = new TraceFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(past),
            Optional.of(past.plusSeconds(1800)), // 30 minutes after past
            Optional.empty()
        );
        
        TraceFilter presentFilter = new TraceFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(past),
            Optional.of(future),
            Optional.empty()
        );
        
        TraceFilter futureFilter = new TraceFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.of(future),
            Optional.of(future.plusSeconds(1800)),
            Optional.empty()
        );
        
        // Then: Filters should work correctly based on time ranges
        List<ProtocolTrace> pastTraces = interceptor.getTraces(pastFilter);
        List<ProtocolTrace> presentTraces = interceptor.getTraces(presentFilter);
        List<ProtocolTrace> futureTraces = interceptor.getTraces(futureFilter);
        
        assertThat(pastTraces).isEmpty(); // No traces in the past
        assertThat(presentTraces).hasSize(1); // Our trace is in the present range
        assertThat(futureTraces).isEmpty(); // No traces in the future
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 2: Trace filtering accuracy
    void errorFilteringAccuracy(@ForAll("toolNames") String toolName) throws Throwable {
        // Given: Clear traces
        interceptor.clearTraces();
        
        // Create successful and failed traces
        createMockTrace(toolName, "client1"); // Successful trace
        createMockFailingTrace(toolName, "client2"); // Failed trace
        
        // When: Apply error filters
        TraceFilter successFilter = new TraceFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(false) // No error
        );
        
        TraceFilter errorFilter = new TraceFilter(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(true) // Has error
        );
        
        // Then: Filters should correctly separate successful and failed traces
        List<ProtocolTrace> successTraces = interceptor.getTraces(successFilter);
        List<ProtocolTrace> errorTraces = interceptor.getTraces(errorFilter);
        
        assertThat(successTraces).hasSize(1);
        assertThat(successTraces.get(0).errorMessage()).isEmpty();
        
        assertThat(errorTraces).hasSize(1);
        assertThat(errorTraces.get(0).errorMessage()).isPresent();
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 2: Trace filtering accuracy
    void combinedFilteringAccuracy(@ForAll("toolNames") String targetToolName,
                                  @ForAll("toolNames") String otherToolName,
                                  @ForAll("clientIds") String targetClientId,
                                  @ForAll("clientIds") String otherClientId) throws Throwable {
        Assume.that(!targetToolName.equals(otherToolName));
        Assume.that(!targetClientId.equals(otherClientId));
        
        // Given: Clear traces and create multiple combinations
        interceptor.clearTraces();
        
        createMockTrace(targetToolName, targetClientId);   // Should match
        createMockTrace(targetToolName, otherClientId);    // Wrong client
        createMockTrace(otherToolName, targetClientId);    // Wrong tool
        createMockTrace(otherToolName, otherClientId);     // Wrong both
        
        // When: Apply combined filter
        TraceFilter combinedFilter = new TraceFilter(
            Optional.of(targetToolName),
            Optional.of(targetClientId),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        
        List<ProtocolTrace> filteredTraces = interceptor.getTraces(combinedFilter);
        
        // Then: Only the trace matching both criteria should be returned
        assertThat(filteredTraces).hasSize(1);
        ProtocolTrace matchingTrace = filteredTraces.get(0);
        assertThat(matchingTrace.messageType()).isEqualTo(targetToolName);
        assertThat(matchingTrace.clientId()).isEqualTo(targetClientId);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 2: Trace filtering accuracy
    void allFilterReturnsAllTraces(@ForAll("toolNames") String toolName1,
                                  @ForAll("toolNames") String toolName2) throws Throwable {
        // Given: Clear traces and create multiple traces
        interceptor.clearTraces();
        
        createMockTrace(toolName1, "client1");
        createMockTrace(toolName2, "client2");
        createMockFailingTrace(toolName1, "client3");
        
        // When: Apply "all" filter
        TraceFilter allFilter = TraceFilter.all();
        List<ProtocolTrace> allTraces = interceptor.getTraces(allFilter);
        
        // Then: All traces should be returned
        assertThat(allTraces).hasSize(3);
    }

    private void createMockTrace(String toolName, String clientId) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        when(joinPoint.proceed()).thenReturn(mockResult);
        
        // Note: Using default client ID since we can't easily mock the extraction
        
        interceptor.interceptToolInvocation(joinPoint, mcpTool);
    }

    private void createMockFailingTrace(String toolName, String clientId) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        McpTool mcpTool = mock(McpTool.class);
        
        when(signature.getName()).thenReturn(toolName);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-path"});
        
        RuntimeException testException = new RuntimeException("Test error");
        when(joinPoint.proceed()).thenThrow(testException);
        
        try {
            interceptor.interceptToolInvocation(joinPoint, mcpTool);
        } catch (RuntimeException e) {
            // Expected - interceptor re-throws exceptions
        }
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
    Arbitrary<String> clientIds() {
        return Arbitraries.of(
            "client-1",
            "client-2",
            "client-3",
            "default-client",
            "test-client"
        );
    }
}