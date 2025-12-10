package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.BranchInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for Git branch operations.
 * 
 * <p>This service provides comprehensive branch management functionality including
 * creation, switching, listing, and deletion of Git branches. All operations
 * maintain repository integrity and handle edge cases appropriately.</p>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GitBranchService {
    
    /**
     * Creates a new branch pointing to the current HEAD commit.
     * 
     * <p>Creates a new branch with the specified name starting from the current
     * HEAD commit. The new branch is created but not checked out automatically.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param branchName the name for the new branch (must be valid Git branch name)
     * @return {@link BranchInfo} containing details about the created branch
     * @throws GitMcpException if the repository is not found, branch already exists, or creation fails
     * @throws IllegalArgumentException if repositoryPath or branchName is null/empty
     */
    BranchInfo createBranch(Path repositoryPath, String branchName);
    
    /**
     * Switches the working directory to the specified branch.
     * 
     * <p>Updates the working directory to reflect the state of the target branch.
     * This operation will fail if there are uncommitted changes that would be
     * overwritten by the branch switch.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param branchName the name of the branch to switch to
     * @throws GitMcpException if the repository or branch is not found, or if uncommitted changes prevent switching
     * @throws IllegalArgumentException if repositoryPath or branchName is null/empty
     */
    void switchBranch(Path repositoryPath, String branchName);
    
    /**
     * Retrieves a list of all local branches in the repository.
     * 
     * <p>Returns information about all local branches including their names,
     * commit hashes, and current branch indication. The list is ordered
     * alphabetically by branch name.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @return list of {@link BranchInfo} objects representing all local branches
     * @throws GitMcpException if the repository is not found or branch listing fails
     * @throws IllegalArgumentException if repositoryPath is null
     */
    List<BranchInfo> listBranches(Path repositoryPath);
    
    /**
     * Deletes the specified branch from the repository.
     * 
     * <p>Removes the branch reference from the repository. This operation will
     * fail if attempting to delete the currently checked-out branch or if the
     * branch has unmerged changes.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param branchName the name of the branch to delete
     * @throws GitMcpException if the repository or branch is not found, or if deletion is not allowed
     * @throws IllegalArgumentException if repositoryPath or branchName is null/empty
     */
    void deleteBranch(Path repositoryPath, String branchName);
}
