# FileBasedStorage Class Documentation

## Overview

The `FileBasedStorage` class is a Spring component that provides persistent storage for MCP protocol traces using a file-based approach. It implements compressed archives with searchable indexes and configurable retention policies, making it suitable for long-term storage and historical analysis of debugging data.

## Key Features

- **Date-based Organization**: Traces are organized by date for efficient retrieval
- **Compression Support**: Optional GZIP compression to reduce storage space
- **Searchable Indexes**: Fast filtering using metadata indexes
- **Batch Operations**: Optimized batch storage for better performance
- **Configurable Retention**: Automatic cleanup of expired traces
- **Thread Safety**: Concurrent read/write operations with ReadWriteLock
- **Index Caching**: In-memory caching of index files for performance

## Class Structure

### Dependencies

```mermaid
graph TD
    A[FileBasedStorage] --> B[ObjectMapper]
    A --> C[JavaTimeModule]
    A --> D[ProtocolTrace]
    A --> E[TraceFilter]
    A --> F[TimeRange]
    A --> G[ReadWriteLock]
    A --> H["Path/Files"]
    A --> I["GZIP Streams"]
    A --> J["Spring @Component"]
    
    B --> K["Jackson JSON"]
    C --> L["Java Time API"]
    D --> M["Debug Model"]
    E --> M
    F --> M
    G --> N["java.util.concurrent"]
    H --> O["java.nio.file"]
    I --> P["java.util.zip"]
```

### Internal Data Structures

```mermaid
classDiagram
    class FileBasedStorage {
        -ObjectMapper objectMapper
        -ReadWriteLock lock
        -Map indexCache
        -Path basePath
        -Duration retentionPolicy
        -boolean compressionEnabled
        +setBasePath(Path)
        +setRetentionPolicy(Duration)
        +setCompressionEnabled(boolean)
        +storeTrace(ProtocolTrace)
        +storeTraces(List)
        +getTraces(TraceFilter)
        +cleanupExpiredTraces()
        +getStorageStats()
    }
    
    class TraceFileIndex {
        +int traceCount
        +Set messageTypes
        +Set clientIds
        +Instant earliestTimestamp
        +Instant latestTimestamp
    }
    
    class StorageStats {
        +long totalSizeBytes
        +int fileCount
        +int traceCount
    }
    
    FileBasedStorage --> TraceFileIndex
    FileBasedStorage --> StorageStats
```

## Public Methods

### Configuration Methods

#### `setBasePath(Path basePath)`
**Purpose**: Sets the base directory path for storage files  
**Parameters**: 
- `basePath`: Path object representing the storage directory
**Behavior**: 
- Creates the directory if it doesn't exist
- Throws RuntimeException if directory creation fails
**Thread Safety**: Not thread-safe, should be called during initialization

#### `setRetentionPolicy(Duration retention)`
**Purpose**: Configures how long traces are kept before cleanup  
**Parameters**: 
- `retention`: Duration object (must be positive)
**Validation**: 
- Throws IllegalArgumentException for null or negative durations
**Thread Safety**: Thread-safe

#### `setCompressionEnabled(boolean enabled)`
**Purpose**: Enables or disables GZIP compression for stored files  
**Parameters**: 
- `enabled`: Boolean flag for compression
**Impact**: 
- Reduces storage space when enabled
- Slightly increases CPU usage for compression/decompression
**Thread Safety**: Thread-safe

### Storage Operations

#### `storeTrace(ProtocolTrace trace)`
**Purpose**: Stores a single protocol trace to persistent storage  
**Parameters**: 
- `trace`: ProtocolTrace object to store
**Behavior**: 
- Organizes traces by date (YYYY-MM-DD format)
- Appends to existing daily file or creates new one
- Updates searchable index with trace metadata
- Uses write lock for thread safety
**File Structure**: 
- Trace file: `YYYY-MM-DD.traces.gz`
- Index file: `YYYY-MM-DD.index.json`

