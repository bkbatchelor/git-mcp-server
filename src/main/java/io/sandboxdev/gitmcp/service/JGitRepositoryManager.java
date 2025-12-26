package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.GitBranchInfo;
import io.sandboxdev.gitmcp.model.GitCommitInfo;
import io.sandboxdev.gitmcp.model.GitStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Git repository manager using JGit.
 */
@Service
public class JGitRepositoryManager {
    
    private final ConcurrentHashMap<Path, Repository> repositoryCache = new ConcurrentHashMap<>();
    
    /**
     * Get or create a Git repository instance.
     */
    public Repository getRepository(Path repositoryPath) throws IOException {
        if (repositoryPath == null) {
            throw new IllegalArgumentException("Repository path cannot be null");
        }
        
        return repositoryCache.computeIfAbsent(repositoryPath, path -> {
            try {
                // Check if path exists and is a directory
                if (!path.toFile().exists()) {
                    throw new IOException("Repository path does not exist: " + path);
                }
                
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                
                // Try to find .git directory
                if (path.resolve(".git").toFile().exists()) {
                    builder.setGitDir(path.resolve(".git").toFile());
                } else {
                    builder.setWorkTree(path.toFile());
                }
                
                Repository repo = builder
                    .readEnvironment()
                    .findGitDir()
                    .build();
                    
                // Validate that this is actually a Git repository
                if (repo.getObjectDatabase() == null) {
                    throw new IOException("Not a valid Git repository: " + path);
                }
                
                return repo;
            } catch (IOException e) {
                throw new RuntimeException("Failed to open repository: " + path, e);
            }
        });
    }
    
    /**
     * Get Git status for repository.
     */
    public GitStatus getStatus(Repository repo) throws GitAPIException {
        try (Git git = new Git(repo)) {
            Status status = git.status().call();
            
            return new GitStatus(
                new ArrayList<>(status.getModified()),
                new ArrayList<>(status.getAdded()),
                new ArrayList<>(status.getUntracked()),
                status.isClean()
            );
        }
    }
    
    /**
     * Get Git status for repository by path.
     */
    public GitStatus getStatus(Path repositoryPath) throws IOException, GitAPIException {
        Repository repo = getRepository(repositoryPath);
        return getStatus(repo);
    }
    
    /**
     * Create a commit with the given message.
     */
    public ObjectId commit(Repository repo, String message) throws GitAPIException {
        try (Git git = new Git(repo)) {
            RevCommit commit = git.commit()
                .setMessage(message)
                .call();
            return commit.getId();
        }
    }
    
