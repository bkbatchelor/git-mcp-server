package io.sandboxdev.gitmcp.debug.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import io.sandboxdev.gitmcp.debug.model.TimeRange;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File-based storage implementation for persistent protocol trace storage.
 * Provides compressed archives with searchable indexes and configurable retention.
 */
@Component
public class FileBasedStorage {
    
    private static final String TRACE_FILE_EXTENSION = ".traces.gz";
    private static final String INDEX_FILE_EXTENSION = ".index.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, TraceFileIndex> indexCache = new ConcurrentHashMap<>();
    
    private Path basePath = Paths.get("debug-data");
    private Duration retentionPolicy = Duration.ofDays(7);
    private boolean compressionEnabled = true;
    
    public FileBasedStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Sets the base path for storage files.
     */
    public void setBasePath(Path basePath) {
        this.basePath = basePath;
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + basePath, e);
        }
    }
    
    /**
     * Sets the retention policy for stored traces.
     */
    public void setRetentionPolicy(Duration retention) {
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("Retention policy must be positive");
        }
        this.retentionPolicy = retention;
    }
    
    /**
     * Enables or disables compression for stored files.
     */
    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
    }
    
    /**
     * Stores a protocol trace to persistent storage.
     * Traces are organized by date for efficient retrieval.
     */
    public void storeTrace(ProtocolTrace trace) {
        if (trace == null) {
            return;
        }
        
        String dateKey = getDateKey(trace.timestamp());
        Path traceFile = getTraceFilePath(dateKey);
        Path indexFile = getIndexFilePath(dateKey);
        
        lock.writeLock().lock();
        try {
            // Ensure directory exists
            Files.createDirectories(traceFile.getParent());
            
            // Append trace to file
            appendTraceToFile(trace, traceFile);
            
            // Update index
            updateIndex(trace, indexFile, dateKey);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store trace: " + trace.traceId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Stores multiple traces in a batch operation for better performance.
     */
    public void storeTraces(List<ProtocolTrace> traces) {
        if (traces == null || traces.isEmpty()) {
            return;
        }
        
        // Group traces by date for efficient storage
        Map<String, List<ProtocolTrace>> tracesByDate = traces.stream()
            .collect(Collectors.groupingBy(trace -> getDateKey(trace.timestamp())));
        
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, List<ProtocolTrace>> entry : tracesByDate.entrySet()) {
                String dateKey = entry.getKey();
                List<ProtocolTrace> dailyTraces = entry.getValue();
                
                Path traceFile = getTraceFilePath(dateKey);
                Path indexFile = getIndexFilePath(dateKey);
                
                // Ensure directory exists
                Files.createDirectories(traceFile.getParent());
                
                // Append all traces for this date
                appendTracesToFile(dailyTraces, traceFile);
                
                // Update index for all traces
                updateIndexBatch(dailyTraces, indexFile, dateKey);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store traces batch", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Retrieves traces matching the specified filter.
     */
    public List<ProtocolTrace> getTraces(TraceFilter filter) {
        lock.readLock().lock();
        try {
            Set<String> dateKeys = getRelevantDateKeys(filter);
            List<ProtocolTrace> results = new ArrayList<>();
            
            for (String dateKey : dateKeys) {
                List<ProtocolTrace> dailyTraces = loadTracesFromFile(dateKey, filter);
                results.addAll(dailyTraces);
            }
            
            // Sort by timestamp (newest first)
            results.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
            
            return results;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Performs cleanup of expired traces based on retention policy.
     */
    public int cleanupExpiredTraces() {
        Instant cutoff = Instant.now().minus(retentionPolicy);
        LocalDate cutoffDate = LocalDate.ofInstant(cutoff, ZoneOffset.UTC);
        
        lock.writeLock().lock();
        try {
            int deletedFiles = 0;
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(basePath)) {
                for (Path path : stream) {
                    if (Files.isRegularFile(path)) {
                        String fileName = path.getFileName().toString();
                        
                        if (fileName.endsWith(TRACE_FILE_EXTENSION) || fileName.endsWith(INDEX_FILE_EXTENSION)) {
                            String dateStr = extractDateFromFileName(fileName);
                            if (dateStr != null) {
                                LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                                if (fileDate.isBefore(cutoffDate)) {
                                    Files.deleteIfExists(path);
                                    deletedFiles++;
                                    
                                    // Remove from cache
                                    indexCache.remove(dateStr);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to cleanup expired traces", e);
            }
            
            return deletedFiles;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets storage statistics.
     */
    public StorageStats getStorageStats() {
        lock.readLock().lock();
        try {
            long totalSize = 0;
            int fileCount = 0;
            int traceCount = 0;
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(basePath)) {
                for (Path path : stream) {
                    if (Files.isRegularFile(path)) {
                        String fileName = path.getFileName().toString();
                        
                        if (fileName.endsWith(TRACE_FILE_EXTENSION)) {
                            totalSize += Files.size(path);
                            fileCount++;
                            
                            // Count traces in index
                            String dateKey = extractDateFromFileName(fileName);
                            if (dateKey != null) {
                                TraceFileIndex index = loadIndex(dateKey);
                                if (index != null) {
                                    traceCount += index.traceCount();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to calculate storage stats", e);
            }
            
            return new StorageStats(totalSize, fileCount, traceCount);
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Private helper methods
    
    private String getDateKey(Instant timestamp) {
        return LocalDate.ofInstant(timestamp, ZoneOffset.UTC).format(DATE_FORMATTER);
    }
    
    private Path getTraceFilePath(String dateKey) {
        return basePath.resolve(dateKey + TRACE_FILE_EXTENSION);
    }
    
    private Path getIndexFilePath(String dateKey) {
        return basePath.resolve(dateKey + INDEX_FILE_EXTENSION);
    }
    
    private void appendTraceToFile(ProtocolTrace trace, Path traceFile) throws IOException {
        String jsonLine = objectMapper.writeValueAsString(trace) + "\n";
        
        if (compressionEnabled) {
            try (FileOutputStream fos = new FileOutputStream(traceFile.toFile(), true);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 OutputStreamWriter writer = new OutputStreamWriter(gzos)) {
                writer.write(jsonLine);
            }
        } else {
            Files.write(traceFile, jsonLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
    
    private void appendTracesToFile(List<ProtocolTrace> traces, Path traceFile) throws IOException {
        if (compressionEnabled) {
            try (FileOutputStream fos = new FileOutputStream(traceFile.toFile(), true);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 OutputStreamWriter writer = new OutputStreamWriter(gzos)) {
                
                for (ProtocolTrace trace : traces) {
                    String jsonLine = objectMapper.writeValueAsString(trace) + "\n";
                    writer.write(jsonLine);
                }
            }
        } else {
            StringBuilder content = new StringBuilder();
            for (ProtocolTrace trace : traces) {
                content.append(objectMapper.writeValueAsString(trace)).append("\n");
            }
            Files.write(traceFile, content.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
    
    private void updateIndex(ProtocolTrace trace, Path indexFile, String dateKey) throws IOException {
        TraceFileIndex index = loadIndex(dateKey);
        if (index == null) {
            index = new TraceFileIndex(0, new HashSet<>(), new HashSet<>(), 
                                     trace.timestamp(), trace.timestamp());
        }
        
        Set<String> messageTypes = new HashSet<>(index.messageTypes());
        messageTypes.add(trace.messageType());
        
        Set<String> clientIds = new HashSet<>(index.clientIds());
        clientIds.add(trace.clientId());
        
        Instant earliestTime = trace.timestamp().isBefore(index.earliestTimestamp()) ? 
                              trace.timestamp() : index.earliestTimestamp();
        Instant latestTime = trace.timestamp().isAfter(index.latestTimestamp()) ? 
                            trace.timestamp() : index.latestTimestamp();
        
        TraceFileIndex updatedIndex = new TraceFileIndex(
            index.traceCount() + 1,
            messageTypes,
            clientIds,
            earliestTime,
            latestTime
        );
        
        String indexJson = objectMapper.writeValueAsString(updatedIndex);
        Files.write(indexFile, indexJson.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // Update cache
        indexCache.put(dateKey, updatedIndex);
    }
    
    private void updateIndexBatch(List<ProtocolTrace> traces, Path indexFile, String dateKey) throws IOException {
        TraceFileIndex index = loadIndex(dateKey);
        if (index == null) {
            index = new TraceFileIndex(0, new HashSet<>(), new HashSet<>(), 
                                     Instant.MAX, Instant.MIN);
        }
        
        Set<String> messageTypes = new HashSet<>(index.messageTypes());
        Set<String> clientIds = new HashSet<>(index.clientIds());
        Instant earliestTime = index.earliestTimestamp();
        Instant latestTime = index.latestTimestamp();
        
        for (ProtocolTrace trace : traces) {
            messageTypes.add(trace.messageType());
            clientIds.add(trace.clientId());
            
            if (trace.timestamp().isBefore(earliestTime)) {
                earliestTime = trace.timestamp();
            }
            if (trace.timestamp().isAfter(latestTime)) {
                latestTime = trace.timestamp();
            }
        }
        
        TraceFileIndex updatedIndex = new TraceFileIndex(
            index.traceCount() + traces.size(),
            messageTypes,
            clientIds,
            earliestTime,
            latestTime
        );
        
        String indexJson = objectMapper.writeValueAsString(updatedIndex);
        Files.write(indexFile, indexJson.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // Update cache
        indexCache.put(dateKey, updatedIndex);
    }
    
    private TraceFileIndex loadIndex(String dateKey) {
        // Check cache first
        TraceFileIndex cached = indexCache.get(dateKey);
        if (cached != null) {
            return cached;
        }
        
        Path indexFile = getIndexFilePath(dateKey);
        if (!Files.exists(indexFile)) {
            return null;
        }
        
        try {
            String indexJson = Files.readString(indexFile);
            TraceFileIndex index = objectMapper.readValue(indexJson, TraceFileIndex.class);
            indexCache.put(dateKey, index);
            return index;
        } catch (IOException e) {
            // Index file corrupted, return null to rebuild
            return null;
        }
    }
    
    private Set<String> getRelevantDateKeys(TraceFilter filter) {
        Set<String> dateKeys = new HashSet<>();
        
        if (filter == null || filter.timeRange() == null) {
            // No time filter, check all available files
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(basePath)) {
                for (Path path : stream) {
                    if (Files.isRegularFile(path)) {
                        String fileName = path.getFileName().toString();
                        if (fileName.endsWith(TRACE_FILE_EXTENSION)) {
                            String dateKey = extractDateFromFileName(fileName);
                            if (dateKey != null) {
                                dateKeys.add(dateKey);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to list trace files", e);
            }
        } else {
            // Generate date keys for the time range
            TimeRange timeRange = filter.timeRange();
            LocalDate startDate = LocalDate.ofInstant(timeRange.start(), ZoneOffset.UTC);
            LocalDate endDate = LocalDate.ofInstant(timeRange.end(), ZoneOffset.UTC);
            
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String dateKey = current.format(DATE_FORMATTER);
                if (Files.exists(getTraceFilePath(dateKey))) {
                    dateKeys.add(dateKey);
                }
                current = current.plusDays(1);
            }
        }
        
        return dateKeys;
    }
    
    private List<ProtocolTrace> loadTracesFromFile(String dateKey, TraceFilter filter) {
        Path traceFile = getTraceFilePath(dateKey);
        if (!Files.exists(traceFile)) {
            return Collections.emptyList();
        }
        
        // Check index for quick filtering
        TraceFileIndex index = loadIndex(dateKey);
        if (index != null && !indexMatchesFilter(index, filter)) {
            return Collections.emptyList();
        }
        
        List<ProtocolTrace> traces = new ArrayList<>();
        
        try {
            InputStream inputStream;
            if (compressionEnabled) {
                inputStream = new GZIPInputStream(new FileInputStream(traceFile.toFile()));
            } else {
                inputStream = new FileInputStream(traceFile.toFile());
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        try {
                            ProtocolTrace trace = objectMapper.readValue(line, ProtocolTrace.class);
                            if (matchesFilter(trace, filter)) {
                                traces.add(trace);
                            }
                        } catch (IOException e) {
                            // Skip corrupted lines
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load traces from file: " + traceFile, e);
        }
        
        return traces;
    }
    
    private boolean indexMatchesFilter(TraceFileIndex index, TraceFilter filter) {
        if (filter == null) {
            return true;
        }
        
        // Check time range
        if (filter.timeRange() != null) {
            TimeRange timeRange = filter.timeRange();
            if (index.latestTimestamp().isBefore(timeRange.start()) ||
                index.earliestTimestamp().isAfter(timeRange.end())) {
                return false;
            }
        }
        
        // Check message type
        if (filter.messageType() != null && !index.messageTypes().contains(filter.messageType())) {
            return false;
        }
        
        // Check client ID
        if (filter.clientId() != null && !index.clientIds().contains(filter.clientId())) {
            return false;
        }
        
        return true;
    }
    
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
    
    private String extractDateFromFileName(String fileName) {
        if (fileName.endsWith(TRACE_FILE_EXTENSION)) {
            return fileName.substring(0, fileName.length() - TRACE_FILE_EXTENSION.length());
        } else if (fileName.endsWith(INDEX_FILE_EXTENSION)) {
            return fileName.substring(0, fileName.length() - INDEX_FILE_EXTENSION.length());
        }
        return null;
    }
    
    /**
     * Index information for a trace file.
     */
    public record TraceFileIndex(
        int traceCount,
        Set<String> messageTypes,
        Set<String> clientIds,
        Instant earliestTimestamp,
        Instant latestTimestamp
    ) {}
    
    /**
     * Storage statistics.
     */
    public record StorageStats(
        long totalSizeBytes,
        int fileCount,
        int traceCount
    ) {}
}