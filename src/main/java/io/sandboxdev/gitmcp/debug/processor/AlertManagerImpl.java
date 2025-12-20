package io.sandboxdev.gitmcp.debug.processor;

import io.sandboxdev.gitmcp.debug.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Implementation of AlertManager for managing alerts and notifications.
 * Supports configurable alert rules, notification channels, and retry mechanisms.
 */
@Component
@ConditionalOnProperty(name = "mcp-debug.enabled", havingValue = "true", matchIfMissing = false)
public class AlertManagerImpl implements AlertManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertManagerImpl.class);
    
    private final List<AlertRule> alertRules = new CopyOnWriteArrayList<>();
    private final List<NotificationChannel> notificationChannels = new CopyOnWriteArrayList<>();
    private final List<AlertHistoryEntry> alertHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Instant> lastAlertTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(2);
    private final BlockingQueue<PendingNotification> notificationQueue = new LinkedBlockingQueue<>();
    
    public AlertManagerImpl() {
        // Start notification processing thread
        startNotificationProcessor();
    }
    
    @Override
    public void configureAlertRules(List<AlertRule> rules) {
        alertRules.clear();
        alertRules.addAll(rules);
        logger.info("Configured {} alert rules with filtering by error type, severity, and frequency (Requirement 10.3)", 
            rules.size());
        
        for (AlertRule rule : rules) {
            logger.debug("Alert rule '{}': {} severity, cooldown: {}", 
                rule.name(), rule.severity(), rule.cooldownPeriod());
        }
    }
    
    @Override
    public void triggerAlert(AlertType type, String message, Map<String, Object> context) {
        logger.debug("Processing alert trigger: {} - {}", type, message);
        
        // Find matching alert rules (Requirement 10.3: filtering by error type, severity, frequency)
        List<AlertRule> matchingRules = alertRules.stream()
            .filter(rule -> matchesAlertRule(rule, type, context))
            .filter(rule -> !isInCooldownPeriod(rule))
            .collect(Collectors.toList());
        
        if (matchingRules.isEmpty()) {
            logger.debug("No matching alert rules found for alert type: {}", type);
            return;
        }
        
        for (AlertRule rule : matchingRules) {
            // Create alert with actionable information (Requirement 10.4)
            Alert alert = createActionableAlert(type, message, context, rule);
            
            // Record in alert history
            AlertHistoryEntry historyEntry = new AlertHistoryEntry(
                alert.id(),
                alert.timestamp(),
                rule.name(),
                alert.type(),
                alert.severity(),
                alert.message(),
                alert.context()
            );
            alertHistory.add(historyEntry);
            
            // Update cooldown tracking
            lastAlertTimes.put(rule.name(), alert.timestamp());
            
            // Send notifications through configured channels (Requirement 10.1)
            sendNotifications(alert, rule.notificationChannels());
            
            logger.info("🚨 ALERT TRIGGERED: {} [{}] - {}", 
                rule.name(), alert.severity(), alert.message());
        }
    }
    
    @Override
    public void setNotificationChannels(List<NotificationChannel> channels) {
        notificationChannels.clear();
        notificationChannels.addAll(channels);
        logger.info("Configured {} notification channels: {} (Requirement 10.1)", 
            channels.size(), 
            channels.stream().map(ch -> ch.type().toString()).collect(Collectors.toList()));
    }
    
    @Override
    public AlertHistory getAlertHistory(TimeRange timeRange) {
        List<AlertHistoryEntry> filteredEntries = alertHistory.stream()
            .filter(entry -> isWithinTimeRange(entry.timestamp(), timeRange))
            .sorted(Comparator.comparing(AlertHistoryEntry::timestamp).reversed())
            .collect(Collectors.toList());
        
        List<AlertHistory.TriggeredAlert> triggeredAlerts = filteredEntries.stream()
            .map(entry -> new AlertHistory.TriggeredAlert(
                entry.ruleName(),
                entry.type(),
                entry.severity(),
                entry.message(),
                entry.timestamp(),
                entry.context()
            ))
            .collect(Collectors.toList());
        
        return new AlertHistory(timeRange, triggeredAlerts);
    }
    
    /**
     * Get recent alerts for a specific alert type.
     */
    public List<AlertHistoryEntry> getRecentAlerts(AlertType type, Duration lookback) {
        Instant cutoff = Instant.now().minus(lookback);
        return alertHistory.stream()
            .filter(entry -> entry.type() == type)
            .filter(entry -> entry.timestamp().isAfter(cutoff))
            .sorted(Comparator.comparing(AlertHistoryEntry::timestamp).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get alert statistics for monitoring dashboard.
     */
    public Map<AlertType, Long> getAlertStatistics(TimeRange timeRange) {
        return alertHistory.stream()
            .filter(entry -> isWithinTimeRange(entry.timestamp(), timeRange))
            .collect(Collectors.groupingBy(
                AlertHistoryEntry::type,
                Collectors.counting()
            ));
    }
    
    private boolean matchesAlertRule(AlertRule rule, AlertType type, Map<String, Object> context) {
        // Check if alert condition matches the current alert
        AlertCondition condition = rule.condition();
        
        // Match based on metric name and alert type - in a full implementation this would be more sophisticated
        String metricName = condition.metricName();
        
        switch (type) {
            case PERFORMANCE_DEGRADATION:
                return metricName.contains("response_time") || metricName.contains("performance");
            case RESOURCE_SIZE_EXCEEDED:
                return metricName.contains("resource_size") || metricName.contains("size");
            case TOOL_FAILURE:
            case SYSTEM_ERROR:
                return metricName.contains("error") || metricName.contains("failure");
            case HEALTH_CHECK_FAILURE:
                return metricName.contains("health") || metricName.contains("availability");
            case CONFIGURATION_ERROR:
                return metricName.contains("config") || metricName.contains("configuration");
            default:
                return false;
        }
    }
    
    private boolean isInCooldownPeriod(AlertRule rule) {
        Instant lastAlert = lastAlertTimes.get(rule.name());
        if (lastAlert == null) {
            return false;
        }
        
        Instant cooldownEnd = lastAlert.plus(rule.cooldownPeriod());
        boolean inCooldown = Instant.now().isBefore(cooldownEnd);
        
        if (inCooldown) {
            logger.debug("Alert rule '{}' is in cooldown period until {}", rule.name(), cooldownEnd);
        }
        
        return inCooldown;
    }
    
    private Alert createActionableAlert(AlertType type, String message, Map<String, Object> context, AlertRule rule) {
        String alertId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        
        // Enhance message with actionable information (Requirement 10.4)
        String actionableMessage = enhanceMessageWithActionableInfo(message, context, type);
        
        // Add links to relevant debugging data (Requirement 10.4)
        Map<String, Object> enhancedContext = new HashMap<>(context);
        enhancedContext.put("alertId", alertId);
        enhancedContext.put("debugDataLinks", generateDebugDataLinks(type, context));
        enhancedContext.put("recommendedActions", getRecommendedActions(type, context));
        enhancedContext.put("alertRule", rule.name());
        
        return new Alert(
            alertId,
            timestamp,
            type,
            rule.severity(),
            actionableMessage,
            enhancedContext
        );
    }
    
    private String enhanceMessageWithActionableInfo(String message, Map<String, Object> context, AlertType type) {
        StringBuilder enhanced = new StringBuilder(message);
        
        // Add specific actionable information based on alert type
        switch (type) {
            case PERFORMANCE_DEGRADATION:
                if (context.containsKey("toolName") && context.containsKey("exceedancePercentage")) {
                    enhanced.append(String.format(" | Action: Review tool '%s' implementation - performance degraded by %.1f%%", 
                        context.get("toolName"), context.get("exceedancePercentage")));
                }
                break;
            case RESOURCE_SIZE_EXCEEDED:
                if (context.containsKey("resourceId")) {
                    enhanced.append(String.format(" | Action: Optimize resource '%s' or increase size limits", 
                        context.get("resourceId")));
                }
                break;
            case TOOL_FAILURE:
                enhanced.append(" | Action: Check tool implementation and error logs");
                break;
            case HEALTH_CHECK_FAILURE:
                enhanced.append(" | Action: Verify system components and dependencies");
                break;
            case SYSTEM_ERROR:
                enhanced.append(" | Action: Check system logs and investigate root cause");
                break;
            case CONFIGURATION_ERROR:
                enhanced.append(" | Action: Review and validate system configuration");
                break;
        }
        
        return enhanced.toString();
    }
    
    private Map<String, String> generateDebugDataLinks(AlertType type, Map<String, Object> context) {
        Map<String, String> links = new HashMap<>();
        
        // Generate links to relevant debugging data (Requirement 10.4)
        links.put("metricsReport", "/debug/metrics/report");
        links.put("traceHistory", "/debug/traces/recent");
        links.put("healthStatus", "/debug/health/status");
        
        if (context.containsKey("toolName")) {
            links.put("toolMetrics", "/debug/metrics/tool/" + context.get("toolName"));
        }
        
        if (context.containsKey("timestamp")) {
            links.put("timeRangeAnalysis", "/debug/analysis/timerange?around=" + context.get("timestamp"));
        }
        
        return links;
    }
    
    private List<String> getRecommendedActions(AlertType type, Map<String, Object> context) {
        List<String> actions = new ArrayList<>();
        
        switch (type) {
            case PERFORMANCE_DEGRADATION:
                actions.add("Review recent code changes to the affected tool");
                actions.add("Check system resource utilization");
                actions.add("Analyze performance metrics trends");
                break;
            case RESOURCE_SIZE_EXCEEDED:
                actions.add("Review resource content and optimize if possible");
                actions.add("Consider implementing pagination or chunking");
                actions.add("Increase resource size limits if appropriate");
                break;
            case TOOL_FAILURE:
                actions.add("Check application logs for detailed error information");
                actions.add("Verify tool dependencies and configuration");
                actions.add("Test tool functionality manually");
                break;
            case HEALTH_CHECK_FAILURE:
                actions.add("Verify all system dependencies are available");
                actions.add("Check network connectivity and permissions");
                actions.add("Review system configuration");
                break;
            case SYSTEM_ERROR:
                actions.add("Check system logs for error details");
                actions.add("Verify system resources and dependencies");
                actions.add("Consider restarting affected components");
                break;
            case CONFIGURATION_ERROR:
                actions.add("Review configuration files for syntax errors");
                actions.add("Validate configuration against schema");
                actions.add("Check file permissions and accessibility");
                break;
        }
        
        return actions;
    }
    
    private void sendNotifications(Alert alert, List<String> channelNames) {
        for (String channelName : channelNames) {
            NotificationChannel channel = findNotificationChannel(channelName);
            if (channel != null) {
                PendingNotification notification = new PendingNotification(
                    alert,
                    channel,
                    0, // retry count
                    Instant.now()
                );
                
                // Queue notification for processing (Requirement 10.5: queuing and retry)
                notificationQueue.offer(notification);
                logger.debug("Queued notification for channel: {}", channelName);
            } else {
                logger.warn("Notification channel '{}' not found", channelName);
            }
        }
    }
    
    private NotificationChannel findNotificationChannel(String channelName) {
        return notificationChannels.stream()
            .filter(channel -> channelName.equals(channel.name()))
            .findFirst()
            .orElse(null);
    }
    
    private void startNotificationProcessor() {
        // Background thread to process notification queue
        Thread processor = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PendingNotification notification = notificationQueue.take();
                    processNotification(notification);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing notification: {}", e.getMessage(), e);
                }
            }
        });
        processor.setDaemon(true);
        processor.setName("AlertManager-NotificationProcessor");
        processor.start();
        
        logger.info("Started notification processor for alert delivery");
    }
    
    private void processNotification(PendingNotification notification) {
        try {
            boolean success = deliverNotification(notification.alert(), notification.channel());
            
            if (success) {
                logger.info("✅ Notification delivered successfully via {}: {}", 
                    notification.channel().type(), notification.alert().message());
            } else {
                // Retry with exponential backoff (Requirement 10.5)
                scheduleRetry(notification);
            }
        } catch (Exception e) {
            logger.error("Failed to deliver notification via {}: {}", 
                notification.channel().type(), e.getMessage());
            scheduleRetry(notification);
        }
    }
    
    private boolean deliverNotification(Alert alert, NotificationChannel channel) {
        // Simulate notification delivery - in a real implementation this would
        // integrate with actual email/webhook services
        switch (channel.type()) {
            case EMAIL:
                return deliverEmailNotification(alert, channel);
            case WEBHOOK:
                return deliverWebhookNotification(alert, channel);
            case SLACK:
                return deliverSlackNotification(alert, channel);
            default:
                logger.warn("Unsupported notification channel type: {}", channel.type());
                return false;
        }
    }
    
    private boolean deliverEmailNotification(Alert alert, NotificationChannel channel) {
        // Simulate email delivery
        logger.info("📧 Sending email notification to {}: {}", 
            channel.configuration().get("recipients"), alert.message());
        
        // In a real implementation, this would use JavaMail or similar
        // For now, simulate success/failure
        return Math.random() > 0.1; // 90% success rate
    }
    
    private boolean deliverWebhookNotification(Alert alert, NotificationChannel channel) {
        // Simulate webhook delivery
        logger.info("🔗 Sending webhook notification to {}: {}", 
            channel.configuration().get("url"), alert.message());
        
        // In a real implementation, this would use HTTP client
        // For now, simulate success/failure
        return Math.random() > 0.1; // 90% success rate
    }
    
    private boolean deliverSlackNotification(Alert alert, NotificationChannel channel) {
        // Simulate Slack delivery
        logger.info("💬 Sending Slack notification to {}: {}", 
            channel.configuration().get("channel"), alert.message());
        
        // In a real implementation, this would use Slack API
        // For now, simulate success/failure
        return Math.random() > 0.1; // 90% success rate
    }
    
    private void scheduleRetry(PendingNotification notification) {
        int retryCount = notification.retryCount();
        
        // Maximum 5 retries with exponential backoff (Requirement 10.5)
        if (retryCount >= 5) {
            logger.error("❌ Maximum retries exceeded for notification via {}: {}", 
                notification.channel().type(), notification.alert().message());
            return;
        }
        
        // Exponential backoff: 2^retryCount seconds
        long delaySeconds = (long) Math.pow(2, retryCount);
        
        PendingNotification retryNotification = new PendingNotification(
            notification.alert(),
            notification.channel(),
            retryCount + 1,
            notification.queuedAt()
        );
        
        retryExecutor.schedule(() -> {
            logger.info("🔄 Retrying notification delivery (attempt {}) via {}", 
                retryCount + 1, notification.channel().type());
            notificationQueue.offer(retryNotification);
        }, delaySeconds, TimeUnit.SECONDS);
        
        logger.debug("Scheduled retry #{} for notification in {} seconds", retryCount + 1, delaySeconds);
    }
    
    private boolean isWithinTimeRange(Instant timestamp, TimeRange timeRange) {
        return !timestamp.isBefore(timeRange.start()) && !timestamp.isAfter(timeRange.end());
    }
    
    /**
     * Pending notification for retry processing.
     */
    private record PendingNotification(
        Alert alert,
        NotificationChannel channel,
        int retryCount,
        Instant queuedAt
    ) {}
    
    /**
     * Alert record with enhanced context.
     */
    public record Alert(
        String id,
        Instant timestamp,
        AlertType type,
        AlertSeverity severity,
        String message,
        Map<String, Object> context
    ) {}
    
    /**
     * Alert history entry.
     */
    public record AlertHistoryEntry(
        String alertId,
        Instant timestamp,
        String ruleName,
        AlertType type,
        AlertSeverity severity,
        String message,
        Map<String, Object> context
    ) {}
}