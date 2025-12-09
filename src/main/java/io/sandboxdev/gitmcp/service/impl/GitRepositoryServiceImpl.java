package io.sandboxdev.gitmcp.service.impl;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.*;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitRepositoryServiceImpl implements GitRepositoryService {
    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryServiceImpl.class);
    
    private final JGitRepositoryManager repositoryManager;
    private final JGitCommandExecutor commandExecutor;

    
    public GitRepositoryServiceImpl(JGitRepositoryManager repositoryManager, 
                                   JGitCommandExecutor commandExecutor) {
        this.repositoryManager = repositoryManager;
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public RepositoryInfo initRepository(Path path) {
        logger.debug("Initializing repository at: {}", path);
        
        try {
            File repoDir = path.toFile();
            
            // Check if repository already exists
            if (repoDir.exists() && new File(repoDir, ".git").exists()) {
                throw new GitMcpException(
                    ErrorCode.REPOSITORY_ALREADY_EXISTS,
                    "Repository already exists at: " + path
                );
            }
            
            // Create directory if it doesn't exist
            if (!repoDir.exists()) {
                Files.createDirectories(path);
            }
            
            // Initialize repository
            Git git = Git.init().setDirectory(repoDir).call();
            Repository repository = git.getRepository();
            
            String defaultBranch = repository.getBranch();
            boolean isBare = repository.isBare();
            
            git.close();
            
            logger.info("Initialized repository at: {}", path);
            return new RepositoryInfo(path, defaultBranch, isBare);
            
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "init repository");
        }
    }

    
    @Override
    public RepositoryInfo cloneRepository(String url, Path targetPath, Credentials credentials) {
        logger.debug("Cloning repository from {} to {}", url, targetPath);
        
        try {
            CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setDirectory(targetPath.toFile());
            
            // Add credentials if provided
            if (credentials != null && !credentials.isEmpty()) {
                cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(
                        credentials.username(),
                        credentials.password()
                    )
                );
            }
            
            Git git = cloneCommand.call();
            Repository repository = git.getRepository();
            
            String defaultBranch = repository.getBranch();
            boolean isBare = repository.isBare();
            
            git.close();
            
            logger.info("Cloned repository from {} to {}", url, targetPath);
            return new RepositoryInfo(targetPath, defaultBranch, isBare);
            
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "clone repository");
        }
    }

    
    @Override
    public RepositoryStatus getStatus(Path repositoryPath) {
        logger.debug("Getting status for repository: {}", repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                Status status = git.status().call();
            String currentBranch = repository.getBranch();
            
            List<String> stagedFiles = new ArrayList<>(status.getAdded());
            stagedFiles.addAll(status.getChanged());
            stagedFiles.addAll(status.getRemoved());
            
            List<String> unstagedFiles = new ArrayList<>(status.getModified());
            unstagedFiles.addAll(status.getMissing());
            
            List<String> untrackedFiles = new ArrayList<>(status.getUntracked());
            
                boolean hasUncommittedChanges = !stagedFiles.isEmpty() || !unstagedFiles.isEmpty();
                
                return new RepositoryStatus(
                    currentBranch,
                    stagedFiles,
                    unstagedFiles,
                    untrackedFiles,
                    hasUncommittedChanges
                );
            }
            
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "get status");
        }
    }

    
    @Override
    public List<CommitInfo> getHistory(Path repositoryPath, int limit) {
        logger.debug("Getting history for repository: {} (limit: {})", repositoryPath, limit);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().setMaxCount(limit).call();
                List<CommitInfo> history = new ArrayList<>();
                
                for (RevCommit commit : commits) {
                    history.add(convertToCommitInfo(commit));
                }
                
                logger.debug("Retrieved {} commits", history.size());
                return history;
            }
            
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "get history");
        }
    }
    
    @Override
    public CommitInfo getCommitDetails(Path repositoryPath, String commitHash) {
        logger.debug("Getting commit details: {} in {}", commitHash, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            
            try (RevWalk revWalk = new RevWalk(repository)) {
                ObjectId commitId = repository.resolve(commitHash);
                if (commitId == null) {
                    throw new GitMcpException(
                        ErrorCode.INVALID_PARAMETERS,
                        "Invalid commit hash: " + commitHash
                    );
                }
                
                RevCommit commit = revWalk.parseCommit(commitId);
                return convertToCommitInfo(commit);
            }
            
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "get commit details");
        }
    }

    
    @Override
    public String getCurrentBranch(Path repositoryPath) {
        logger.debug("Getting current branch for: {}", repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            String branch = repository.getBranch();
            
            if (branch == null || branch.isEmpty()) {
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Repository has no current branch"
                );
            }
            
            return branch;
            
        } catch (IOException e) {
            throw commandExecutor.translateException(e, "get current branch");
        }
    }
    
    /**
     * Converts a RevCommit to CommitInfo.
     */
    private CommitInfo convertToCommitInfo(RevCommit commit) {
        PersonIdent author = commit.getAuthorIdent();
        
        return new CommitInfo(
            commit.getName(),
            commit.abbreviate(7).name(),
            commit.getFullMessage(),
            new AuthorInfo(author.getName(), author.getEmailAddress()),
            author.getWhenAsInstant(),
            List.of(), // Changed files - would need additional processing
            new DiffStats(0, 0, 0) // Stats - would need additional processing
        );
    }
}
