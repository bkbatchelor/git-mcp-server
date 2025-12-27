package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitDiffToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.springframework.stereotype.Component;

import java.nio.file.Path; // Added import
import java.io.IOException;

/**
 * Git diff tool implementation.
 */
@Component
public class GitDiffTool {

    private final JGitRepositoryManager repositoryManager;
    private final ObjectMapper objectMapper;

    // Construtor for production
    public GitDiffTool(JGitRepositoryManager repositoryManager, ObjectMapper objectMapper) {
        this.repositoryManager = repositoryManager;
        this.objectMapper = objectMapper;
    }

    // Constructor for testing
    public GitDiffTool() {
        this.repositoryManager = new JGitRepositoryManager();
        this.objectMapper = new ObjectMapper();
    }

    public ToolResult execute(GitDiffToolSchema schema) {
        try {
            Path repositoryPath = java.nio.file.Paths.get(schema.repositoryPath());

            // Validate repository path
            if (!repositoryManager.isValidRepository(repositoryPath)) {
                return ToolResult.error("Repository not found at path: " + schema.repositoryPath());
            }

            // Get repository instance
            var repo = repositoryManager.getRepository(repositoryPath);

            String diffOutput;
            String filePath = schema.filePath().orElse(null);

            if (schema.fromRef().isPresent()) {
                // Case: valid fromRef, optional toRef
                String fromRef = schema.fromRef().get();
                String toRef = schema.toRef().orElse(null); // If null, diff against working tree? No, JGit diff default
                                                            // is index vs workdir mostly.
                // Wait.
                // If fromRef is present but toRef is empty:
                // JGit diff with one tree usually compares that tree vs WORKDIR (or index).
                // Actually JGitRepositoryManager.getDiff(repo, fromRef, toRef) handles nulls.
                // If toRef is null, getDiff currently uses `prepareTreeParser` only if not
                // null.
                // If setNewTree is not called, it compares against what?
                // The current implementation of getDiff in JGitRepositoryManager sets oldTree
                // if fromRef!=null.
                // If newTree is not set, DiffCommand defaults to... comparing against working
                // tree?
                // Let's check JGit docs or assume standard behavior.
                // Usually DiffCommand compares cached index vs working tree if both trees are
                // null.
                // If oldTree is set, and newTree is null (default), it compares oldTree vs ?
                // DiffCommand: "If no trees are defined, the Working TreeIterator and the
                // DirCacheIterator ... are set."
                // "If setOldTree is called... setOldTree(AbstractTreeIterator oldTree)"
                // Ideally we want:
                // 1. Unstaged (Working Tree vs Index): no refs.
                // 2. Cached (Index vs HEAD): --cached. (Not explicitly requested in schema,
                // maybe later).
                // 3. Commit vs Working Tree: fromRef, no toRef.
                // 4. Commit vs Commit: fromRef, toRef.

                // My JGitRepositoryManager.getDiff handles setting trees if they are not null.
                // If toRef is null, it doesn't set newTree.
                // This implies comparison against working tree if fromRef is set.

                diffOutput = repositoryManager.getDiff(repo, fromRef, toRef, filePath);
            } else {
                // Case: No refs. Unstaged changes (Index vs Working Tree)
                diffOutput = repositoryManager.getUnstagedDiff(repo, filePath);
            }

            // Return plain text diff
            return ToolResult.success(objectMapper.valueToTree(diffOutput));

        } catch (Exception e) {
            return ToolResult.error("Failed to get diff: " + e.getMessage());
        }
    }
}
