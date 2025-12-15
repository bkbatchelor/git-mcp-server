package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.AlertHistory;
import io.sandboxdev.gitmcp.debug.model.AlertRule;
import io.sandboxdev.gitmcp.debug.model.AlertType;
import io.sandboxdev.gitmcp.debug.model.NotificationChannel;
import io.sandboxdev.gitmcp.debug.model.TimeRange;

import java.util.List;
import java.util.Map;

/**
 * Interface for managing alerts and notifications in the debug system.
 * Handles alert rule configuration, triggering, and notification delivery.
 */
public interface AlertManager {
    
    /**
     * Configure alert rules for the system.
     * 
     * @param rules list of alert rules to configure
     */
    void configureAlertRules(List<AlertRule> rules);
    
    /**
     * Trigger an alert with the specified type and context.
     * 
     * @param type the type of alert to trigger
     * @param message the alert message
     * @param context additional context information for the alert
     */
    void triggerAlert(AlertType type, String message, Map<String, Object> context);
    
    /**
     * Set the notification channels for alert delivery.
     * 
     * @param channels list of notification channels to use
     */
    void setNotificationChannels(List<NotificationChannel> channels);
    
    /**
     * Get the alert history for the specified time range.
     * 
     * @param timeRange the time range for alert history
     * @return alert history containing triggered alerts
     */
    AlertHistory getAlertHistory(TimeRange timeRange);
}