package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.AuthorInfo;
import io.sandboxdev.gitmcp.model.CommitInfo;
import io.sandboxdev.gitmcp.model.DiffType;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for Git commit and file operations.
 */
public interface GitCommitService {
    
    /**
     * Stages files for commit.
     */
    void stageFiles(Path repositoryPath, List<String> filePaths);
    
    /**
     * Unstages files from the staging area.
     */
    void unstageFiles(Path repositoryPath, List<String> filePaths);
    
    /**
     * Creates a commit with staged changes.
     */
    CommitInfo createCommit(Path repositoryPath, String message, AuthorInfo author);
    
    /**
     * Gets diff for specified type.
     */
    String getDiff(Path repositoryPath, DiffType type, String... refs);
    
    /**
     * Gets file contents at a specific commit.
     */
    String getFileContents(Path repositoryPath, String commitHash, String filePath);
}
