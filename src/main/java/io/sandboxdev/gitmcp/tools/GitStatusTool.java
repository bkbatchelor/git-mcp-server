package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitStatus;
import io.sandboxdev.gitmcp.model.GitStatusToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import io.sandboxdev.gitmcp.security.GitInputValidator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        this.validator = new GitInputValidator();
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
