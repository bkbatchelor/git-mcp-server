package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.model.Threshold;
import io.sandboxdev.gitmcp.debug.processor.MetricsAggregatorImpl;
import net.jqwik.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Property-based test for alert threshold enforcement functionality.
 * 
 * Feature: mcp-debugging, Property 5: Alert threshold enforcement
 * Validates: Requirements 2.5
 */
public class AlertThresholdEnforcementProperty {

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 5: Alert threshold enforcement
    void alertsTriggeredWhenThresholdsExceeded(@ForAll("toolNames") String toolName,
                                              @ForAll("thresholdValues") long thresholdMs,
                                              @ForAll("exceedingResponseTimes") long responseTimeMs) throws Exception {
        Assume.that(responseTimeMs > thresholdMs);
        
        // Given: MetricsAggregator with threshold configured
        MetricsAggregatorImpl aggregator = new MetricsAggregatorImpl();
        
        Threshold threshold = new Threshold(thresholdMs, Threshold.ThresholdType.GREATER_THAN);
        aggregator.setAlertThresholds(Map.of(toolName, threshold));
        
        // Set up alert listener to capture alerts
        List<MetricsAggregatorImpl.AlertEvent> capturedAlerts = new ArrayList<>();
        Consumer<MetricsAggregatorImpl.AlertEvent> alertListener = capturedAlerts::add;
        aggregator.addAlertListener(alertListener);
        
        // When: Record tool invocation that exceeds threshold
        Duration responseTime = Duration.ofMillis(responseTimeMs);
        aggregator.recordToolInvocation(toolName, responseTime);
        
        // Then: Alert should be triggered with contextual information
        assertThat(capturedAlerts).hasSize(1);
        
        MetricsAggregatorImpl.AlertEvent alert = capturedAlerts.get(0);
        assertThat(alert.type()).isEqualTo(io.sandboxdev.gitmcp.debug.model.AlertType.PERFORMANCE_DEGRADATION);
        assertThat(alert.message()).contains(toolName);
        assertThat(alert.message()).contains(String.valueOf(responseTimeMs));
        assertThat(alert.message()).contains(String.valueOf(thresholdMs));
        
        // Verify contextual information is included
        Map<String, Object> context = alert.context();
        assertThat(context).containsKey("toolName");
        assertThat(context).containsKey("actualResponseTime");
        assertThat(context).containsKey("thresholdResponseTime");
        assertThat(context).containsKey("timestamp");
        assertThat(context).containsKey("exceedancePercentage");
        
        assertThat(context.get("toolName")).isEqualTo(toolName);
        assertThat(context.get("actualResponseTime")).isEqualTo(responseTimeMs);
        assertThat(context.get("thresholdResponseTime")).isEqualTo(thresholdMs);
        
        // Verify exceedance percentage is calculated correctly
        double expectedPercentage = ((double) responseTimeMs / thresholdMs - 1.0) * 100;
        assertThat((Double) context.get("exceedancePercentage")).isCloseTo(expectedPercentage, within(0.01));
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 5: Alert threshold enforcement
    void noAlertsWhenThresholdsNotExceeded(@ForAll("toolNames") String toolName,
                                          @ForAll("thresholdValues") long thresholdMs,
                                          @ForAll("nonExceedingResponseTimes") long responseTimeMs) throws Exception {
        Assume.that(responseTimeMs <= thresholdMs);
        
        // Given: MetricsAggregator with threshold configured
        MetricsAggregatorImpl aggregator = new MetricsAggregatorImpl();
        
        Threshold threshold = new Threshold(thresholdMs, Threshold.ThresholdType.GREATER_THAN);
        aggregator.setAlertThresholds(Map.of(toolName, threshold));
        
        // Set up alert listener to capture alerts
        List<MetricsAggregatorImpl.AlertEvent> capturedAlerts = new ArrayList<>();
        Consumer<MetricsAggregatorImpl.AlertEvent> alertListener = capturedAlerts::add;
        aggregator.addAlertListener(alertListener);
        
        // When: Record tool invocation that does not exceed threshold
        Duration responseTime = Duration.ofMillis(responseTimeMs);
        aggregator.recordToolInvocation(toolName, responseTime);
        
        // Then: No alerts should be triggered
        assertThat(capturedAlerts).isEmpty();
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 5: Alert threshold enforcement
    void alertsOnlyTriggeredForConfiguredTools(@ForAll("toolNames") String configuredTool,
                                              @ForAll("toolNames") String unconfiguredTool,
                                              @ForAll("thresholdValues") long thresholdMs,
                                              @ForAll("exceedingResponseTimes") long responseTimeMs) throws Exception {
        Assume.that(!configuredTool.equals(unconfiguredTool));
        Assume.that(responseTimeMs > thresholdMs);
        
        // Given: MetricsAggregator with threshold configured for only one tool
        MetricsAggregatorImpl aggregator = new MetricsAggregatorImpl();
        
        Threshold threshold = new Threshold(thresholdMs, Threshold.ThresholdType.GREATER_THAN);
        aggregator.setAlertThresholds(Map.of(configuredTool, threshold));
        
        // Set up alert listener to capture alerts
        List<MetricsAggregatorImpl.AlertEvent> capturedAlerts = new ArrayList<>();
        Consumer<MetricsAggregatorImpl.AlertEvent> alertListener = capturedAlerts::add;
        aggregator.addAlertListener(alertListener);
        
        Duration responseTime = Duration.ofMillis(responseTimeMs);
        
        // When: Record invocations for both configured and unconfigured tools
        aggregator.recordToolInvocation(configuredTool, responseTime);
        aggregator.recordToolInvocation(unconfiguredTool, responseTime);
        
        // Then: Alert should only be triggered for the configured tool
        assertThat(capturedAlerts).hasSize(1);
        
        MetricsAggregatorImpl.AlertEvent alert = capturedAlerts.get(0);
        assertThat(alert.context().get("toolName")).isEqualTo(configuredTool);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 5: Alert threshold enforcement
    void multipleAlertsForRepeatedThresholdViolations(@ForAll("toolNames") String toolName,
                                                     @ForAll("thresholdValues") long thresholdMs,
                                                     @ForAll("violationCounts") int violationCount) throws Exception {
        Assume.that(violationCount >= 1 && violationCount <= 10);
        
        // Given: MetricsAggregator with threshold configured
        MetricsAggregatorImpl aggregator = new MetricsAggregatorImpl();
        
        Threshold threshold = new Threshold(thresholdMs, Threshold.ThresholdType.GREATER_THAN);
        aggregator.setAlertThresholds(Map.of(toolName, threshold));
        
        // Set up alert listener to capture alerts
        List<MetricsAggregatorImpl.AlertEvent> capturedAlerts = new ArrayList<>();
        Consumer<MetricsAggregatorImpl.AlertEvent> alertListener = capturedAlerts::add;
        aggregator.addAlertListener(alertListener);
        
        // When: Record multiple tool invocations that exceed threshold
        Duration exceedingResponseTime = Duration.ofMillis(thresholdMs + 100);
        for (int i = 0; i < violationCount; i++) {
            aggregator.recordToolInvocation(toolName, exceedingResponseTime);
        }
        
        // Then: Each violation should trigger an alert
        assertThat(capturedAlerts).hasSize(violationCount);
        
        // Verify all alerts are for the same tool and have correct context
        for (MetricsAggregatorImpl.AlertEvent alert : capturedAlerts) {
            assertThat(alert.type()).isEqualTo(io.sandboxdev.gitmcp.debug.model.AlertType.PERFORMANCE_DEGRADATION);
            assertThat(alert.context().get("toolName")).isEqualTo(toolName);
            assertThat(alert.context().get("actualResponseTime")).isEqualTo(thresholdMs + 100);
            assertThat(alert.context().get("thresholdResponseTime")).isEqualTo(thresholdMs);
        }
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 5: Alert threshold enforcement
    void resourceSizeThresholdEnforcement(@ForAll("resourceIds") String resourceId,
                                         @ForAll("thresholdValues") long thresholdBytes,
                                         @ForAll("exceedingSizes") long responseSize) throws Exception {
        Assume.that(responseSize > thresholdBytes);
        
        // Given: MetricsAggregator with resource size threshold configured
        MetricsAggregatorImpl aggregator = new MetricsAggregatorImpl();
        
        String resourceThresholdKey = "resource_" + resourceId;
        Threshold threshold = new Threshold(thresholdBytes, Threshold.ThresholdType.GREATER_THAN);
        aggregator.setAlertThresholds(Map.of(resourceThresholdKey, threshold));
        
        // Set up alert listener to capture alerts
        List<MetricsAggregatorImpl.AlertEvent> capturedAlerts = new ArrayList<>();
        Consumer<MetricsAggregatorImpl.AlertEvent> alertListener = capturedAlerts::add;
        aggregator.addAlertListener(alertListener);
        
        // When: Record resource access that exceeds threshold
        aggregator.recordResourceAccess(resourceId, responseSize);
        
        // Then: Alert should be triggered for resource size violation
        assertThat(capturedAlerts).hasSize(1);
        
        MetricsAggregatorImpl.AlertEvent alert = capturedAlerts.get(0);
        assertThat(alert.type()).isEqualTo(io.sandboxdev.gitmcp.debug.model.AlertType.RESOURCE_SIZE_EXCEEDED);
        assertThat(alert.message()).contains(resourceId);
        assertThat(alert.message()).contains(String.valueOf(responseSize));
        assertThat(alert.message()).contains(String.valueOf(thresholdBytes));
        
        // Verify contextual information
        Map<String, Object> context = alert.context();
        assertThat(context.get("resourceId")).isEqualTo(resourceId);
        assertThat(context.get("actualSize")).isEqualTo(responseSize);
        assertThat(context.get("thresholdSize")).isEqualTo(thresholdBytes);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 5: Alert threshold enforcement
    void alertListenerManagement(@ForAll("toolNames") String toolName,
                                @ForAll("thresholdValues") long thresholdMs) throws Exception {
        // Given: MetricsAggregator with threshold configured
        MetricsAggregatorImpl aggregator = new MetricsAggregatorImpl();
        
        Threshold threshold = new Threshold(thresholdMs, Threshold.ThresholdType.GREATER_THAN);
        aggregator.setAlertThresholds(Map.of(toolName, threshold));
        
        // Set up multiple alert listeners
        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        
        Consumer<MetricsAggregatorImpl.AlertEvent> listener1 = alert -> listener1Count.incrementAndGet();
        Consumer<MetricsAggregatorImpl.AlertEvent> listener2 = alert -> listener2Count.incrementAndGet();
        
        aggregator.addAlertListener(listener1);
        aggregator.addAlertListener(listener2);
        
        // When: Record tool invocation that exceeds threshold
        Duration exceedingResponseTime = Duration.ofMillis(thresholdMs + 100);
        aggregator.recordToolInvocation(toolName, exceedingResponseTime);
        
        // Then: Both listeners should be notified
        assertThat(listener1Count.get()).isEqualTo(1);
        assertThat(listener2Count.get()).isEqualTo(1);
        
        // When: Remove one listener and trigger another alert
        aggregator.removeAlertListener(listener1);
        aggregator.recordToolInvocation(toolName, exceedingResponseTime);
        
        // Then: Only remaining listener should be notified
        assertThat(listener1Count.get()).isEqualTo(1); // No change
        assertThat(listener2Count.get()).isEqualTo(2); // Incremented
    }

    @Provide
    Arbitrary<String> toolNames() {
        return Arbitraries.of(
            "getStatus",
            "getCurrentBranch", 
            "listBranches",
            "getHistory",
            "createCommit",
            "stageFiles"
        );
    }

    @Provide
    Arbitrary<String> resourceIds() {
        return Arbitraries.of(
            "repository-status",
            "commit-history",
            "branch-list",
            "file-content",
            "diff-output"
        );
    }

    @Provide
    Arbitrary<Long> thresholdValues() {
        return Arbitraries.longs().between(100L, 5000L);
    }

    @Provide
    Arbitrary<Long> exceedingResponseTimes() {
        return Arbitraries.longs().between(5001L, 10000L);
    }

    @Provide
    Arbitrary<Long> nonExceedingResponseTimes() {
        return Arbitraries.longs().between(1L, 5000L);
    }

    @Provide
    Arbitrary<Long> exceedingSizes() {
        return Arbitraries.longs().between(10001L, 50000L);
    }

    @Provide
    Arbitrary<Integer> violationCounts() {
        return Arbitraries.integers().between(1, 10);
    }
}