package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.BranchInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for Git branch operations.
 */
public interface GitBranchService {
    
    /**
     * Creates a new branch.
     */
    BranchInfo createBranch(Path repositoryPath, String branchName);
    
    /**
     * Switches to a different branch.
     */
    void switchBranch(Path repositoryPath, String branchName);
    
    /**
     * Lists all branches.
     */
    List<BranchInfo> listBranches(Path repositoryPath);
    
    /**
     * Deletes a branch.
     */
    void deleteBranch(Path repositoryPath, String branchName);
}
