package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for Git repository operations.
 */
public interface GitRepositoryService {
    
    /**
     * Initializes a new Git repository at the specified path.
     */
    RepositoryInfo initRepository(Path path);
    
    /**
     * Clones a repository from a remote URL.
     */
    RepositoryInfo cloneRepository(String url, Path targetPath, Credentials credentials);
    
    /**
     * Gets the current status of a repository.
     */
    RepositoryStatus getStatus(Path repositoryPath);
    
    /**
     * Gets commit history with optional limit.
     */
    List<CommitInfo> getHistory(Path repositoryPath, int limit);
    
    /**
     * Gets details for a specific commit.
     */
    CommitInfo getCommitDetails(Path repositoryPath, String commitHash);
    
    /**
     * Gets the current branch name.
     */
    String getCurrentBranch(Path repositoryPath);
}
