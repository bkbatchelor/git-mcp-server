package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitCheckoutToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class GitCheckoutTool {
    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;

    public GitCheckoutTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
    }

    public GitCheckoutTool() {
        this(new JGitRepositoryManager(), new ObjectMapper());
    }

    public ToolResult execute(GitCheckoutToolSchema schema) {
        try {
            Path repositoryPath = Paths.get(schema.repositoryPath());

            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            Repository repo = repositoryManager.getRepository(repositoryPath);

            // Check for uncommitted changes to prevent data loss or conflicts
            if (repositoryManager.hasUncommittedChanges(repo)) {
                return ToolResult.error(
                        "Cannot checkout branch: Repository has uncommitted changes. Please commit or stash them first.");
            }

            repositoryManager.checkout(repo, schema.branchName());

            return ToolResult.success(objectMapper.createObjectNode()
                    .put("message", "Checked out branch: " + schema.branchName())
                    .put("currentBranch", schema.branchName()));

        } catch (Exception e) {
            return ToolResult.error("Failed to checkout branch: " + e.getMessage());
        }
    }
}
