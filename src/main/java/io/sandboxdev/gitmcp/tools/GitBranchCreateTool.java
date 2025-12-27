package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitBranchCreateToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class GitBranchCreateTool {
    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;

    public GitBranchCreateTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
    }

    public GitBranchCreateTool() {
        this(new JGitRepositoryManager(), new ObjectMapper());
    }

    public ToolResult execute(GitBranchCreateToolSchema schema) {
        try {
            Path repositoryPath = Paths.get(schema.repositoryPath());

            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            if (schema.branchName() == null || schema.branchName().trim().isEmpty()) {
                return ToolResult.error("Branch name cannot be empty");
            }

            Repository repo = repositoryManager.getRepository(repositoryPath);
            repositoryManager.createBranch(repo, schema.branchName());

            return ToolResult.success(objectMapper.createObjectNode()
                    .put("message", "Branch created successfully: " + schema.branchName())
                    .put("branchName", schema.branchName()));

        } catch (Exception e) {
            return ToolResult.error("Failed to create branch: " + e.getMessage());
        }
    }
}
