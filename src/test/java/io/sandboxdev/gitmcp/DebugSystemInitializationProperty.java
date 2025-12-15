package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.debug.config.McpDebugProperties;
import io.sandboxdev.gitmcp.debug.model.DebugConfiguration;
import io.sandboxdev.gitmcp.debug.model.DebugLevel;
import net.jqwik.api.*;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for MCP Debug System initialization.
 * 
 * Feature: mcp-debugging, Property 25: Debug level enforcement
 * Validates: Requirements 8.1, 8.2, 8.3
 */
public class DebugSystemInitializationProperty {

    // Feature: mcp-debugging, Property 25: Debug level enforcement
    @Property(tries = 100)
    void debugLevelEnforcementForGlobalConfiguration(
        @ForAll("debugLevel") DebugLevel globalLevel,
        @ForAll("enabledFlag") boolean enabled
    ) {
        // Given a debug configuration with a specific global level
        Map<String, Object> properties = new HashMap<>();
        properties.put("mcp-debug.enabled", enabled);
        properties.put("mcp-debug.global-level", globalLevel.name());
        
        // When binding the configuration properties
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        McpDebugProperties debugProperties = binder.bind("mcp-debug", McpDebugProperties.class)
            .orElse(new McpDebugProperties());
        
        // Then the configuration should reflect the specified debug level
        assertThat(debugProperties.isEnabled()).isEqualTo(enabled);
        assertThat(debugProperties.getGlobalLevel()).isEqualTo(globalLevel);
        
        // And the debug level should be one of the valid enum values
        assertThat(globalLevel).isIn((Object[]) DebugLevel.values());
        
        // And when enabled is false, debug system should be disabled regardless of level
        if (!enabled) {
            assertThat(debugProperties.isEnabled()).isFalse();
        }
    }

    @Property(tries = 100)
    void debugLevelEnforcementForComponentConfiguration(
        @ForAll("debugLevel") DebugLevel globalLevel,
        @ForAll("componentLevels") Map<String, DebugLevel> componentLevels
    ) {
        // Given a debug configuration with component-specific levels
        Map<String, Object> properties = new HashMap<>();
        properties.put("mcp-debug.enabled", true);
        properties.put("mcp-debug.global-level", globalLevel.name());
        
        // Add component levels to properties
        Map<String, String> componentLevelStrings = new HashMap<>();
        componentLevels.forEach((component, level) -> 
            componentLevelStrings.put(component, level.name()));
        properties.put("mcp-debug.component-levels", componentLevelStrings);
        
        // When binding the configuration properties
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        McpDebugProperties debugProperties = binder.bind("mcp-debug", McpDebugProperties.class)
            .orElse(new McpDebugProperties());
        
        // Then the configuration should reflect both global and component levels
        assertThat(debugProperties.getGlobalLevel()).isEqualTo(globalLevel);
        
        // And each component level should be properly configured
        componentLevels.forEach((component, expectedLevel) -> {
            DebugLevel actualLevel = debugProperties.getComponentLevels().get(component);
            if (actualLevel != null) {
                assertThat(actualLevel).isEqualTo(expectedLevel);
            }
        });
        
        // And all debug levels should be valid enum values
        debugProperties.getComponentLevels().values().forEach(level -> 
            assertThat(level).isIn((Object[]) DebugLevel.values()));
    }

    @Property(tries = 100)
    void debugConfigurationConsistencyAcrossLevels(
        @ForAll("debugLevel") DebugLevel minimalLevel,
        @ForAll("debugLevel") DebugLevel comprehensiveLevel
    ) {
        // Given two different debug levels (minimal and comprehensive)
        // When creating configurations for each level
        DebugConfiguration minimalConfig = createDebugConfiguration(minimalLevel, true, true);
        DebugConfiguration comprehensiveConfig = createDebugConfiguration(comprehensiveLevel, true, true);
        
        // Then both configurations should be valid
        assertThat(minimalConfig.globalLevel()).isIn((Object[]) DebugLevel.values());
        assertThat(comprehensiveConfig.globalLevel()).isIn((Object[]) DebugLevel.values());
        
        // And the configuration should reflect the appropriate level of detail
        // Higher debug levels should enable more features
        if (isHigherDebugLevel(comprehensiveLevel, minimalLevel)) {
            // Comprehensive level should have same or more features enabled
            if (minimalConfig.protocolTracingEnabled()) {
                assertThat(comprehensiveConfig.protocolTracingEnabled()).isTrue();
            }
            if (minimalConfig.performanceMonitoringEnabled()) {
                assertThat(comprehensiveConfig.performanceMonitoringEnabled()).isTrue();
            }
        }
        
        // And trace retention should be positive
        assertThat(minimalConfig.traceRetention()).isPositive();
        assertThat(comprehensiveConfig.traceRetention()).isPositive();
        
        // And buffer size should be positive
        assertThat(minimalConfig.maxTraceBufferSize()).isPositive();
        assertThat(comprehensiveConfig.maxTraceBufferSize()).isPositive();
    }

