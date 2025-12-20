# InMemoryTraceBuffer Class Documentation

## Overview

The `InMemoryTraceBuffer` class is a thread-safe ring buffer implementation designed for storing protocol traces in memory. It provides configurable size and retention policy with concurrent access support for the Git MCP Server's debugging and monitoring capabilities.

## Class Description

The `InMemoryTraceBuffer` is a Spring component that implements a circular buffer pattern to efficiently store and retrieve `ProtocolTrace` objects. It uses a fixed-size array with write index management to overwrite the oldest traces when the buffer reaches capacity.

### Key Features

- **Thread-Safe Operations**: Uses ReadWriteLock for concurrent access
- **Ring Buffer Implementation**: Efficient memory usage with fixed capacity
- **Configurable Retention Policy**: Time-based trace expiration
- **Filtering Support**: Advanced trace filtering capabilities
- **Manual Cleanup**: Explicit expired trace removal

## Public Methods

### Constructors

#### `InMemoryTraceBuffer()`

Creates a trace buffer with default capacity of 10,000 traces.

**Usage:**

```java
InMemoryTraceBuffer buffer = new InMemoryTraceBuffer();
```

#### `InMemoryTraceBuffer(int capacity)`

Creates a trace buffer with specified capacity.

**Parameters:**

- `capacity` - Maximum number of traces to store (must be positive)

**Throws:**

- `IllegalArgumentException` - If capacity is not positive

**Usage:**

```java
InMemoryTraceBuffer buffer = new InMemoryTraceBuffer(5000);
```

### Core Operations

#### `addTrace(ProtocolTrace trace)`

Adds a protocol trace to the buffer. If buffer is full, overwrites the oldest trace.

**Parameters:**

- `trace` - The protocol trace to add (null traces are ignored)

**Behavior:**

- Thread-safe write operation
- Circular buffer overwrite when full
- Atomic size and index updates

#### `getTraces(TraceFilter filter)`

Retrieves all traces that match the given filter criteria.

**Parameters:**

- `filter` - Filter criteria for traces

**Returns:**

- `List<ProtocolTrace>` - Matching traces ordered by timestamp (newest first)

**Features:**

- Thread-safe read operation
- Reverse chronological ordering
- Comprehensive filtering support

#### `getAllTraces()`

Retrieves all traces in the buffer without filtering.

**Returns:**

- `List<ProtocolTrace>` - All traces ordered by timestamp (newest first)

#### `clear()`

Removes all traces from the buffer and resets internal state.

**Behavior:**

- Thread-safe write operation
- Nullifies all buffer entries
- Resets size and write index

### Buffer Information

#### `size()`

Gets the current number of traces in the buffer.

**Returns:**

- `int` - Current number of stored traces

#### `capacity()`

Gets the maximum capacity of the buffer.

**Returns:**

- `int` - Maximum buffer capacity

### Retention Policy Management

#### `setRetentionPolicy(Duration retention)`

Sets the retention policy for traces.

**Parameters:**

- `retention` - Retention duration (must be positive)

**Throws:**

- `IllegalArgumentException` - If retention is null or negative

#### `getRetentionPolicy()`

Gets the current retention policy.

**Returns:**

- `Duration` - Current retention duration

#### `cleanupExpiredTraces()`

Removes expired traces based on the retention policy.

**Returns:**

- `int` - Number of traces removed

**Behavior:**

- Manual cleanup operation
- Thread-safe write operation
- Rebuilds buffer without expired traces

## Flow Diagrams

### Add Trace Flow

```mermaid
flowchart TD
    A[addTrace called] --> B{trace == null?}
    B -->|Yes| C[Return early]
    B -->|No| D[Acquire write lock]
    D --> E[Store trace at writeIndex]
    E --> F[Update writeIndex = next position mod capacity]
    F --> G{size < capacity?}
    G -->|Yes| H[Increment size]
    G -->|No| I[Keep size unchanged]
    H --> J[Release write lock]
    I --> J
    J --> K[Operation complete]
```

### Get Traces Flow

```mermaid
flowchart TD
    A[getTraces called] --> B[Acquire read lock]
    B --> C[Initialize result list]
    C --> D[Start iteration: i = 0]
    D --> E{i < size?}
    E -->|No| F[Release read lock]
    E -->|Yes| G[Calculate reverse index]
    G --> H[Get trace at index]
    H --> I{trace exists and matches filter?}
    I -->|Yes| J[Add trace to result]
    I -->|No| K[Skip trace]
    J --> L[Increment i]
    K --> L
    L --> E
    F --> M[Return result list]
```

### Cleanup Expired Traces Flow

```mermaid
flowchart TD
    A[cleanupExpiredTraces called] --> B[Calculate cutoff = now - retentionPolicy]
    B --> C[Initialize removedCount = 0]
    C --> D[Acquire write lock]
    D --> E[Create validTraces list]
    E --> F[Iterate through buffer]
    F --> G{More traces?}
    G -->|No| H[Clear buffer]
    G -->|Yes| I{trace.timestamp > cutoff?}
    I -->|Yes| J[Add to validTraces]
    I -->|No| K[Increment removedCount]
    J --> L[Continue iteration]
    K --> L
    L --> G
    H --> M[Re-add valid traces]
    M --> N[Release write lock]
    N --> O[Return removedCount]
```

## Dependencies Diagram

