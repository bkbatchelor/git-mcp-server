package io.sandboxdev.gitmcp.headless;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.springframework.stereotype.Component;

/**
 * Handles batch processing for sequential Git operations.
 * Requirement 16.6: Batch processing mode for sequential Git operations
 * Requirement 16.8: Docker deployment with mounted configuration files
 */
@Component
public class BatchProcessor {

    private final GitMcpProperties.HeadlessConfig config;

    public BatchProcessor(GitMcpProperties properties) {
        this.config = properties.headless();
    }

    public boolean isBatchProcessingEnabled() {
        return config.batchProcessing();
    }

    public int getShutdownTimeout() {
        return config.shutdownTimeoutSeconds();
    }
}
