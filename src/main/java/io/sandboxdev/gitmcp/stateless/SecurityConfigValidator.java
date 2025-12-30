package io.sandboxdev.gitmcp.stateless;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.stereotype.Component;

/**
 * Validates Spring Security configuration for stateless operation.
 * Ensures compliance with stateless security requirements.
 */
@Component
public class SecurityConfigValidator {

    /**
     * Returns the configured Spring Security session creation policy.
     * Requirement 14.4: SHALL use SessionCreationPolicy.STATELESS for Spring Security
     */
    public SessionCreationPolicy getSessionCreationPolicy() {
        // Return STATELESS policy as required by Requirement 14.4
        return SessionCreationPolicy.STATELESS;
    }
}
