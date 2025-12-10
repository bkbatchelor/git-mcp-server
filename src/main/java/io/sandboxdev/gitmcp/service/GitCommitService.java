package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.AuthorInfo;
import io.sandboxdev.gitmcp.model.CommitInfo;
import io.sandboxdev.gitmcp.model.DiffType;

import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for Git commit and file operations.
 * 
 * <p>This service handles staging, committing, and file content operations.
 * It provides functionality for managing the Git staging area, creating commits,
 * and retrieving file contents and diffs.</p>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GitCommitService {
    
    /**
     * Stages the specified files for the next commit.
     * 
     * <p>Adds the specified files to the Git staging area (index). Files can be
     * new, modified, or deleted. Relative paths are resolved against the repository
     * working directory.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param filePaths list of file paths to stage (relative to repository root)
     * @throws GitMcpException if the repository is not found or staging fails
     * @throws IllegalArgumentException if repositoryPath or filePaths is null
     */
    void stageFiles(Path repositoryPath, List<String> filePaths);
    
    /**
     * Removes the specified files from the staging area.
     * 
     * <p>Unstages files from the Git index while preserving changes in the
     * working directory. This effectively undoes the staging operation.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param filePaths list of file paths to unstage (relative to repository root)
     * @throws GitMcpException if the repository is not found or unstaging fails
     * @throws IllegalArgumentException if repositoryPath or filePaths is null
     */
    void unstageFiles(Path repositoryPath, List<String> filePaths);
    
    /**
     * Creates a new commit with all currently staged changes.
     * 
     * <p>Creates a commit containing all files in the staging area with the
     * specified commit message and author information. The staging area is
     * cleared after successful commit creation.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param message the commit message (must not be empty)
     * @param author author information for the commit (can be null to use Git config)
     * @return {@link CommitInfo} containing details about the created commit
     * @throws GitMcpException if no changes are staged, repository is not found, or commit creation fails
     * @throws IllegalArgumentException if repositoryPath or message is null/empty
     */
    CommitInfo createCommit(Path repositoryPath, String message, AuthorInfo author);
    
    /**
     * Retrieves a diff for the specified type and references.
     * 
     * <p>Generates a unified diff showing changes between different states of the
     * repository. The diff type determines what is compared (staged vs unstaged,
     * commit vs commit, etc.).</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param type the type of diff to generate (STAGED, UNSTAGED, COMMIT_TO_COMMIT)
     * @param refs optional commit references for commit-to-commit diffs
     * @return unified diff string showing the changes
     * @throws GitMcpException if the repository is not found or diff generation fails
     * @throws IllegalArgumentException if repositoryPath or type is null
     */
    String getDiff(Path repositoryPath, DiffType type, String... refs);
    
    /**
     * Retrieves the contents of a file at a specific commit.
     * 
     * <p>Returns the file contents as they existed in the specified commit.
     * This allows browsing historical versions of files without affecting
     * the working directory.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param commitHash the SHA-1 hash of the commit (full or abbreviated)
     * @param filePath the path to the file (relative to repository root)
     * @return the file contents as a string
     * @throws GitMcpException if the repository, commit, or file is not found
     * @throws IllegalArgumentException if any parameter is null
     */
    String getFileContents(Path repositoryPath, String commitHash, String filePath);
}
