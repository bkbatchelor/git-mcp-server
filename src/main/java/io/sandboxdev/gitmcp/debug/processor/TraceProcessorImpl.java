package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Enhanced implementation of TraceProcessor for storing and retrieving protocol traces.
 * Supports real-time monitoring, trace retention policies, and error highlighting.
 */
@Component
@ConditionalOnProperty(name = "mcp-debug.enabled", havingValue = "true", matchIfMissing = false)
public class TraceProcessorImpl implements TraceProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TraceProcessorImpl.class);
    
    private final List<ProtocolTrace> traces = new CopyOnWriteArrayList<>();
    private final List<Consumer<ProtocolTrace>> realTimeListeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private boolean realTimeMonitoringEnabled = false;
    private Duration retentionPolicy = Duration.ofHours(24);
    
    @Override
    public void processProtocolTrace(ProtocolTrace trace) {
        traces.add(trace);
        
        // Real-time monitoring with enhanced error highlighting (Requirement 1.3, 1.5)
        if (realTimeMonitoringEnabled) {
            if (trace.errorMessage().isPresent()) {
                logger.error("🔴 ERROR TRACE: {} - {} ({}ms) - ERROR: {}", 
                    trace.messageType(), 
                    trace.traceId(), 
                    trace.processingTime().toMillis(),
                    trace.errorMessage().get());
            } else {
                logger.info("✅ Real-time trace: {} - {} ({}ms)", 
                    trace.messageType(), 
                    trace.traceId(), 
                    trace.processingTime().toMillis());
            }
            
            // Notify real-time listeners for live message flows
            notifyRealTimeListeners(trace);
        }
        
        // Clean up old traces based on retention policy
        cleanupOldTraces();
    }
    
    @Override
    public List<ProtocolTrace> getTraces(TraceFilter filter) {
        return traces.stream()
            .filter(trace -> matchesFilter(trace, filter))
            .toList();
    }
    
    @Override
    public void enableRealTimeMonitoring(boolean enabled) {
        this.realTimeMonitoringEnabled = enabled;
        logger.info("Real-time monitoring {} - Live message flows will {} be displayed", 
            enabled ? "enabled" : "disabled",
            enabled ? "" : "NOT");
        
        if (enabled) {
            // Start periodic cleanup when monitoring is enabled
            startPeriodicCleanup();
        }
    }
    
    @Override
    public void setTraceRetentionPolicy(Duration retention) {
        this.retentionPolicy = retention;
        logger.info("Trace retention policy set to: {} - Traces older than this will be automatically cleaned up", retention);
        cleanupOldTraces();
    }
    
    /**
     * Add a real-time listener for live message flows (Requirement 1.3).
     * 
     * @param listener consumer that will be notified of new traces
     */
    public void addRealTimeListener(Consumer<ProtocolTrace> listener) {
        realTimeListeners.add(listener);
        logger.debug("Added real-time trace listener");
    }
    
    /**
     * Remove a real-time listener.
     * 
     * @param listener consumer to remove
     */
    public void removeRealTimeListener(Consumer<ProtocolTrace> listener) {
        realTimeListeners.remove(listener);
        logger.debug("Removed real-time trace listener");
    }
    
    /**
     * Get traces with error highlighting (Requirement 1.5).
     * 
     * @param filter trace filter criteria
     * @return list of traces with error information highlighted
     */
    public List<ProtocolTrace> getTracesWithErrorHighlighting(TraceFilter filter) {
        return traces.stream()
            .filter(trace -> matchesFilter(trace, filter))
            .map(this::highlightErrors)
            .toList();
    }
    
    private void cleanupOldTraces() {
        Instant cutoff = Instant.now().minus(retentionPolicy);
        int beforeSize = traces.size();
        traces.removeIf(trace -> trace.timestamp().isBefore(cutoff));
        int afterSize = traces.size();
        
        if (beforeSize != afterSize) {
            logger.debug("Cleaned up {} old traces (retention policy: {})", 
                beforeSize - afterSize, retentionPolicy);
        }
    }
    
    private void startPeriodicCleanup() {
        // Schedule cleanup every hour when real-time monitoring is active
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupOldTraces,
            1, 1, TimeUnit.HOURS
        );
        logger.debug("Started periodic trace cleanup (every 1 hour)");
    }
    
    private void notifyRealTimeListeners(ProtocolTrace trace) {
        for (Consumer<ProtocolTrace> listener : realTimeListeners) {
            try {
                listener.accept(trace);
            } catch (Exception e) {
                logger.warn("Error notifying real-time listener: {}", e.getMessage());
            }
        }
    }
    
    private ProtocolTrace highlightErrors(ProtocolTrace trace) {
        // If trace has an error, enhance the error message with highlighting markers
        if (trace.errorMessage().isPresent()) {
            String originalError = trace.errorMessage().get();
            String highlightedError = "🔴 ERROR: " + originalError + " 🔴";
            
            return new ProtocolTrace(
                trace.traceId(),
                trace.timestamp(),
                trace.clientId(),
                trace.messageType(),
                trace.parameters(),
                trace.response(),
                trace.processingTime(),
                java.util.Optional.of(highlightedError)
            );
        }
        return trace;
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