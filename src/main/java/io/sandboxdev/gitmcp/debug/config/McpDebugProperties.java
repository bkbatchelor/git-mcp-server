package io.sandboxdev.gitmcp.debug.config;

import io.sandboxdev.gitmcp.debug.model.DebugLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the MCP Debug System.
 */
@ConfigurationProperties(prefix = "mcp-debug")
public class McpDebugProperties {
    
    /**
     * Whether the debug system is enabled.
     */
    private boolean enabled = false;
    
    /**
     * Global debug level.
     */
    private DebugLevel globalLevel = DebugLevel.INFO;
    
    /**
     * Component-specific debug levels.
     */
    private Map<String, DebugLevel> componentLevels = new HashMap<>();
    
    /**
     * Protocol tracing configuration.
     */
    private ProtocolTracing protocolTracing = new ProtocolTracing();
    
    /**
     * Performance monitoring configuration.
     */
    private PerformanceMonitoring performanceMonitoring = new PerformanceMonitoring();
    
    /**
     * Health check configuration.
     */
    private HealthChecks healthChecks = new HealthChecks();
    
    /**
     * Storage configuration.
     */
    private Storage storage = new Storage();
    
    /**
     * Notification configuration.
     */
    private Notifications notifications = new Notifications();
    
    /**
     * Export configuration.
     */
    private Export export = new Export();
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public DebugLevel getGlobalLevel() { return globalLevel; }
    public void setGlobalLevel(DebugLevel globalLevel) { this.globalLevel = globalLevel; }
    
    public Map<String, DebugLevel> getComponentLevels() { return componentLevels; }
    public void setComponentLevels(Map<String, DebugLevel> componentLevels) { this.componentLevels = componentLevels; }
    
    public ProtocolTracing getProtocolTracing() { return protocolTracing; }
    public void setProtocolTracing(ProtocolTracing protocolTracing) { this.protocolTracing = protocolTracing; }
    
    public PerformanceMonitoring getPerformanceMonitoring() { return performanceMonitoring; }
    public void setPerformanceMonitoring(PerformanceMonitoring performanceMonitoring) { this.performanceMonitoring = performanceMonitoring; }
    
    public HealthChecks getHealthChecks() { return healthChecks; }
    public void setHealthChecks(HealthChecks healthChecks) { this.healthChecks = healthChecks; }
    
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    
    public Notifications getNotifications() { return notifications; }
    public void setNotifications(Notifications notifications) { this.notifications = notifications; }
    
    public Export getExport() { return export; }
    public void setExport(Export export) { this.export = export; }
    
    /**
     * Protocol tracing configuration.
     */
    public static class ProtocolTracing {
        private boolean enabled = true;
        private Duration retention = Duration.ofHours(24);
        private int bufferSize = 10000;
        private boolean realTimeMonitoring = false;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getRetention() { return retention; }
        public void setRetention(Duration retention) { this.retention = retention; }
        
        public int getBufferSize() { return bufferSize; }
        public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
        
        public boolean isRealTimeMonitoring() { return realTimeMonitoring; }
        public void setRealTimeMonitoring(boolean realTimeMonitoring) { this.realTimeMonitoring = realTimeMonitoring; }
    }
    
    /**
     * Performance monitoring configuration.
     */
    public static class PerformanceMonitoring {
        private boolean enabled = true;
        private Duration collectionInterval = Duration.ofSeconds(1);
        private Map<String, String> thresholds = new HashMap<>();
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getCollectionInterval() { return collectionInterval; }
        public void setCollectionInterval(Duration collectionInterval) { this.collectionInterval = collectionInterval; }
        
        public Map<String, String> getThresholds() { return thresholds; }
        public void setThresholds(Map<String, String> thresholds) { this.thresholds = thresholds; }
    }
    
    /**
     * Health check configuration.
     */
    public static class HealthChecks {
        private boolean enabled = true;
        private Duration interval = Duration.ofMinutes(5);
        private Duration timeout = Duration.ofSeconds(30);
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getInterval() { return interval; }
        public void setInterval(Duration interval) { this.interval = interval; }
        
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
    }
    
    /**
     * Storage configuration.
     */
    public static class Storage {
        private String type = "file";
        private String basePath = "debug-data";
        private Duration retention = Duration.ofDays(7);
        private boolean compression = true;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        
        public Duration getRetention() { return retention; }
        public void setRetention(Duration retention) { this.retention = retention; }
        
        public boolean isCompression() { return compression; }
        public void setCompression(boolean compression) { this.compression = compression; }
    }
    
    /**
     * Notification configuration.
     */
    public static class Notifications {
        private boolean enabled = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    /**
     * Export configuration.
     */
    public static class Export {
        private boolean sanitizeSensitiveData = true;
        private String maxExportSize = "100MB";
        
        // Getters and setters
        public boolean isSanitizeSensitiveData() { return sanitizeSensitiveData; }
        public void setSanitizeSensitiveData(boolean sanitizeSensitiveData) { this.sanitizeSensitiveData = sanitizeSensitiveData; }
        
        public String getMaxExportSize() { return maxExportSize; }
        public void setMaxExportSize(String maxExportSize) { this.maxExportSize = maxExportSize; }
    }
}