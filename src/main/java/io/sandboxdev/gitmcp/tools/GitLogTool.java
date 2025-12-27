package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitLogToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
public class GitLogTool {
    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;

    public GitLogTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
    }

    public GitLogTool() {
        this(new JGitRepositoryManager(), new ObjectMapper().findAndRegisterModules());
    }

    public ToolResult execute(GitLogToolSchema schema) {
        try {
            Path repositoryPath = java.nio.file.Paths.get(schema.repositoryPath());

            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            org.eclipse.jgit.lib.Repository repo = repositoryManager.getRepository(repositoryPath);
            int limit = schema.limit().orElse(0);
            String filePath = schema.filePath().orElse(null);

            java.util.List<io.sandboxdev.gitmcp.model.GitCommitInfo> commits = repositoryManager.getLog(repo, limit,
                    filePath);

            return ToolResult.success(objectMapper.valueToTree(commits));

        } catch (Exception e) {
            return ToolResult.error("Failed to get git log: " + e.getMessage());
        }
    }
}
