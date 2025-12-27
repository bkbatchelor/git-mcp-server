package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitBranchInfo;
import io.sandboxdev.gitmcp.model.GitBranchListToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class GitBranchListTool {
    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;

    public GitBranchListTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
    }

    public GitBranchListTool() {
        this(new JGitRepositoryManager(), new ObjectMapper());
    }

    public ToolResult execute(GitBranchListToolSchema schema) {
        try {
            Path repositoryPath = Paths.get(schema.repositoryPath());

            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            Repository repo = repositoryManager.getRepository(repositoryPath);
            List<GitBranchInfo> branches = repositoryManager.listBranches(repo);

            return ToolResult.success(objectMapper.valueToTree(branches));
        } catch (Exception e) {
            return ToolResult.error("Failed to list branches: " + e.getMessage());
        }
    }
}
