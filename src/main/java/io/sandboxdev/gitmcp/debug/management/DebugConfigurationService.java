package io.sandboxdev.gitmcp.debug.management;

import io.sandboxdev.gitmcp.debug.model.DebugConfiguration;
import io.sandboxdev.gitmcp.debug.model.DebugLevel;

/**
 * Interface for managing debug system configuration.
 * Handles debug level configuration and dynamic updates.
 */
public interface DebugConfigurationService {
    
    /**
     * Set the global debug level for the system.
     * 
     * @param level the debug level to set
     */
    void setDebugLevel(DebugLevel level);
    
    /**
     * Set the debug level for a specific component.
     * 
     * @param component the component name
     * @param level the debug level to set for the component
     */
    void setComponentDebugLevel(String component, DebugLevel level);
    
    /**
     * Get the current debug configuration.
     * 
     * @return the current debug configuration
     */
    DebugConfiguration getCurrentConfiguration();
    
    /**
     * Apply a new debug configuration to the system.
     * 
     * @param config the debug configuration to apply
     */
    void applyConfiguration(DebugConfiguration config);
}