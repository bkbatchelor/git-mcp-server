package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitCommitInfo;
import io.sandboxdev.gitmcp.model.GitCommitToolSchema;
import io.sandboxdev.gitmcp.model.GitStatus;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Git commit tool implementation.
 * 
 * Creates commits with provided messages for repositories with staged changes.
 */
@Component
public class GitCommitTool {

    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;

    public GitCommitTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
    }

    public GitCommitTool() {
        this.repositoryManager = new JGitRepositoryManager();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Register JSR310 module for Java 8 time types
    }

    public ToolResult execute(GitCommitToolSchema schema) {
        try {
            Path repositoryPath = Paths.get(schema.repositoryPath());
            
            // Validate repository path
            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            // Validate commit message
            if (isInvalidCommitMessage(schema.message())) {
                return ToolResult.error("Invalid commit message");
            }

            Repository repo = repositoryManager.getRepository(repositoryPath);
            
            // Check if there are staged changes
            GitStatus status = repositoryManager.getStatus(repo);
            if (status.stagedFiles().isEmpty()) {
                return ToolResult.error("nothing to commit");
            }

            // Create commit
            ObjectId commitId = repositoryManager.commit(repo, schema.message());
            
            // Get commit info
            GitCommitInfo commitInfo = repositoryManager.getCommitInfo(repo, commitId);
            
            // Convert to JSON and return
            return ToolResult.success(objectMapper.valueToTree(commitInfo));
            
        } catch (Exception e) {
            return ToolResult.error("Failed to create commit: " + e.getMessage());
        }
    }

    private boolean isInvalidCommitMessage(String message) {
        return message == null || message.trim().isEmpty() || 
               message.chars().allMatch(c -> c <= 31); // Control characters only
    }
}