#### `storeTraces(List<ProtocolTrace> traces)`
**Purpose**: Stores multiple traces in a batch operation for better performance  
**Parameters**: 
- `traces`: List of ProtocolTrace objects
**Optimization**: 
- Groups traces by date to minimize file operations
- Single write lock acquisition for entire batch
- Bulk index updates
**Performance**: Significantly faster than individual `storeTrace()` calls

### Retrieval Operations

#### `getTraces(TraceFilter filter)`
**Purpose**: Retrieves traces matching the specified filter criteria  
**Parameters**: 
- `filter`: TraceFilter object with search criteria
**Returns**: List of ProtocolTrace objects sorted by timestamp (newest first)
**Optimization Strategy**:
1. Determines relevant date files based on time range
2. Uses index files for quick filtering
3. Only loads and parses files that might contain matching traces
4. Applies detailed filtering on loaded traces

**Filter Criteria Supported**:
- Time range filtering
- Client ID filtering  
- Message type filtering
- Error-only filtering

### Maintenance Operations

#### `cleanupExpiredTraces()`
**Purpose**: Removes traces older than the configured retention policy  
**Returns**: Integer count of deleted files
**Behavior**: 
- Calculates cutoff date based on retention policy
- Deletes both trace files and index files
- Removes entries from index cache
- Uses write lock for thread safety
**Scheduling**: Should be called periodically (e.g., daily cron job)

#### `getStorageStats()`
**Purpose**: Provides statistics about current storage usage  
**Returns**: StorageStats record with:
- `totalSizeBytes`: Total disk space used
- `fileCount`: Number of trace files
- `traceCount`: Total number of stored traces
**Use Cases**: 
- Monitoring storage growth
- Capacity planning
- Performance analysis

## File Organization

### Directory Structure

```text
debug-data/
├── 2024-12-15.traces.gz     # Compressed trace data
├── 2024-12-15.index.json    # Searchable index
├── 2024-12-16.traces.gz
├── 2024-12-16.index.json
└── ...
```

### File Formats

#### Trace File Format
- **Uncompressed**: JSONL (JSON Lines) format
- **Compressed**: GZIP-compressed JSONL
- **Structure**: One JSON object per line, each representing a ProtocolTrace

#### Index File Format
```json
{
  "traceCount": 1250,
  "messageTypes": ["tool-invocation", "resource-request"],
  "clientIds": ["client-123", "client-456"],
  "earliestTimestamp": "2024-12-15T00:00:00Z",
  "latestTimestamp": "2024-12-15T23:59:59Z"
}
```

## Flow Diagrams

### Storage Flow

```mermaid
flowchart TD
    A[storeTrace/storeTraces] --> B{Null check}
    B -->|Null| C[Return early]
    B -->|Valid| D[Extract date key from timestamp]
    D --> E[Acquire write lock]
    E --> F[Create directories if needed]
    F --> G[Append to trace file]
    G --> H{Compression enabled?}
    H -->|Yes| I[Write with GZIP compression]
    H -->|No| J[Write as plain text]
    I --> K[Update index file]
    J --> K
    K --> L[Update index cache]
    L --> M[Release write lock]
    M --> N[Complete]
```

### Retrieval Flow

```mermaid
flowchart TD
    A[getTraces with filter] --> B[Acquire read lock]
    B --> C[Determine relevant date keys]
    C --> D{Time range in filter?}
    D -->|Yes| E[Generate date keys for range]
    D -->|No| F[Scan all trace files]
    E --> G[For each date key]
    F --> G
    G --> H[Load index file]
    H --> I{Index matches filter?}
    I -->|No| J[Skip this date]
    I -->|Yes| K[Load trace file]
    K --> L{Compression enabled?}
    L -->|Yes| M[Read with GZIP decompression]
    L -->|No| N[Read as plain text]
    M --> O[Parse JSON lines]
    N --> O
    O --> P[Apply detailed filtering]
    P --> Q[Add matching traces to results]
    Q --> R[Sort by timestamp desc]
    R --> S[Release read lock]
    S --> T[Return results]
    J --> R
```

