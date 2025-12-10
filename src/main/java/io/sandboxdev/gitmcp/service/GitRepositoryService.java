package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for Git repository operations.
 * 
 * <p>This service provides core repository management functionality including
 * initialization, cloning, status retrieval, and history browsing. All operations
 * are thread-safe and handle JGit exceptions by translating them to domain-specific
 * exceptions.</p>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GitRepositoryService {
    
    /**
     * Initializes a new Git repository at the specified path.
     * 
     * <p>Creates a new Git repository with default configuration. The repository
     * will be initialized with a .git directory and default branch (typically 'main').</p>
     * 
     * @param path the file system path where the repository should be initialized
     * @return {@link RepositoryInfo} containing details about the newly created repository
     * @throws GitMcpException if the path already contains a repository or if initialization fails
     * @throws IllegalArgumentException if the path is null or invalid
     */
    RepositoryInfo initRepository(Path path);
    
    /**
     * Clones a repository from a remote URL to a local path.
     * 
     * <p>Downloads the complete repository history and creates a local working copy.
     * Supports both public and private repositories through optional credentials.</p>
     * 
     * @param url the remote repository URL (HTTP, HTTPS, or SSH)
     * @param targetPath the local path where the repository should be cloned
     * @param credentials optional credentials for authentication (can be null for public repos)
     * @return {@link RepositoryInfo} containing details about the cloned repository
     * @throws GitMcpException if cloning fails due to network, authentication, or file system issues
     * @throws IllegalArgumentException if url or targetPath is null
     */
    RepositoryInfo cloneRepository(String url, Path targetPath, Credentials credentials);
    
    /**
     * Retrieves the current status of a Git repository.
     * 
     * <p>Returns comprehensive status information including current branch,
     * staged files, unstaged changes, and untracked files.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @return {@link RepositoryStatus} containing current repository state
     * @throws GitMcpException if the repository is not found or is in an invalid state
     * @throws IllegalArgumentException if repositoryPath is null
     */
    RepositoryStatus getStatus(Path repositoryPath);
    
    /**
     * Retrieves commit history from the repository.
     * 
     * <p>Returns a list of commits starting from HEAD, ordered from most recent
     * to oldest. The number of commits returned is limited by the specified limit.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param limit maximum number of commits to return (must be positive)
     * @return list of {@link CommitInfo} objects representing the commit history
     * @throws GitMcpException if the repository is not found or history cannot be retrieved
     * @throws IllegalArgumentException if repositoryPath is null or limit is not positive
     */
    List<CommitInfo> getHistory(Path repositoryPath, int limit);
    
    /**
     * Retrieves detailed information about a specific commit.
     * 
     * <p>Returns comprehensive commit information including metadata, changed files,
     * and diff statistics for the specified commit hash.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param commitHash the SHA-1 hash of the commit (full or abbreviated)
     * @return {@link CommitInfo} containing detailed commit information
     * @throws GitMcpException if the repository or commit is not found
     * @throws IllegalArgumentException if repositoryPath or commitHash is null
     */
    CommitInfo getCommitDetails(Path repositoryPath, String commitHash);
    
    /**
     * Retrieves the name of the currently checked-out branch.
     * 
     * <p>Returns the name of the current branch. For detached HEAD state,
     * returns the commit hash instead of a branch name.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @return the current branch name or commit hash if in detached HEAD state
     * @throws GitMcpException if the repository is not found or current branch cannot be determined
     * @throws IllegalArgumentException if repositoryPath is null
     */
    String getCurrentBranch(Path repositoryPath);
}
