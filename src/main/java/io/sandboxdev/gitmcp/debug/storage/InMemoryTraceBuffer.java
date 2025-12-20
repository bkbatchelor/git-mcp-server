package io.sandboxdev.gitmcp.debug.storage;

import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe ring buffer implementation for storing protocol traces in memory.
 * Provides configurable size and retention policy with concurrent access support.
 */
@Component
public class InMemoryTraceBuffer {
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ProtocolTrace[] buffer;
    private final int capacity;
    private volatile int writeIndex = 0;
    private volatile int size = 0;
    private volatile Duration retentionPolicy = Duration.ofHours(1);
    
    /**
     * Creates a trace buffer with default capacity of 10,000 traces.
     */
    public InMemoryTraceBuffer() {
        this(10000);
    }
    
    /**
     * Creates a trace buffer with specified capacity.
     * 
     * @param capacity Maximum number of traces to store
     */
    public InMemoryTraceBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new ProtocolTrace[capacity];
    }
    
    /**
     * Adds a protocol trace to the buffer.
     * If buffer is full, overwrites the oldest trace.
     * 
     * @param trace The protocol trace to add
     */
    public void addTrace(ProtocolTrace trace) {
        if (trace == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            buffer[writeIndex] = trace;
            writeIndex = (writeIndex + 1) % capacity;
            
            if (size < capacity) {
                size++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Retrieves all traces that match the given filter.
     * 
     * @param filter Filter criteria for traces
     * @return List of matching traces, ordered by timestamp (newest first)
     */
    public List<ProtocolTrace> getTraces(TraceFilter filter) {
        lock.readLock().lock();
        try {
            List<ProtocolTrace> result = new ArrayList<>();
            
            // Iterate through buffer in reverse chronological order
            for (int i = 0; i < size; i++) {
                int index = (writeIndex - 1 - i + capacity) % capacity;
                ProtocolTrace trace = buffer[index];
                
                if (trace != null && matchesFilter(trace, filter)) {
                    result.add(trace);
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Retrieves all traces in the buffer.
     * 
     * @return List of all traces, ordered by timestamp (newest first)
     */
    public List<ProtocolTrace> getAllTraces() {
        return getTraces(TraceFilter.builder().build());
    }
    
    /**
     * Clears all traces from the buffer.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
            writeIndex = 0;
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current number of traces in the buffer.
     * 
     * @return Current size
     */
    public int size() {
        return size;
    }
    
    /**
     * Gets the maximum capacity of the buffer.
     * 
     * @return Buffer capacity
     */
    public int capacity() {
        return capacity;
    }
    
    /**
     * Sets the retention policy for traces.
     * Traces older than this duration will be considered expired.
     * 
     * @param retention Retention duration
     */
    public void setRetentionPolicy(Duration retention) {
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("Retention policy must be positive");
        }
        this.retentionPolicy = retention;
    }
    
    /**
     * Gets the current retention policy.
     * 
     * @return Current retention duration
     */
    public Duration getRetentionPolicy() {
        return retentionPolicy;
    }
    
    /**
     * Removes expired traces based on the retention policy.
     * This is a manual cleanup operation.
     * 
     * @return Number of traces removed
     */
    public int cleanupExpiredTraces() {
        Instant cutoff = Instant.now().minus(retentionPolicy);
        int removedCount = 0;
        
        lock.writeLock().lock();
        try {
            // Create new buffer without expired traces
            List<ProtocolTrace> validTraces = new ArrayList<>();
            
            for (int i = 0; i < size; i++) {
                int index = (writeIndex - 1 - i + capacity) % capacity;
                ProtocolTrace trace = buffer[index];
                
                if (trace != null && trace.timestamp().isAfter(cutoff)) {
                    validTraces.add(trace);
                } else if (trace != null) {
                    removedCount++;
                }
            }
            
            // Clear buffer and re-add valid traces
            clear();
            for (ProtocolTrace trace : validTraces) {
                addTraceInternal(trace);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
        
        return removedCount;
    }
    
    /**
     * Internal method to add trace without acquiring lock.
     * Must be called within write lock.
     */
    private void addTraceInternal(ProtocolTrace trace) {
        buffer[writeIndex] = trace;
        writeIndex = (writeIndex + 1) % capacity;
        
        if (size < capacity) {
            size++;
        }
    }
    
    /**
     * Checks if a trace matches the given filter criteria.
     */
    private boolean matchesFilter(ProtocolTrace trace, TraceFilter filter) {
        if (filter == null) {
            return true;
        }
        
        // Check time range
        if (filter.timeRange() != null) {
            Instant timestamp = trace.timestamp();
            if (timestamp.isBefore(filter.timeRange().start()) || 
                timestamp.isAfter(filter.timeRange().end())) {
                return false;
            }
        }
        
        // Check client ID
        if (filter.clientId() != null && !filter.clientId().equals(trace.clientId())) {
            return false;
        }
        
        // Check message type
        if (filter.messageType() != null && !filter.messageType().equals(trace.messageType())) {
            return false;
        }
        
        // Check error filter
        if (filter.errorsOnly() && trace.errorMessage().isEmpty()) {
            return false;
        }
        
        return true;
    }
}