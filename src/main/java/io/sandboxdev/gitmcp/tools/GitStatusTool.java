package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.config.GitMcpProperties;
import io.sandboxdev.gitmcp.model.GitStatus;
import io.sandboxdev.gitmcp.model.GitStatusToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.security.GitInputValidator;
import io.sandboxdev.gitmcp.security.SecurityGuardrails;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import io.sandboxdev.gitmcp.security.GitInputValidator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Git status tool implementation.
 *
 * Provides Git working tree status information including modified, staged, and
 * untracked files.
 */
@Component
public class GitStatusTool {

    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;
    private final GitInputValidator validator;

    public GitStatusTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper,
            GitInputValidator validator) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public GitStatusTool() {
        this.repositoryManager = new JGitRepositoryManager();
        this.objectMapper = new ObjectMapper();
        // Create minimal SecurityGuardrails for testing
        var properties = new GitMcpProperties(
                new GitMcpProperties.TransportConfig(true, false, 8080, Duration.ofSeconds(30)),
                new GitMcpProperties.SecurityConfig(List.of("/"), true, 10, 5),
                new GitMcpProperties.RepositoryConfig("main", false, 30, "10MB"),
                new GitMcpProperties.HeadlessConfig(false, false, 10, false),
                new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
        this.validator = new GitInputValidator(new SecurityGuardrails(properties));
    }

    public ToolResult execute(GitStatusToolSchema schema) {
        try {
            Path repositoryPath = Paths.get(schema.repositoryPath());

            // Validate repository path
            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            // Get Git status using JGitRepositoryManager
            GitStatus status = repositoryManager.getStatus(repositoryPath);

            // Convert to JSON and return
            return ToolResult.success(objectMapper.valueToTree(status));

        } catch (Exception e) {
            return ToolResult.error("Failed to get Git status: " + e.getMessage());
        }
    }
}
