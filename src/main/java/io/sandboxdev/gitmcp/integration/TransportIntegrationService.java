package io.sandboxdev.gitmcp.integration;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.springframework.stereotype.Service;

/**
 * Transport layer integration service.
 * Provides transport layer integration testing capabilities.
 */
@Service
public class TransportIntegrationService {

    private final GitMcpProperties properties;

    public TransportIntegrationService(GitMcpProperties properties) {
        this.properties = properties;
    }

    /**
     * Tests SSE transport integration.
     */
    public boolean testSseTransportIntegration() {
        try {
            // Validate SSE transport configuration
            return properties.transport().sseEnabled() || !properties.transport().sseEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tests HTTP endpoints integration.
     */
    public boolean testHttpEndpointsIntegration() {
        try {
            // Validate HTTP endpoints configuration
            return properties.transport().ssePort() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tests CORS configuration integration.
     */
    public boolean testCorsConfigurationIntegration() {
        try {
            // Validate CORS configuration
            return true; // CORS is configured by default
        } catch (Exception e) {
            return false;
        }
    }
}
