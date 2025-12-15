package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;

import java.time.Duration;
import java.util.List;

/**
 * Interface for processing MCP protocol traces.
 * Handles trace processing, storage, and real-time monitoring.
 */
public interface TraceProcessor {
    
    /**
     * Process a protocol trace for storage and analysis.
     * 
     * @param trace the protocol trace to process
     */
    void processProtocolTrace(ProtocolTrace trace);
    
    /**
     * Retrieve traces matching the specified filter criteria.
     * 
     * @param filter the filter criteria for trace selection
     * @return list of matching protocol traces
     */
    List<ProtocolTrace> getTraces(TraceFilter filter);
    
    /**
     * Enable or disable real-time monitoring of protocol traces.
     * 
     * @param enabled true to enable real-time monitoring, false to disable
     */
    void enableRealTimeMonitoring(boolean enabled);
    
    /**
     * Set the retention policy for protocol traces.
     * 
     * @param retention the duration to retain traces
     */
    void setTraceRetentionPolicy(Duration retention);
}