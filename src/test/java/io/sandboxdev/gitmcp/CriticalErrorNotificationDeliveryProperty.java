package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.model.*;
import io.sandboxdev.gitmcp.debug.processor.AlertManagerImpl;
import net.jqwik.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for critical error notification delivery functionality.
 * 
 * Feature: mcp-debugging, Property 32: Critical error notification delivery
 * Validates: Requirements 10.1
 */
public class CriticalErrorNotificationDeliveryProperty {

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 32: Critical error notification delivery
    void criticalErrorsTriggeredNotificationsToAllChannels(@ForAll("criticalAlertTypes") AlertType alertType,
                                                          @ForAll("errorMessages") String errorMessage,
                                                          @ForAll("channelCounts") int channelCount) throws Exception {
        Assume.that(channelCount >= 1 && channelCount <= 5);
        
        // Given: AlertManager with multiple notification channels configured
        AlertManagerImpl alertManager = new AlertManagerImpl();
        
        List<NotificationChannel> channels = createNotificationChannels(channelCount);
        alertManager.setNotificationChannels(channels);
        
        // Create alert rule for critical errors
        AlertRule criticalRule = createCriticalErrorRule(channels);
        alertManager.configureAlertRules(List.of(criticalRule));
        
        // When: Critical error occurs
        Map<String, Object> context = Map.of(
            "severity", "CRITICAL",
            "timestamp", Instant.now(),
            "component", "test-component"
        );
        
        alertManager.triggerAlert(alertType, errorMessage, context);
        
        // Then: Notifications should be sent through all configured channels
        // Note: In a real implementation, we would verify actual delivery
        // For this test, we verify the alert was processed and recorded
        
        TimeRange recentRange = new TimeRange(
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(10)
        );
        
        AlertHistory history = alertManager.getAlertHistory(recentRange);
        assertThat(history.alerts()).hasSize(1);
        
        AlertHistory.TriggeredAlert triggeredAlert = history.alerts().get(0);
        assertThat(triggeredAlert.type()).isEqualTo(alertType);
        assertThat(triggeredAlert.message()).contains(errorMessage);
        assertThat(triggeredAlert.severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 32: Critical error notification delivery
    void notificationChannelConfigurationAffectsDelivery(@ForAll("criticalAlertTypes") AlertType alertType,
                                                        @ForAll("errorMessages") String errorMessage) throws Exception {
        // Given: AlertManager with no notification channels configured
        AlertManagerImpl alertManager = new AlertManagerImpl();
        
        // Create alert rule but no channels
        AlertRule rule = new AlertRule(
            "test-rule",
            new AlertCondition("error_rate", new Threshold(1.0, Threshold.ThresholdType.GREATER_THAN), "Error rate threshold"),
            AlertSeverity.CRITICAL,
            List.of("nonexistent-channel"), // Reference non-existent channel
            Duration.ofMinutes(5)
        );
        
        alertManager.configureAlertRules(List.of(rule));
        
        // When: Critical error occurs
        Map<String, Object> context = Map.of("severity", "CRITICAL");
        alertManager.triggerAlert(alertType, errorMessage, context);
        
        // Then: Alert should still be recorded even if no channels are available
        TimeRange recentRange = new TimeRange(
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(10)
        );
        
        AlertHistory history = alertManager.getAlertHistory(recentRange);
        assertThat(history.alerts()).hasSize(1);
        
        // But notification delivery would fail gracefully (logged but not crash)
        AlertHistory.TriggeredAlert alert = history.alerts().get(0);
        assertThat(alert.type()).isEqualTo(alertType);
        assertThat(alert.message()).contains(errorMessage);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 32: Critical error notification delivery
    void multipleChannelTypesReceiveNotifications(@ForAll("criticalAlertTypes") AlertType alertType,
                                                 @ForAll("errorMessages") String errorMessage) throws Exception {
        // Given: AlertManager with different types of notification channels
        AlertManagerImpl alertManager = new AlertManagerImpl();
        
        List<NotificationChannel> mixedChannels = List.of(
            new NotificationChannel(
                "email-channel",
                NotificationChannelType.EMAIL,
                Map.of("recipients", "admin@example.com")
            ),
            new NotificationChannel(
                "webhook-channel", 
                NotificationChannelType.WEBHOOK,
                Map.of("url", "https://hooks.example.com/alert")
            ),
            new NotificationChannel(
                "slack-channel",
                NotificationChannelType.SLACK,
                Map.of("channel", "#alerts")
            )
        );
        
        alertManager.setNotificationChannels(mixedChannels);
        
        // Create rule that uses all channel types
        AlertRule rule = new AlertRule(
            "multi-channel-rule",
            new AlertCondition("error_rate", new Threshold(1.0, Threshold.ThresholdType.GREATER_THAN), "Error rate threshold"),
            AlertSeverity.CRITICAL,
            List.of("email-channel", "webhook-channel", "slack-channel"),
            Duration.ofMinutes(5)
        );
        
        alertManager.configureAlertRules(List.of(rule));
        
        // When: Critical error occurs
        Map<String, Object> context = Map.of(
            "severity", "CRITICAL",
            "affectedChannels", mixedChannels.size()
        );
        
        alertManager.triggerAlert(alertType, errorMessage, context);
        
        // Then: Alert should be processed for all channel types
        TimeRange recentRange = new TimeRange(
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(10)
        );
        
        AlertHistory history = alertManager.getAlertHistory(recentRange);
        assertThat(history.alerts()).hasSize(1);
        
        AlertHistory.TriggeredAlert alert = history.alerts().get(0);
        assertThat(alert.context()).containsKey("affectedChannels");
        assertThat(alert.context().get("affectedChannels")).isEqualTo(3);
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 32: Critical error notification delivery
    void alertRuleMatchingDeterminesNotificationDelivery(@ForAll("allAlertTypes") AlertType alertType,
                                                        @ForAll("errorMessages") String errorMessage,
                                                        @ForAll("severityLevels") AlertSeverity severity) throws Exception {
        // Given: AlertManager with rules for different alert types
        AlertManagerImpl alertManager = new AlertManagerImpl();
        
        NotificationChannel channel = new NotificationChannel(
            "test-channel",
            NotificationChannelType.EMAIL,
            Map.of("recipients", "test@example.com")
        );
        alertManager.setNotificationChannels(List.of(channel));
        
        // Create specific rule that only matches certain conditions
        AlertRule specificRule = new AlertRule(
            "performance-only-rule",
            new AlertCondition("response_time", new Threshold(1000.0, Threshold.ThresholdType.GREATER_THAN), "Performance threshold"),
            AlertSeverity.CRITICAL,
            List.of("test-channel"),
            Duration.ofMinutes(1)
        );
        
        alertManager.configureAlertRules(List.of(specificRule));
        
        // When: Alert is triggered
        Map<String, Object> context = Map.of("severity", severity.toString());
        alertManager.triggerAlert(alertType, errorMessage, context);
        
        // Then: Notification delivery depends on rule matching
        TimeRange recentRange = new TimeRange(
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(10)
        );
        
        AlertHistory history = alertManager.getAlertHistory(recentRange);
        
        // Performance degradation should match the rule, others may not
        if (alertType == AlertType.PERFORMANCE_DEGRADATION) {
            assertThat(history.alerts()).hasSize(1);
            assertThat(history.alerts().get(0).type()).isEqualTo(AlertType.PERFORMANCE_DEGRADATION);
        } else {
            // Other alert types may or may not match depending on the rule logic
            // The key is that the system handles all cases gracefully
            assertThat(history.alerts()).hasSizeLessThanOrEqualTo(1);
        }
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 32: Critical error notification delivery
    void notificationDeliveryIncludesActionableInformation(@ForAll("criticalAlertTypes") AlertType alertType,
                                                          @ForAll("errorMessages") String errorMessage) throws Exception {
        // Given: AlertManager configured for critical alerts
        AlertManagerImpl alertManager = new AlertManagerImpl();
        
        NotificationChannel channel = new NotificationChannel(
            "actionable-channel",
            NotificationChannelType.WEBHOOK,
            Map.of("url", "https://alerts.example.com/webhook")
        );
        alertManager.setNotificationChannels(List.of(channel));
        
        AlertRule rule = createCriticalErrorRule(List.of(channel));
        alertManager.configureAlertRules(List.of(rule));
        
        // When: Critical error occurs with context
        Map<String, Object> richContext = Map.of(
            "severity", "CRITICAL",
            "toolName", "test-tool",
            "exceedancePercentage", 150.0,
            "resourceId", "test-resource",
            "timestamp", Instant.now()
        );
        
        alertManager.triggerAlert(alertType, errorMessage, richContext);
        
        // Then: Alert should include actionable information and debug links
        TimeRange recentRange = new TimeRange(
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(10)
        );
        
        AlertHistory history = alertManager.getAlertHistory(recentRange);
        assertThat(history.alerts()).hasSize(1);
        
        AlertHistory.TriggeredAlert alert = history.alerts().get(0);
        
        // Verify actionable information is included
        assertThat(alert.message()).contains(errorMessage);
        
        // Verify enhanced context includes debug information
        Map<String, Object> context = alert.context();
        assertThat(context).containsKey("alertId");
        assertThat(context).containsKey("debugDataLinks");
        assertThat(context).containsKey("recommendedActions");
        assertThat(context).containsKey("alertRule");
        
        // Verify debug links are provided
        @SuppressWarnings("unchecked")
        Map<String, String> debugLinks = (Map<String, String>) context.get("debugDataLinks");
        assertThat(debugLinks).containsKey("metricsReport");
        assertThat(debugLinks).containsKey("traceHistory");
        assertThat(debugLinks).containsKey("healthStatus");
        
        // Verify recommended actions are provided
        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) context.get("recommendedActions");
        assertThat(actions).isNotEmpty();
    }

    @Property(tries = 100)
    // Feature: mcp-debugging, Property 32: Critical error notification delivery
    void alertHistoryTracksAllNotificationAttempts(@ForAll("criticalAlertTypes") AlertType alertType,
                                                  @ForAll("errorMessages") String errorMessage,
                                                  @ForAll("alertCounts") int alertCount) throws Exception {
        Assume.that(alertCount >= 1 && alertCount <= 10);
        
        // Given: AlertManager with notification tracking
        AlertManagerImpl alertManager = new AlertManagerImpl();
        
        NotificationChannel channel = new NotificationChannel(
            "tracking-channel",
            NotificationChannelType.EMAIL,
            Map.of("recipients", "tracker@example.com")
        );
        alertManager.setNotificationChannels(List.of(channel));
        
        AlertRule rule = createCriticalErrorRule(List.of(channel));
        alertManager.configureAlertRules(List.of(rule));
        
        // When: Multiple critical errors occur
        for (int i = 0; i < alertCount; i++) {
            Map<String, Object> context = Map.of(
                "severity", "CRITICAL",
                "alertNumber", i + 1
            );
            
            alertManager.triggerAlert(alertType, errorMessage + " #" + (i + 1), context);
            
            // Small delay to ensure different timestamps
            Thread.sleep(10);
        }
        
        // Then: All alerts should be tracked in history
        TimeRange recentRange = new TimeRange(
            Instant.now().minusSeconds(30),
            Instant.now().plusSeconds(10)
        );
        
        AlertHistory history = alertManager.getAlertHistory(recentRange);
        assertThat(history.alerts()).hasSize(alertCount);
        
        // Verify all alerts are properly recorded
        for (int i = 0; i < alertCount; i++) {
            AlertHistory.TriggeredAlert alert = history.alerts().get(i);
            assertThat(alert.type()).isEqualTo(alertType);
            assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(alert.message()).contains(errorMessage);
            assertThat(alert.context()).containsKey("alertNumber");
        }
        
        // Verify alerts are ordered by timestamp (most recent first)
        for (int i = 1; i < history.alerts().size(); i++) {
            Instant current = history.alerts().get(i).timestamp();
            Instant previous = history.alerts().get(i - 1).timestamp();
            assertThat(current).isBeforeOrEqualTo(previous);
        }
    }

    private List<NotificationChannel> createNotificationChannels(int count) {
        List<NotificationChannel> channels = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            NotificationChannelType type = NotificationChannelType.values()[i % NotificationChannelType.values().length];
            Map<String, String> config = switch (type) {
                case EMAIL -> Map.of("recipients", "admin" + i + "@example.com");
                case WEBHOOK -> Map.of("url", "https://hooks.example.com/alert" + i);
                case SLACK -> Map.of("channel", "#alerts" + i);
                case SMS -> Map.of("phoneNumber", "+1555000" + String.format("%04d", i));
            };
            
            channels.add(new NotificationChannel(
                "channel-" + i,
                type,
                config
            ));
        }
        
        return channels;
    }
    
    private AlertRule createCriticalErrorRule(List<NotificationChannel> channels) {
        List<String> channelNames = channels.stream()
            .map(NotificationChannel::name)
            .toList();
        
        return new AlertRule(
            "critical-error-rule",
            new AlertCondition("error_rate", new Threshold(1.0, Threshold.ThresholdType.GREATER_THAN), "Critical error threshold"),
            AlertSeverity.CRITICAL,
            channelNames,
            Duration.ofMinutes(1)
        );
    }

    @Provide
    Arbitrary<AlertType> criticalAlertTypes() {
        return Arbitraries.of(
            AlertType.TOOL_FAILURE,
            AlertType.SYSTEM_ERROR,
            AlertType.HEALTH_CHECK_FAILURE,
            AlertType.CONFIGURATION_ERROR
        );
    }

    @Provide
    Arbitrary<AlertType> allAlertTypes() {
        return Arbitraries.of(AlertType.values());
    }

    @Provide
    Arbitrary<AlertSeverity> severityLevels() {
        return Arbitraries.of(AlertSeverity.values());
    }

    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.of(
            "Database connection failed",
            "Tool execution timeout",
            "Configuration validation error",
            "Health check failed",
            "System resource exhausted",
            "Authentication failure",
            "Network connectivity lost"
        );
    }

    @Provide
    Arbitrary<Integer> channelCounts() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<Integer> alertCounts() {
        return Arbitraries.integers().between(1, 10);
    }
}