```mermaid
classDiagram
    class InMemoryTraceBuffer {
        -ReadWriteLock lock
        -ProtocolTrace[] buffer
        -int capacity
        -volatile int writeIndex
        -volatile int size
        -volatile Duration retentionPolicy
        +addTrace(ProtocolTrace)
        +getTraces(TraceFilter) List~ProtocolTrace~
        +getAllTraces() List~ProtocolTrace~
        +clear()
        +size() int
        +capacity() int
        +setRetentionPolicy(Duration)
        +getRetentionPolicy() Duration
        +cleanupExpiredTraces() int
        -addTraceInternal(ProtocolTrace)
        -matchesFilter(ProtocolTrace, TraceFilter) boolean
    }

    class ProtocolTrace {
        +String traceId
        +Instant timestamp
        +String clientId
        +String messageType
        +Map~String,Object~ parameters
        +Map~String,Object~ response
        +Duration processingTime
        +Optional~String~ errorMessage
    }

    class TraceFilter {
        +String messageType
        +String clientId
        +TimeRange timeRange
        +boolean errorsOnly
        +builder() Builder
        +all() TraceFilter
        +byMessageType(String) TraceFilter
        +byClientId(String) TraceFilter
        +errorsOnly() TraceFilter
    }

    class TimeRange {
        +Instant start
        +Instant end
        +lastDuration(Duration) TimeRange
        +lastHour() TimeRange
        +lastDay() TimeRange
    }

    class TraceFilterBuilder {
        -String messageType
        -String clientId
        -TimeRange timeRange
        -boolean errorsOnly
        +messageType(String) Builder
        +clientId(String) Builder
        +timeRange(TimeRange) Builder
        +errorsOnly(boolean) Builder
        +build() TraceFilter
    }

    class ReadWriteLock {
        <<interface>>
        +readLock() Lock
        +writeLock() Lock
    }

    class ReentrantReadWriteLock {
        +readLock() Lock
        +writeLock() Lock
    }

    class Duration {
        <<Java Time API>>
        +ofHours(long) Duration
        +isNegative() boolean
    }

    class Instant {
        <<Java Time API>>
        +now() Instant
        +minus(Duration) Instant
        +isAfter(Instant) boolean
        +isBefore(Instant) boolean
    }

    InMemoryTraceBuffer --> ProtocolTrace : stores
    InMemoryTraceBuffer --> TraceFilter : uses for filtering
    InMemoryTraceBuffer --> ReadWriteLock : uses for synchronization
    InMemoryTraceBuffer --> Duration : uses for retention policy
    TraceFilter --> TimeRange : contains
    TraceFilter --> TraceFilterBuilder : creates via builder
    TimeRange --> Instant : contains start/end
    TimeRange --> Duration : uses for calculations
    ProtocolTrace --> Instant : contains timestamp
    ProtocolTrace --> Duration : contains processingTime
    ReadWriteLock <|.. ReentrantReadWriteLock : implements

    %% Spring Framework Dependencies
    InMemoryTraceBuffer : @Component
```

## Thread Safety

The `InMemoryTraceBuffer` implements thread safety through:

### ReadWriteLock Strategy

- **Read Operations**: Multiple concurrent readers allowed
- **Write Operations**: Exclusive access required
- **Lock Granularity**: Method-level locking

### Volatile Fields

- `writeIndex`: Ensures visibility of write position
- `size`: Ensures visibility of current buffer size
- `retentionPolicy`: Ensures visibility of policy changes

### Atomic Operations

- Buffer updates are atomic within write locks
- Index calculations use modulo arithmetic for wraparound
- Size management prevents race conditions

## Performance Characteristics

### Time Complexity

- **Add Trace**: O(1) - Constant time insertion
- **Get Traces**: O(n) - Linear scan with filtering
- **Clear**: O(n) - Must nullify all entries
- **Cleanup**: O(n) - Must scan and rebuild buffer

### Space Complexity

- **Memory Usage**: O(capacity) - Fixed array size
- **Additional Storage**: O(1) - Minimal overhead

### Concurrency Performance

- **Read Scalability**: High - Multiple concurrent readers
- **Write Contention**: Minimal - Fast write operations
- **Lock Overhead**: Low - Efficient ReadWriteLock implementation

## Usage Examples

### Basic Usage

```java
// Create buffer with default capacity
InMemoryTraceBuffer buffer = new InMemoryTraceBuffer();

// Add traces
ProtocolTrace trace = new ProtocolTrace(
    "trace-001",
    Instant.now(),
    "client-123",
    "git-status",
    Map.of("repository", "/path/to/repo"),
    Map.of("status", "clean"),
    Duration.ofMillis(150),
    Optional.empty()
);
buffer.addTrace(trace);

// Retrieve all traces
List<ProtocolTrace> allTraces = buffer.getAllTraces();

// Filter traces by message type
TraceFilter filter = TraceFilter.byMessageType("git-status");
List<ProtocolTrace> statusTraces = buffer.getTraces(filter);
```

### Advanced Filtering

```java
// Complex filter with multiple criteria
TraceFilter complexFilter = TraceFilter.builder()
    .messageType("git-commit")
    .clientId("client-123")
    .timeRange(TimeRange.lastHour())
    .errorsOnly(false)
    .build();

List<ProtocolTrace> filteredTraces = buffer.getTraces(complexFilter);
```

### Retention Management

```java
// Set 2-hour retention policy
buffer.setRetentionPolicy(Duration.ofHours(2));

// Manual cleanup
int removedCount = buffer.cleanupExpiredTraces();
System.out.println("Removed " + removedCount + " expired traces");
```

## Integration Points

### Spring Framework

- Annotated with `@Component` for dependency injection
- Singleton scope by default
- Can be autowired into other components

### Debug System

- Central storage for protocol traces
- Integrates with trace collection mechanisms
- Supports debugging and monitoring workflows

### MCP Protocol

- Stores traces of MCP message exchanges
- Supports protocol analysis and debugging
- Enables performance monitoring