### Cleanup Flow

```mermaid
flowchart TD
    A[cleanupExpiredTraces] --> B[Calculate cutoff date]
    B --> C[Acquire write lock]
    C --> D[Scan storage directory]
    D --> E[For each file]
    E --> F[Extract date from filename]
    F --> G{Date before cutoff?}
    G -->|Yes| H[Delete file]
    G -->|No| I[Keep file]
    H --> J[Remove from index cache]
    J --> K[Increment deleted count]
    K --> L[Continue to next file]
    I --> L
    L --> M{More files?}
    M -->|Yes| E
    M -->|No| N[Release write lock]
    N --> O[Return deleted count]
```

## Thread Safety

### Locking Strategy
- **ReadWriteLock**: Allows multiple concurrent readers or single writer
- **Read Operations**: `getTraces()`, `getStorageStats()` use read lock
- **Write Operations**: `storeTrace()`, `storeTraces()`, `cleanupExpiredTraces()` use write lock
- **Configuration**: Setter methods are not synchronized (initialization only)

### Concurrent Access Patterns
```mermaid
sequenceDiagram
    participant R1 as "Reader 1"
    participant R2 as "Reader 2"
    participant W as Writer
    participant L as ReadWriteLock
    
    R1->>L: Acquire read lock
    R2->>L: Acquire read lock (allowed)
    R1->>R1: Read traces
    R2->>R2: Read traces
    W->>L: Request write lock (blocked)
    R1->>L: Release read lock
    R2->>L: Release read lock
    W->>L: Acquire write lock (now allowed)
    W->>W: Store traces
    W->>L: Release write lock
```

## Performance Considerations

### Optimization Strategies
1. **Index-based Filtering**: Quick elimination of irrelevant files
2. **Batch Operations**: Reduced lock contention and I/O operations
3. **Index Caching**: In-memory cache for frequently accessed indexes
4. **Date-based Partitioning**: Limits search scope for time-range queries
5. **Compression**: Reduces I/O and storage costs

### Memory Usage
- **Index Cache**: Bounded by number of unique dates
- **Streaming**: Large files processed line-by-line
- **Batch Size**: Configurable batch sizes prevent memory exhaustion

### I/O Patterns
- **Sequential Writes**: Append-only operations for optimal disk performance
- **Selective Reads**: Only reads files that might contain matching data
- **Compression Trade-off**: CPU vs. I/O and storage space

## Error Handling

### Exception Types
- **RuntimeException**: Wraps I/O errors with contextual information
- **IllegalArgumentException**: Invalid configuration parameters
- **IOException**: File system operations (wrapped in RuntimeException)

### Recovery Strategies
- **Corrupted Index**: Rebuilds index from trace file if needed
- **Corrupted Trace Lines**: Skips individual corrupted lines, continues processing
- **Missing Directories**: Automatically creates required directories
- **File Access Errors**: Fails fast with descriptive error messages

## Configuration Examples

### Basic Configuration
```java
FileBasedStorage storage = new FileBasedStorage();
storage.setBasePath(Paths.get("/var/log/mcp-debug"));
storage.setRetentionPolicy(Duration.ofDays(30));
storage.setCompressionEnabled(true);
```

### Production Configuration
```java
// High-performance production setup
storage.setBasePath(Paths.get("/fast-ssd/mcp-traces"));
storage.setRetentionPolicy(Duration.ofDays(90));
storage.setCompressionEnabled(true); // Save storage space

// Schedule cleanup
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(
    storage::cleanupExpiredTraces,
    0, 24, TimeUnit.HOURS
);
```

This implementation provides a robust, scalable solution for persistent trace storage with excellent performance characteristics and operational flexibility.