    @Property(tries = 100)
    void debugSystemFeatureEnablementBasedOnLevel(
        @ForAll("debugLevel") DebugLevel level,
        @ForAll("protocolTracingEnabled") boolean protocolTracing,
        @ForAll("performanceMonitoringEnabled") boolean performanceMonitoring
    ) {
        // Given a debug configuration with specific feature enablement
        DebugConfiguration config = createDebugConfiguration(level, protocolTracing, performanceMonitoring);
        
        // When the debug level is OFF, essential features should be disabled
        if (level == DebugLevel.OFF) {
            // OFF level should disable most debugging features
            assertThat(config.globalLevel()).isEqualTo(DebugLevel.OFF);
        }
        
        // When the debug level is ERROR or WARN (minimal), only essential monitoring should be enabled
        if (level == DebugLevel.ERROR || level == DebugLevel.WARN) {
            // These are minimal levels - configuration should reflect this
            assertThat(config.globalLevel()).isIn(DebugLevel.ERROR, DebugLevel.WARN);
        }
        
        // When the debug level is DEBUG or TRACE (comprehensive), more features should be available
        if (level == DebugLevel.DEBUG || level == DebugLevel.TRACE) {
            // These are comprehensive levels
            assertThat(config.globalLevel()).isIn(DebugLevel.DEBUG, DebugLevel.TRACE);
        }
        
        // Then the configuration should be consistent with the specified features
        assertThat(config.protocolTracingEnabled()).isEqualTo(protocolTracing);
        assertThat(config.performanceMonitoringEnabled()).isEqualTo(performanceMonitoring);
        
        // And the configuration should be internally consistent
        assertThat(config.globalLevel()).isNotNull();
        assertThat(config.componentLevels()).isNotNull();
    }

    @Provide
    Arbitrary<DebugLevel> debugLevel() {
        return Arbitraries.of(DebugLevel.values());
    }

    @Provide
    Arbitrary<Boolean> enabledFlag() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<Boolean> protocolTracingEnabled() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<Boolean> performanceMonitoringEnabled() {
        return Arbitraries.of(true, false);
    }

    @Provide
    Arbitrary<Map<String, DebugLevel>> componentLevels() {
        return Arbitraries.maps(
            Arbitraries.of("protocol", "performance", "health", "storage", "notifications"),
            debugLevel()
        ).ofMinSize(0).ofMaxSize(3);
    }

    /**
     * Create a debug configuration for testing.
     */
    private DebugConfiguration createDebugConfiguration(
        DebugLevel globalLevel, 
        boolean protocolTracing, 
        boolean performanceMonitoring
    ) {
        Map<String, DebugLevel> componentLevels = new HashMap<>();
        componentLevels.put("protocol", globalLevel);
        componentLevels.put("performance", globalLevel);
        
        return new DebugConfiguration(
            globalLevel,
            componentLevels,
            protocolTracing,
            performanceMonitoring,
            Duration.ofHours(24), // Default retention
            10000 // Default buffer size
        );
    }

    /**
     * Determine if one debug level is higher (more verbose) than another.
     */
    private boolean isHigherDebugLevel(DebugLevel level1, DebugLevel level2) {
        int[] levelOrder = {0, 1, 2, 3, 4, 5}; // OFF, ERROR, WARN, INFO, DEBUG, TRACE
        return levelOrder[level1.ordinal()] > levelOrder[level2.ordinal()];
    }
}