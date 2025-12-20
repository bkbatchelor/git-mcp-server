# Ring Buffer Full Buffer Handling

## Overview

The `InMemoryTraceBuffer` implements a circular ring buffer for storing protocol traces with automatic overflow handling. When the buffer reaches its maximum capacity, it employs a **circular overwrite strategy** where new traces automatically replace the oldest traces.

## Buffer Structure

- **Fixed Capacity**: The buffer has a predetermined maximum size (default: 10,000 traces)
- **Circular Array**: Uses a fixed-size array with circular indexing
- **Write Index**: Tracks the next position for new trace insertion
- **Size Counter**: Maintains current number of stored traces

## Full Buffer Handling Strategy

### 1. Circular Overwrite Behavior

When the buffer is full and a new trace arrives:

```java
buffer[writeIndex] = trace;
writeIndex = (writeIndex + 1) % capacity;

if (size < capacity) {
    size++;
}
```

**Key Characteristics:**
- **No Blocking**: New traces are always accepted, never rejected
- **Automatic Replacement**: Oldest traces are silently overwritten
- **Constant Time**: O(1) insertion regardless of buffer state
- **Circular Indexing**: Write index wraps around using modulo operation

### 2. Memory Management

**Fixed Memory Footprint:**
- Buffer size remains constant regardless of usage patterns
- No dynamic allocation during normal operation
- Predictable memory consumption for capacity planning

**Garbage Collection Friendly:**
- Old trace references are immediately replaced
- No memory leaks from accumulated traces
- Bounded memory usage prevents OutOfMemoryError

### 3. Thread Safety

**Concurrent Access Protection:**
- Uses `ReadWriteLock` for thread-safe operations
- Multiple readers can access simultaneously
- Writers have exclusive access during insertion
- Lock-free size and capacity queries

### 4. Data Retrieval Order

**Chronological Access:**
- Traces retrieved in reverse chronological order (newest first)
- Complex indexing calculation to maintain temporal ordering:

```java
int index = (writeIndex - 1 - i + capacity) % capacity;
```

**Iteration Logic:**
- Starts from most recently written position
- Works backward through the circular buffer
- Handles wrap-around automatically

### 5. Retention Policy Integration

**Time-Based Cleanup:**
- Manual cleanup removes traces older than retention period
- Preserves buffer structure while removing expired data
- Returns count of removed traces for monitoring

**Cleanup Process:**
1. Calculate cutoff timestamp based on retention policy
2. Collect all valid (non-expired) traces
3. Clear entire buffer
4. Re-insert valid traces in chronological order

## Advantages of This Approach

1. **Predictable Performance**: Constant-time operations regardless of load
2. **Memory Bounded**: Fixed memory usage prevents resource exhaustion
3. **Always Available**: Never blocks or rejects new data
4. **Recent Data Priority**: Automatically maintains most recent traces
5. **Production Safe**: Suitable for high-throughput production environments

## Trade-offs

1. **Data Loss**: Oldest traces are permanently lost when overwritten
2. **No Backpressure**: No mechanism to slow down trace generation
3. **Fixed Capacity**: Cannot dynamically adjust to varying loads
4. **Manual Cleanup**: Retention policy requires explicit cleanup calls

## Configuration Considerations

- **Capacity Sizing**: Balance memory usage vs. trace history depth
- **Retention Policy**: Set appropriate time-based cleanup intervals
- **Monitoring**: Track buffer utilization and overflow frequency
- **Cleanup Scheduling**: Regular cleanup prevents unbounded growth of expired traces

This ring buffer design prioritizes **reliability** and **performance** over **data completeness**, making it ideal for real-time debugging scenarios where recent data is most valuable and system stability is paramount.