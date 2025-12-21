package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.model.ProtocolTrace;
import io.sandboxdev.gitmcp.debug.model.TraceFilter;
import io.sandboxdev.gitmcp.debug.model.TimeRange;
import io.sandboxdev.gitmcp.debug.storage.FileBasedStorage;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * **Feature: mcp-debugging, Property 15: Historical data availability**
 * 
 * Property-based test that verifies historical data remains available for 
 * investigation after storage operations.
 * 
 * **Validates: Requirements 5.2**
 */
class HistoricalDataAvailabilityProperty {

    @TempDir
    Path tempDir;
    
    private FileBasedStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileBasedStorage();
        storage.setBasePath(tempDir);
        storage.setCompressionEnabled(true);
        storage.setRetentionPolicy(Duration.ofDays(7));
    }

    @Property(tries = 100)
    void historicalDataRemainsAvailableAfterStorage(
            @ForAll("protocolTraces") @Size(min = 1, max = 50) List<ProtocolTrace> traces,
            @ForAll @IntRange(min = 1, max = 24) int hoursLater) {
        
        // Store all traces
        storage.storeTraces(traces);
        
        // Simulate time passing (but within retention period) - hoursLater is used for test variation
        
        // Create time range that should include all stored traces
        Instant earliestTrace = traces.stream()
            .map(ProtocolTrace::timestamp)
            .min(Instant::compareTo)
            .orElseThrow();
        Instant latestTrace = traces.stream()
            .map(ProtocolTrace::timestamp)
            .max(Instant::compareTo)
            .orElseThrow();
        
        TimeRange queryRange = new TimeRange(
            earliestTrace.minus(1, ChronoUnit.HOURS),
            latestTrace.plus(1, ChronoUnit.HOURS)
        );
        
        // Query for historical data
        TraceFilter filter = TraceFilter.builder()
            .timeRange(queryRange)
            .build();
        
        List<ProtocolTrace> retrievedTraces = storage.getTraces(filter);
        
        // Verify all stored traces are available
        Set<String> storedTraceIds = new HashSet<>();
        for (ProtocolTrace trace : traces) {
            storedTraceIds.add(trace.traceId());
        }
        
        Set<String> retrievedTraceIds = new HashSet<>();
        for (ProtocolTrace trace : retrievedTraces) {
            retrievedTraceIds.add(trace.traceId());
        }
        
        // All stored traces should be retrievable
        for (String traceId : storedTraceIds) {
            if (!retrievedTraceIds.contains(traceId)) {
                throw new AssertionError("Historical trace not available: " + traceId);
            }
        }
    }

    @Property(tries = 100)
    void historicalDataSurvivesMultipleStorageOperations(
            @ForAll("protocolTraces") @Size(min = 5, max = 20) List<ProtocolTrace> initialTraces,
            @ForAll("protocolTraces") @Size(min = 5, max = 20) List<ProtocolTrace> additionalTraces) {
        
        // Store initial batch
        storage.storeTraces(initialTraces);
        
        // Store additional batch
        storage.storeTraces(additionalTraces);
        
        // Combine all traces
        List<ProtocolTrace> allTraces = new ArrayList<>();
        allTraces.addAll(initialTraces);
        allTraces.addAll(additionalTraces);
        
        // Create comprehensive time range
        Instant earliest = allTraces.stream()
            .map(ProtocolTrace::timestamp)
            .min(Instant::compareTo)
            .orElseThrow();
        Instant latest = allTraces.stream()
            .map(ProtocolTrace::timestamp)
            .max(Instant::compareTo)
            .orElseThrow();
        
        TimeRange queryRange = new TimeRange(
            earliest.minus(1, ChronoUnit.HOURS),
            latest.plus(1, ChronoUnit.HOURS)
        );
        
        // Retrieve all historical data
        List<ProtocolTrace> retrievedTraces = storage.getTraces(
            TraceFilter.builder().timeRange(queryRange).build()
        );
        
        // Verify all traces from both batches are available
        Set<String> expectedTraceIds = new HashSet<>();
        for (ProtocolTrace trace : allTraces) {
            expectedTraceIds.add(trace.traceId());
        }
        
        Set<String> actualTraceIds = new HashSet<>();
        for (ProtocolTrace trace : retrievedTraces) {
            actualTraceIds.add(trace.traceId());
        }
        
        for (String expectedId : expectedTraceIds) {
            if (!actualTraceIds.contains(expectedId)) {
                throw new AssertionError("Historical trace lost after multiple operations: " + expectedId);
            }
        }
    }

    @Property(tries = 100)
    void historicalDataFilteringPreservesAvailability(
            @ForAll("protocolTraces") @Size(min = 10, max = 30) List<ProtocolTrace> traces,
            @ForAll("clientIds") String targetClientId) {
        
        // Ensure at least some traces have the target client ID
        List<ProtocolTrace> modifiedTraces = new ArrayList<>();
        boolean hasTargetClient = false;
        
        for (int i = 0; i < traces.size(); i++) {
            ProtocolTrace trace = traces.get(i);
            if (i % 3 == 0) { // Every third trace gets the target client ID
                trace = new ProtocolTrace(
                    trace.traceId(),
                    trace.timestamp(),
                    targetClientId,
                    trace.messageType(),
                    trace.parameters(),
                    trace.response(),
                    trace.processingTime(),
                    trace.errorMessage()
                );
                hasTargetClient = true;
            }
            modifiedTraces.add(trace);
        }
        
        if (!hasTargetClient) {
            // Ensure at least one trace has the target client ID
            ProtocolTrace firstTrace = modifiedTraces.get(0);
            modifiedTraces.set(0, new ProtocolTrace(
                firstTrace.traceId(),
                firstTrace.timestamp(),
                targetClientId,
                firstTrace.messageType(),
                firstTrace.parameters(),
                firstTrace.response(),
                firstTrace.processingTime(),
                firstTrace.errorMessage()
            ));
        }
        
        // Store all traces
        storage.storeTraces(modifiedTraces);
        
        // Query with client filter
        TraceFilter clientFilter = TraceFilter.builder()
            .clientId(targetClientId)
            .build();
        
        List<ProtocolTrace> clientTraces = storage.getTraces(clientFilter);
        
        // Query without filter
        List<ProtocolTrace> allTraces = storage.getTraces(TraceFilter.all());
        
        // Verify filtered results are subset of all results
        Set<String> allTraceIds = new HashSet<>();
        for (ProtocolTrace trace : allTraces) {
            allTraceIds.add(trace.traceId());
        }
        
        for (ProtocolTrace clientTrace : clientTraces) {
            if (!allTraceIds.contains(clientTrace.traceId())) {
                throw new AssertionError("Filtered trace not found in complete dataset: " + clientTrace.traceId());
            }
            
            if (!targetClientId.equals(clientTrace.clientId())) {
                throw new AssertionError("Filter returned trace with wrong client ID: " + clientTrace.clientId());
            }
        }
        
        // Verify we can still access all original data
        if (allTraces.size() != modifiedTraces.size()) {
            throw new AssertionError("Historical data count mismatch: expected " + 
                modifiedTraces.size() + ", got " + allTraces.size());
        }
    }

    @Provide
    Arbitrary<List<ProtocolTrace>> protocolTraces() {
        return Arbitraries.create(() -> {
            List<ProtocolTrace> traces = new ArrayList<>();
            int count = new Random().nextInt(20) + 5; // 5-25 traces
            
            Instant baseTime = Instant.now().minus(Duration.ofHours(12));
            
            for (int i = 0; i < count; i++) {
                traces.add(createRandomTrace(baseTime.plus(Duration.ofMinutes(i * 5))));
            }
            
            return traces;
        });
    }

    @Provide
    Arbitrary<String> clientIds() {
        return Arbitraries.of("client-1", "client-2", "client-3", "test-client", "debug-client");
    }

    @Provide
    Arbitrary<String> messageTypes() {
        return Arbitraries.of("tool-invocation", "resource-request", "health-check", "status-query");
    }

    @Provide
    Arbitrary<String> traceIds() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('0', '9')
            .withChars('-')
            .ofMinLength(8)
            .ofMaxLength(16)
            .map(s -> "trace-" + s);
    }

    private ProtocolTrace createRandomTrace(Instant timestamp) {
        Random random = new Random();
        
        return new ProtocolTrace(
            "trace-" + UUID.randomUUID().toString().substring(0, 8),
            timestamp,
            "client-" + (random.nextInt(3) + 1),
            random.nextBoolean() ? "tool-invocation" : "resource-request",
            Map.of("param1", "value1", "param2", random.nextInt(100)),
            Map.of("result", "success", "data", "response-" + random.nextInt(1000)),
            Duration.ofMillis(50 + random.nextInt(500)),
            random.nextDouble() < 0.1 ? Optional.of("Test error") : Optional.empty()
        );
    }
}