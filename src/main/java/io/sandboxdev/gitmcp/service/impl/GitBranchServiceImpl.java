package io.sandboxdev.gitmcp.service.impl;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.BranchInfo;
import io.sandboxdev.gitmcp.service.GitBranchService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitBranchServiceImpl implements GitBranchService {
    private static final Logger logger = LoggerFactory.getLogger(GitBranchServiceImpl.class);
    
    private final JGitRepositoryManager repositoryManager;
    private final JGitCommandExecutor commandExecutor;
    
    public GitBranchServiceImpl(JGitRepositoryManager repositoryManager,
                               JGitCommandExecutor commandExecutor) {
        this.repositoryManager = repositoryManager;
        this.commandExecutor = commandExecutor;
    }

    
    @Override
    public BranchInfo createBranch(Path repositoryPath, String branchName) {
        logger.debug("Creating branch {} in {}", branchName, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                Ref ref = git.branchCreate()
                    .setName(branchName)
                    .call();
                
                String commitHash = ref.getObjectId().getName();
                
                logger.info("Created branch: {}", branchName);
                return new BranchInfo(branchName, commitHash, false);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "create branch");
        }
    }
    
    @Override
    public void switchBranch(Path repositoryPath, String branchName) {
        logger.debug("Switching to branch {} in {}", branchName, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                
                // Check for uncommitted changes
                if (git.status().call().hasUncommittedChanges()) {
                    throw new GitMcpException(
                        ErrorCode.UNCOMMITTED_CHANGES,
                        "Cannot switch branches with uncommitted changes"
                    );
                }
                
                git.checkout()
                    .setName(branchName)
                    .call();
                
                logger.info("Switched to branch: {}", branchName);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "switch branch");
        }
    }

    
    @Override
    public List<BranchInfo> listBranches(Path repositoryPath) {
        logger.debug("Listing branches in {}", repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                List<Ref> branches = git.branchList().call();
                String currentBranch = repository.getBranch();
                
                List<BranchInfo> branchInfos = new ArrayList<>();
                
                for (Ref branch : branches) {
                    String branchName = branch.getName();
                    // Remove refs/heads/ prefix
                    if (branchName.startsWith("refs/heads/")) {
                        branchName = branchName.substring("refs/heads/".length());
                    }
                    
                    String commitHash = branch.getObjectId().getName();
                    boolean isCurrent = branchName.equals(currentBranch);
                    
                    branchInfos.add(new BranchInfo(branchName, commitHash, isCurrent));
                }
                
                logger.debug("Found {} branches", branchInfos.size());
                return branchInfos;
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "list branches");
        }
    }
    
    @Override
    public void deleteBranch(Path repositoryPath, String branchName) {
        logger.debug("Deleting branch {} in {}", branchName, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                String currentBranch = repository.getBranch();
                
                // Prevent deleting current branch
                if (branchName.equals(currentBranch)) {
                    throw new GitMcpException(
                        ErrorCode.INVALID_REPOSITORY_STATE,
                        "Cannot delete current branch: " + branchName
                    );
                }
                
                git.branchDelete()
                    .setBranchNames(branchName)
                    .call();
                
                logger.info("Deleted branch: {}", branchName);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "delete branch");
        }
    }
}
