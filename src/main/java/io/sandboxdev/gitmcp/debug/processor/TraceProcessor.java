package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;

import java.time.Duration;
import java.util.List;

/**
 * Interface for processing MCP protocol traces.
 * 
 * Handles storage, retrieval, and real-time monitoring of protocol traces.
 */
public interface TraceProcessor {
    
    /**
     * Process a captured protocol trace.
     * 
     * @param trace the protocol trace to process
     */
    void processProtocolTrace(ProtocolTrace trace);
    
    /**
     * Retrieve traces matching the given filter.
     * 
     * @param filter the filter criteria
     * @return list of matching traces
     */
    List<ProtocolTrace> getTraces(TraceFilter filter);
    
    /**
     * Enable or disable real-time monitoring.
     * 
     * @param enabled true to enable real-time monitoring
     */
    void enableRealTimeMonitoring(boolean enabled);
    
    /**
     * Set the trace retention policy.
     * 
     * @param retention how long to keep traces
     */
    void setTraceRetentionPolicy(Duration retention);
}