    /**
     * Get commit information.
     */
    public GitCommitInfo getCommitInfo(Repository repo, ObjectId commitId) throws IOException {
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            
            return new GitCommitInfo(
                commit.getId().getName(),
                commit.getId().abbreviate(7).name(),
                commit.getAuthorIdent().getName(),
                commit.getAuthorIdent().getEmailAddress(),
                Instant.ofEpochSecond(commit.getCommitTime()),
                commit.getFullMessage()
            );
        }
    }
    
    /**
     * List branches in repository.
     */
    public List<GitBranchInfo> listBranches(Repository repo) throws GitAPIException {
        try (Git git = new Git(repo)) {
            String currentBranch = repo.getBranch();
            List<GitBranchInfo> branches = new ArrayList<>();
            
            git.branchList().call().forEach(ref -> {
                String branchName = ref.getName().replace("refs/heads/", "");
                boolean isCurrent = branchName.equals(currentBranch);
                
                branches.add(new GitBranchInfo(
                    branchName,
                    ref.getObjectId().getName(),
                    isCurrent,
                    false
                ));
            });
            
            return branches;
        } catch (IOException e) {
            throw new GitAPIException("Failed to get current branch", e) {};
        }
    }
    
    /**
     * Get commit log with optional limit.
     */
    public List<GitCommitInfo> getLog(Repository repo, int limit) throws GitAPIException {
        try (Git git = new Git(repo)) {
            List<GitCommitInfo> commits = new ArrayList<>();
            
            git.log()
                .setMaxCount(limit > 0 ? limit : Integer.MAX_VALUE)
                .call()
                .forEach(commit -> {
                    commits.add(new GitCommitInfo(
                        commit.getId().getName(),
                        commit.getId().abbreviate(7).name(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        Instant.ofEpochSecond(commit.getCommitTime()),
                        commit.getFullMessage()
                    ));
                });
                
            return commits;
        }
    }
    
    /**
     * Get commit log for a specific file path.
     */
    public List<GitCommitInfo> getLog(Repository repo, int limit, String filePath) throws GitAPIException {
        try (Git git = new Git(repo)) {
            List<GitCommitInfo> commits = new ArrayList<>();
            
            var logCommand = git.log()
                .setMaxCount(limit > 0 ? limit : Integer.MAX_VALUE);
                
            if (filePath != null && !filePath.isEmpty()) {
                logCommand.addPath(filePath);
            }
            
            logCommand.call().forEach(commit -> {
                commits.add(new GitCommitInfo(
                    commit.getId().getName(),
                    commit.getId().abbreviate(7).name(),
                    commit.getAuthorIdent().getName(),
                    commit.getAuthorIdent().getEmailAddress(),
                    Instant.ofEpochSecond(commit.getCommitTime()),
                    commit.getFullMessage()
                ));
            });
                
            return commits;
        }
    }
    
    /**
     * Get diff between two commits or refs.
     */
    public String getDiff(Repository repo, String fromRef, String toRef) throws GitAPIException, IOException {
        try (Git git = new Git(repo)) {
            var diffCommand = git.diff();
            
            if (fromRef != null) {
                ObjectId fromCommit = repo.resolve(fromRef);
                if (fromCommit != null) {
                    diffCommand.setOldTree(prepareTreeParser(repo, fromCommit));
                }
            }
            if (toRef != null) {
                ObjectId toCommit = repo.resolve(toRef);
                if (toCommit != null) {
                    diffCommand.setNewTree(prepareTreeParser(repo, toCommit));
                }
            }
            
            var diffEntries = diffCommand.call();
            
            // Format as unified diff
            StringBuilder diffOutput = new StringBuilder();
            for (var entry : diffEntries) {
                diffOutput.append("diff --git a/")
                    .append(entry.getOldPath())
                    .append(" b/")
                    .append(entry.getNewPath())
                    .append("\n");
                    
                diffOutput.append("index ")
                    .append(entry.getOldId().name())
                    .append("..")
                    .append(entry.getNewId().name())
                    .append("\n");
                    
                diffOutput.append("--- a/").append(entry.getOldPath()).append("\n");
                diffOutput.append("+++ b/").append(entry.getNewPath()).append("\n");
                diffOutput.append("@@ -0,0 +0,0 @@\n"); // Simplified diff header
            }
            
            return diffOutput.toString();
        }
    }
    
    /**
     * Helper method to prepare tree parser for diff operations.
     */
    private org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(Repository repo, ObjectId commitId) throws IOException {
        try (org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repo)) {
            org.eclipse.jgit.revwalk.RevCommit commit = walk.parseCommit(commitId);
            org.eclipse.jgit.revwalk.RevTree tree = walk.parseTree(commit.getTree().getId());
            
            org.eclipse.jgit.treewalk.CanonicalTreeParser treeParser = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            try (org.eclipse.jgit.lib.ObjectReader reader = repo.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            
            return treeParser;
        }
    }
    
    /**
     * Get unstaged changes (working tree vs index).
     */
    public String getUnstagedDiff(Repository repo) throws GitAPIException {
        try (Git git = new Git(repo)) {
            var diffEntries = git.diff().call();
            
            StringBuilder diffOutput = new StringBuilder();
            for (var entry : diffEntries) {
                diffOutput.append("diff --git a/")
                    .append(entry.getOldPath())
                    .append(" b/")
                    .append(entry.getNewPath())
                    .append("\n");
            }
            
            return diffOutput.toString();
        }
    }
    
    /**
     * Create a new branch from current HEAD.
     */
    public void createBranch(Repository repo, String branchName) throws GitAPIException {
        try (Git git = new Git(repo)) {
            git.branchCreate()
                .setName(branchName)
                .call();
        }
    }
    
    /**
     * Checkout a branch or commit.
     */
    public void checkout(Repository repo, String ref) throws GitAPIException {
        try (Git git = new Git(repo)) {
            git.checkout()
                .setName(ref)
                .call();
        }
    }
    
    /**
     * Check if repository has uncommitted changes.
     */
    public boolean hasUncommittedChanges(Repository repo) throws GitAPIException {
        GitStatus status = getStatus(repo);
        return !status.isClean();
    }
    
    /**
     * Validate that path points to a valid Git repository.
     */
    public boolean isValidRepository(Path repositoryPath) {
        try {
            // Don't use the cache for validation to avoid caching failed attempts
            if (repositoryPath == null || !repositoryPath.toFile().exists()) {
                return false;
            }
            
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            
            // Try to find .git directory
            if (repositoryPath.resolve(".git").toFile().exists()) {
                builder.setGitDir(repositoryPath.resolve(".git").toFile());
            } else {
                builder.setWorkTree(repositoryPath.toFile());
            }
            
            try (Repository repo = builder
                    .readEnvironment()
                    .findGitDir()
                    .build()) {
                return repo.getObjectDatabase() != null;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Close and cleanup repository resources.
     */
    public void closeRepository(Path repositoryPath) {
        Repository repo = repositoryCache.remove(repositoryPath);
        if (repo != null) {
            repo.close();
        }
    }
    
    /**
     * Close all cached repositories.
     */
    public void closeAll() {
        repositoryCache.values().forEach(Repository::close);
        repositoryCache.clear();
    }
}
