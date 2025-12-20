package io.sandboxdev.gitmcp.debug.storage;

import io.sandboxdev.gitmcp.debug.model.PerformanceMetrics;
import io.sandboxdev.gitmcp.debug.model.TimeRange;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Time-series storage implementation for performance metrics.
 * Provides efficient aggregation, querying, and downsampling capabilities.
 */
@Component
public class MetricsDatabase {
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Raw metrics storage - organized by tool name and time buckets
    private final Map<String, NavigableMap<Instant, List<PerformanceMetrics>>> rawMetrics = new ConcurrentHashMap<>();
    
    // Aggregated metrics storage for different time resolutions
    private final Map<String, NavigableMap<Instant, AggregatedMetrics>> minutelyAggregates = new ConcurrentHashMap<>();
    private final Map<String, NavigableMap<Instant, AggregatedMetrics>> hourlyAggregates = new ConcurrentHashMap<>();
    private final Map<String, NavigableMap<Instant, AggregatedMetrics>> dailyAggregates = new ConcurrentHashMap<>();
    
    private Duration retentionPolicy = Duration.ofDays(30);
    private Duration rawDataRetention = Duration.ofHours(24);
    
    /**
     * Stores a performance metric.
     */
    public void storeMetric(PerformanceMetrics metric) {
        if (metric == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            // Store raw metric
            storeRawMetric(metric);
            
            // Update aggregates
            updateAggregates(metric);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Stores multiple metrics in a batch operation.
     */
    public void storeMetrics(List<PerformanceMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            for (PerformanceMetrics metric : metrics) {
                storeRawMetric(metric);
                updateAggregates(metric);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Retrieves raw metrics for a specific tool within a time range.
     */
    public List<PerformanceMetrics> getRawMetrics(String toolName, TimeRange timeRange) {
        lock.readLock().lock();
        try {
            NavigableMap<Instant, List<PerformanceMetrics>> toolMetrics = rawMetrics.get(toolName);
            if (toolMetrics == null) {
                return Collections.emptyList();
            }
            
            List<PerformanceMetrics> result = new ArrayList<>();
            
            NavigableMap<Instant, List<PerformanceMetrics>> rangeMetrics = toolMetrics.subMap(
                timeRange.start(), true,
                timeRange.end(), true
            );
            
            for (List<PerformanceMetrics> bucketMetrics : rangeMetrics.values()) {
                for (PerformanceMetrics metric : bucketMetrics) {
                    if (!metric.timestamp().isBefore(timeRange.start()) && 
                        !metric.timestamp().isAfter(timeRange.end())) {
                        result.add(metric);
                    }
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Retrieves aggregated metrics with specified resolution.
     */
    public List<AggregatedMetrics> getAggregatedMetrics(String toolName, TimeRange timeRange, 
                                                       AggregationResolution resolution) {
        lock.readLock().lock();
        try {
            Map<String, NavigableMap<Instant, AggregatedMetrics>> aggregateStore = getAggregateStore(resolution);
            NavigableMap<Instant, AggregatedMetrics> toolAggregates = aggregateStore.get(toolName);
            
            if (toolAggregates == null) {
                return Collections.emptyList();
            }
            
            NavigableMap<Instant, AggregatedMetrics> rangeAggregates = toolAggregates.subMap(
                timeRange.start(), true,
                timeRange.end(), true
            );
            
            return new ArrayList<>(rangeAggregates.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Retrieves metrics for all tools within a time range.
     */
    public Map<String, List<PerformanceMetrics>> getAllRawMetrics(TimeRange timeRange) {
        lock.readLock().lock();
        try {
            Map<String, List<PerformanceMetrics>> result = new HashMap<>();
            
            for (Map.Entry<String, NavigableMap<Instant, List<PerformanceMetrics>>> entry : rawMetrics.entrySet()) {
                String toolName = entry.getKey();
                List<PerformanceMetrics> toolMetrics = getRawMetrics(toolName, timeRange);
                if (!toolMetrics.isEmpty()) {
                    result.put(toolName, toolMetrics);
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Calculates statistics for a tool within a time range.
     */
    public MetricsStatistics calculateStatistics(String toolName, TimeRange timeRange) {
        List<PerformanceMetrics> metrics = getRawMetrics(toolName, timeRange);
        
        if (metrics.isEmpty()) {
            return new MetricsStatistics(0, Duration.ZERO, Duration.ZERO, Duration.ZERO, 
                                       Duration.ZERO, 0, 0, 0.0);
        }
        
        List<Duration> responseTimes = metrics.stream()
            .map(PerformanceMetrics::responseTime)
            .sorted()
            .collect(Collectors.toList());
        
        long totalRequests = metrics.size();
        long successfulRequests = metrics.stream()
            .mapToLong(m -> m.success() ? 1 : 0)
            .sum();
        
        Duration minResponseTime = responseTimes.get(0);
        Duration maxResponseTime = responseTimes.get(responseTimes.size() - 1);
        
        Duration avgResponseTime = Duration.ofNanos(
            (long) responseTimes.stream()
                .mapToLong(Duration::toNanos)
                .average()
                .orElse(0)
        );
        
        Duration p95ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.95));
        
        double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        
        return new MetricsStatistics(
            totalRequests,
            minResponseTime,
            maxResponseTime,
            avgResponseTime,
            p95ResponseTime,
            successfulRequests,
            totalRequests - successfulRequests,
            successRate
        );
    }
    
    /**
     * Gets all available tool names.
     */
    public Set<String> getAvailableTools() {
        lock.readLock().lock();
        try {
            return new HashSet<>(rawMetrics.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Sets the retention policy for metrics.
     */
    public void setRetentionPolicy(Duration retention) {
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("Retention policy must be positive");
        }
        this.retentionPolicy = retention;
    }
    
    /**
     * Sets the raw data retention policy.
     */
    public void setRawDataRetention(Duration retention) {
        if (retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("Raw data retention must be positive");
        }
        this.rawDataRetention = retention;
    }
    
    /**
     * Performs cleanup of expired metrics based on retention policies.
     */
    public int cleanupExpiredMetrics() {
        Instant rawCutoff = Instant.now().minus(rawDataRetention);
        Instant aggregateCutoff = Instant.now().minus(retentionPolicy);
        
        lock.writeLock().lock();
        try {
            int removedCount = 0;
            
            // Cleanup raw metrics
            for (NavigableMap<Instant, List<PerformanceMetrics>> toolMetrics : rawMetrics.values()) {
                Iterator<Map.Entry<Instant, List<PerformanceMetrics>>> iterator = toolMetrics.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Instant, List<PerformanceMetrics>> entry = iterator.next();
                    if (entry.getKey().isBefore(rawCutoff)) {
                        removedCount += entry.getValue().size();
                        iterator.remove();
                    }
                }
            }
            
            // Cleanup aggregated metrics
            cleanupAggregatedMetrics(minutelyAggregates, aggregateCutoff);
            cleanupAggregatedMetrics(hourlyAggregates, aggregateCutoff);
            cleanupAggregatedMetrics(dailyAggregates, aggregateCutoff);
            
            return removedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets database statistics.
     */
    public DatabaseStats getDatabaseStats() {
        lock.readLock().lock();
        try {
            int totalRawMetrics = rawMetrics.values().stream()
                .mapToInt(toolMetrics -> toolMetrics.values().stream()
                    .mapToInt(List::size)
                    .sum())
                .sum();
            
            int totalAggregates = 
                minutelyAggregates.values().stream().mapToInt(Map::size).sum() +
                hourlyAggregates.values().stream().mapToInt(Map::size).sum() +
                dailyAggregates.values().stream().mapToInt(Map::size).sum();
            
            return new DatabaseStats(
                rawMetrics.size(),
                totalRawMetrics,
                totalAggregates,
                estimateMemoryUsage()
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Private helper methods
    
    private void storeRawMetric(PerformanceMetrics metric) {
        String toolName = metric.toolName();
        Instant bucketTime = truncateToMinute(metric.timestamp());
        
        rawMetrics.computeIfAbsent(toolName, k -> new TreeMap<>())
                  .computeIfAbsent(bucketTime, k -> new ArrayList<>())
                  .add(metric);
    }
    
    private void updateAggregates(PerformanceMetrics metric) {
        String toolName = metric.toolName();
        
        // Update minutely aggregates
        updateAggregateForResolution(metric, toolName, minutelyAggregates, 
                                   truncateToMinute(metric.timestamp()));
        
        // Update hourly aggregates
        updateAggregateForResolution(metric, toolName, hourlyAggregates, 
                                   truncateToHour(metric.timestamp()));
        
        // Update daily aggregates
        updateAggregateForResolution(metric, toolName, dailyAggregates, 
                                   truncateToDay(metric.timestamp()));
    }
    
    private void updateAggregateForResolution(PerformanceMetrics metric, String toolName,
                                            Map<String, NavigableMap<Instant, AggregatedMetrics>> aggregateStore,
                                            Instant bucketTime) {
        NavigableMap<Instant, AggregatedMetrics> toolAggregates = 
            aggregateStore.computeIfAbsent(toolName, k -> new TreeMap<>());
        
        AggregatedMetrics existing = toolAggregates.get(bucketTime);
        AggregatedMetrics updated;
        
        if (existing == null) {
            updated = new AggregatedMetrics(
                bucketTime,
                1,
                metric.success() ? 1 : 0,
                metric.responseTime(),
                metric.responseTime(),
                metric.responseTime().toNanos(),
                metric.responseSize(),
                metric.responseSize(),
                metric.responseSize()
            );
        } else {
            long newCount = existing.count() + 1;
            long newSuccessCount = existing.successCount() + (metric.success() ? 1 : 0);
            
            Duration newMinResponseTime = metric.responseTime().compareTo(existing.minResponseTime()) < 0 ?
                metric.responseTime() : existing.minResponseTime();
            Duration newMaxResponseTime = metric.responseTime().compareTo(existing.maxResponseTime()) > 0 ?
                metric.responseTime() : existing.maxResponseTime();
            
            long newTotalResponseTimeNanos = existing.totalResponseTimeNanos() + metric.responseTime().toNanos();
            
            long newMinResponseSize = Math.min(metric.responseSize(), existing.minResponseSize());
            long newMaxResponseSize = Math.max(metric.responseSize(), existing.maxResponseSize());
            long newTotalResponseSize = existing.totalResponseSize() + metric.responseSize();
            
            updated = new AggregatedMetrics(
                bucketTime,
                newCount,
                newSuccessCount,
                newMinResponseTime,
                newMaxResponseTime,
                newTotalResponseTimeNanos,
                newMinResponseSize,
                newMaxResponseSize,
                newTotalResponseSize
            );
        }
        
        toolAggregates.put(bucketTime, updated);
    }
    
    private Map<String, NavigableMap<Instant, AggregatedMetrics>> getAggregateStore(AggregationResolution resolution) {
        return switch (resolution) {
            case MINUTE -> minutelyAggregates;
            case HOUR -> hourlyAggregates;
            case DAY -> dailyAggregates;
        };
    }
    
    private void cleanupAggregatedMetrics(Map<String, NavigableMap<Instant, AggregatedMetrics>> aggregateStore,
                                        Instant cutoff) {
        for (NavigableMap<Instant, AggregatedMetrics> toolAggregates : aggregateStore.values()) {
            toolAggregates.headMap(cutoff, false).clear();
        }
    }
    
    private Instant truncateToMinute(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.MINUTES);
    }
    
    private Instant truncateToHour(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.HOURS);
    }
    
    private Instant truncateToDay(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.DAYS);
    }
    
    private long estimateMemoryUsage() {
        // Rough estimation of memory usage
        long rawMetricsSize = rawMetrics.values().stream()
            .mapToLong(toolMetrics -> toolMetrics.values().stream()
                .mapToLong(List::size)
                .sum() * 200) // Estimate 200 bytes per raw metric
            .sum();
        
        long aggregatesSize = 
            (minutelyAggregates.values().stream().mapToLong(Map::size).sum() +
             hourlyAggregates.values().stream().mapToLong(Map::size).sum() +
             dailyAggregates.values().stream().mapToLong(Map::size).sum()) * 150; // Estimate 150 bytes per aggregate
        
        return rawMetricsSize + aggregatesSize;
    }
    
    /**
     * Aggregation resolution for metrics queries.
     */
    public enum AggregationResolution {
        MINUTE, HOUR, DAY
    }
    
    /**
     * Aggregated metrics for a time bucket.
     */
    public record AggregatedMetrics(
        Instant timestamp,
        long count,
        long successCount,
        Duration minResponseTime,
        Duration maxResponseTime,
        long totalResponseTimeNanos,
        long minResponseSize,
        long maxResponseSize,
        long totalResponseSize
    ) {
        
        /**
         * Calculates the average response time.
         */
        public Duration avgResponseTime() {
            return count > 0 ? Duration.ofNanos(totalResponseTimeNanos / count) : Duration.ZERO;
        }
        
        /**
         * Calculates the average response size.
         */
        public double avgResponseSize() {
            return count > 0 ? (double) totalResponseSize / count : 0.0;
        }
        
        /**
         * Calculates the success rate.
         */
        public double successRate() {
            return count > 0 ? (double) successCount / count : 0.0;
        }
    }
    
    /**
     * Statistics for a set of metrics.
     */
    public record MetricsStatistics(
        long totalRequests,
        Duration minResponseTime,
        Duration maxResponseTime,
        Duration avgResponseTime,
        Duration p95ResponseTime,
        long successfulRequests,
        long failedRequests,
        double successRate
    ) {}
    
    /**
     * Database statistics.
     */
    public record DatabaseStats(
        int toolCount,
        int totalRawMetrics,
        int totalAggregates,
        long estimatedMemoryUsageBytes
    ) {}
}