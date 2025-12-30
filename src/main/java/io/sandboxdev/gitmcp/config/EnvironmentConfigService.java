package io.sandboxdev.gitmcp.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Service for handling environment variable configuration.
 * Requirement 15.3: API keys read exclusively from environment variables
 * Requirement 15.4: Validate configuration at startup and fail fast if required properties missing
 */
@Component
public class EnvironmentConfigService {

    private final Environment environment;

    public EnvironmentConfigService(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validates required environment variables at startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateEnvironmentConfiguration() {
        // Validate API keys if AI features are enabled
        if (isAiEnabled()) {
            validateApiKey("OPENAI_API_KEY", "OpenAI API key");
        }
    }

    /**
     * Gets API key from environment variables only.
     * Requirement 15.3: API keys read exclusively from environment variables
     */
    public Optional<String> getApiKey(String keyName) {
        return Optional.ofNullable(System.getenv(keyName));
    }

    private boolean isAiEnabled() {
        return environment.getProperty("spring.ai.enabled", Boolean.class, false);
    }

    private void validateApiKey(String keyName, String description) {
        if (getApiKey(keyName).isEmpty()) {
            throw new IllegalStateException(
                String.format("Required environment variable %s (%s) is not set", keyName, description)
            );
        }
    }
}
