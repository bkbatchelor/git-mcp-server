package io.sandboxdev.gitmcp.mcp;

import io.sandboxdev.gitmcp.model.*;
import io.sandboxdev.gitmcp.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP Tool Provider that exposes Git operations as MCP tools using Spring AI MCP.
 * Each method annotated with @McpTool becomes an available MCP tool for AI assistants.
 */
@Component
public class GitMcpToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(GitMcpToolProvider.class);

    private final GitRepositoryService repositoryService;
    private final GitCommitService commitService;
    private final GitBranchService branchService;
    private final GitRemoteService remoteService;
    private final CommitMessageGeneratorService commitMessageService;

    public GitMcpToolProvider(
            GitRepositoryService repositoryService,
            GitCommitService commitService,
            GitBranchService branchService,
            GitRemoteService remoteService,
            CommitMessageGeneratorService commitMessageService) {
        this.repositoryService = repositoryService;
        this.commitService = commitService;
        this.branchService = branchService;
        this.remoteService = remoteService;
        this.commitMessageService = commitMessageService;
    }

    // Repository Operations

    @McpTool(description = "Initialize a new Git repository at the specified path")
    public Map<String, Object> initRepository(String path) {
        logger.debug("MCP Tool invoked: init-repository with path={}", path);
        try {
            RepositoryInfo info = repositoryService.initRepository(Paths.get(path));
            logger.info("Repository initialized successfully at {}", path);
            return Map.of(
                "success", true,
                "path", info.path().toString(),
                "defaultBranch", info.defaultBranch(),
                "isBare", info.isBare()
            );
        } catch (Exception e) {
            logger.error("Failed to initialize repository at {}", path, e);
            throw e;
        }
    }

    @McpTool(description = "Clone a Git repository from a remote URL to a local path")
    public Map<String, Object> cloneRepository(
            String url,
            String targetPath,
            Optional<String> username,
            Optional<String> password) {
        logger.debug("MCP Tool invoked: clone-repository with url={}, targetPath={}", url, targetPath);
        try {
            Credentials credentials = (username.isPresent() && password.isPresent())
                ? new Credentials(username.get(), password.get())
                : null;
            
            RepositoryInfo info = repositoryService.cloneRepository(url, Paths.get(targetPath), credentials);
            logger.info("Repository cloned successfully from {} to {}", url, targetPath);
            return Map.of(
                "success", true,
                "path", info.path().toString(),
                "defaultBranch", info.defaultBranch()
            );
        } catch (Exception e) {
            logger.error("Failed to clone repository from {} to {}", url, targetPath, e);
            throw e;
        }
    }

    @McpTool(description = "Get the current status of a Git repository")
    public Map<String, Object> getStatus(String repositoryPath) {
        logger.debug("MCP Tool invoked: get-status with repositoryPath={}", repositoryPath);
        try {
            RepositoryStatus status = repositoryService.getStatus(Paths.get(repositoryPath));
            logger.info("Retrieved status for repository at {}", repositoryPath);
            return Map.of(
                "currentBranch", status.currentBranch(),
                "stagedFiles", status.stagedFiles(),
                "unstagedFiles", status.unstagedFiles(),
                "untrackedFiles", status.untrackedFiles(),
                "hasUncommittedChanges", status.hasUncommittedChanges()
            );
        } catch (Exception e) {
            logger.error("Failed to get status for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Get commit history from a repository with optional limit")
    public Map<String, Object> getHistory(String repositoryPath, Optional<Integer> limit) {
        int actualLimit = limit.orElse(10);
        logger.debug("MCP Tool invoked: get-history with repositoryPath={}, limit={}", repositoryPath, actualLimit);
        try {
            List<CommitInfo> commits = repositoryService.getHistory(Paths.get(repositoryPath), actualLimit);
            logger.info("Retrieved {} commits from repository at {}", commits.size(), repositoryPath);
            return Map.of(
                "commits", commits.stream().map(this::commitToMap).toList()
            );
        } catch (Exception e) {
            logger.error("Failed to get history for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Get detailed information about a specific commit")
    public Map<String, Object> getCommitDetails(String repositoryPath, String commitHash) {
        logger.debug("MCP Tool invoked: get-commit-details with repositoryPath={}, commitHash={}", repositoryPath, commitHash);
        try {
            CommitInfo commit = repositoryService.getCommitDetails(Paths.get(repositoryPath), commitHash);
            logger.info("Retrieved commit details for {} in repository at {}", commitHash, repositoryPath);
            return commitToMap(commit);
        } catch (Exception e) {
            logger.error("Failed to get commit details for {} in repository at {}", commitHash, repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Get the name of the currently checked-out branch")
    public Map<String, Object> getCurrentBranch(String repositoryPath) {
        logger.debug("MCP Tool invoked: get-current-branch with repositoryPath={}", repositoryPath);
        try {
            String branch = repositoryService.getCurrentBranch(Paths.get(repositoryPath));
            logger.info("Current branch for repository at {} is {}", repositoryPath, branch);
            return Map.of("currentBranch", branch);
        } catch (Exception e) {
            logger.error("Failed to get current branch for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    // Commit Operations

    @McpTool(description = "Stage files for commit")
    public Map<String, Object> stageFiles(String repositoryPath, List<String> filePaths) {
        logger.debug("MCP Tool invoked: stage-files with repositoryPath={}, files={}", repositoryPath, filePaths);
        try {
            commitService.stageFiles(Paths.get(repositoryPath), filePaths);
            logger.info("Staged {} files in repository at {}", filePaths.size(), repositoryPath);
            return Map.of(
                "success", true,
                "stagedFiles", filePaths
            );
        } catch (Exception e) {
            logger.error("Failed to stage files in repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Unstage files from the staging area")
    public Map<String, Object> unstageFiles(String repositoryPath, List<String> filePaths) {
        logger.debug("MCP Tool invoked: unstage-files with repositoryPath={}, files={}", repositoryPath, filePaths);
        try {
            commitService.unstageFiles(Paths.get(repositoryPath), filePaths);
            logger.info("Unstaged {} files in repository at {}", filePaths.size(), repositoryPath);
            return Map.of(
                "success", true,
                "unstagedFiles", filePaths
            );
        } catch (Exception e) {
            logger.error("Failed to unstage files in repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Create a new commit with staged changes")
    public Map<String, Object> createCommit(
            String repositoryPath,
            String message,
            Optional<String> authorName,
            Optional<String> authorEmail) {
        logger.debug("MCP Tool invoked: create-commit with repositoryPath={}, message={}", repositoryPath, message);
        try {
            AuthorInfo author = (authorName.isPresent() && authorEmail.isPresent())
                ? new AuthorInfo(authorName.get(), authorEmail.get())
                : null;
            
            CommitInfo commit = commitService.createCommit(Paths.get(repositoryPath), message, author);
            logger.info("Created commit {} in repository at {}", commit.shortHash(), repositoryPath);
            return commitToMap(commit);
        } catch (Exception e) {
            logger.error("Failed to create commit in repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Get diff for unstaged changes, staged changes, or between commits")
    public Map<String, Object> getDiff(
            String repositoryPath,
            String diffType,
            Optional<String> ref1,
            Optional<String> ref2) {
        logger.debug("MCP Tool invoked: get-diff with repositoryPath={}, diffType={}", repositoryPath, diffType);
        try {
            DiffType type = DiffType.valueOf(diffType.toUpperCase());
            String[] refs = (ref1.isPresent() && ref2.isPresent())
                ? new String[]{ref1.get(), ref2.get()}
                : new String[0];
            
            String diff = commitService.getDiff(Paths.get(repositoryPath), type, refs);
            logger.info("Retrieved {} diff for repository at {}", diffType, repositoryPath);
            return Map.of(
                "diffType", diffType,
                "diff", diff
            );
        } catch (Exception e) {
            logger.error("Failed to get diff for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Get file contents at a specific commit")
    public Map<String, Object> getFileContents(
            String repositoryPath,
            String commitHash,
            String filePath) {
        logger.debug("MCP Tool invoked: get-file-contents with repositoryPath={}, commitHash={}, filePath={}",
            repositoryPath, commitHash, filePath);
        try {
            String contents = commitService.getFileContents(Paths.get(repositoryPath), commitHash, filePath);
            logger.info("Retrieved file contents for {} at commit {} in repository at {}",
                filePath, commitHash, repositoryPath);
            return Map.of(
                "filePath", filePath,
                "commitHash", commitHash,
                "contents", contents
            );
        } catch (Exception e) {
            logger.error("Failed to get file contents for {} at commit {} in repository at {}",
                filePath, commitHash, repositoryPath, e);
            throw e;
        }
    }

    // Branch Operations

    @McpTool(description = "Create a new branch")
    public Map<String, Object> createBranch(String repositoryPath, String branchName) {
        logger.debug("MCP Tool invoked: create-branch with repositoryPath={}, branchName={}", repositoryPath, branchName);
        try {
            BranchInfo branch = branchService.createBranch(Paths.get(repositoryPath), branchName);
            logger.info("Created branch {} in repository at {}", branchName, repositoryPath);
            return Map.of(
                "name", branch.name(),
                "commitHash", branch.commitHash(),
                "isCurrent", branch.isCurrent()
            );
        } catch (Exception e) {
            logger.error("Failed to create branch {} in repository at {}", branchName, repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Switch to a different branch")
    public Map<String, Object> switchBranch(String repositoryPath, String branchName) {
        logger.debug("MCP Tool invoked: switch-branch with repositoryPath={}, branchName={}", repositoryPath, branchName);
        try {
            branchService.switchBranch(Paths.get(repositoryPath), branchName);
            logger.info("Switched to branch {} in repository at {}", branchName, repositoryPath);
            return Map.of(
                "success", true,
                "currentBranch", branchName
            );
        } catch (Exception e) {
            logger.error("Failed to switch to branch {} in repository at {}", branchName, repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "List all branches in the repository")
    public Map<String, Object> listBranches(String repositoryPath) {
        logger.debug("MCP Tool invoked: list-branches with repositoryPath={}", repositoryPath);
        try {
            List<BranchInfo> branches = branchService.listBranches(Paths.get(repositoryPath));
            logger.info("Retrieved {} branches from repository at {}", branches.size(), repositoryPath);
            return Map.of(
                "branches", branches.stream().map(b -> Map.of(
                    "name", b.name(),
                    "commitHash", b.commitHash(),
                    "isCurrent", b.isCurrent()
                )).toList()
            );
        } catch (Exception e) {
            logger.error("Failed to list branches for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Delete a branch")
    public Map<String, Object> deleteBranch(String repositoryPath, String branchName) {
        logger.debug("MCP Tool invoked: delete-branch with repositoryPath={}, branchName={}", repositoryPath, branchName);
        try {
            branchService.deleteBranch(Paths.get(repositoryPath), branchName);
            logger.info("Deleted branch {} from repository at {}", branchName, repositoryPath);
            return Map.of(
                "success", true,
                "deletedBranch", branchName
            );
        } catch (Exception e) {
            logger.error("Failed to delete branch {} from repository at {}", branchName, repositoryPath, e);
            throw e;
        }
    }

    // Remote Operations

    @McpTool(description = "Push commits to a remote repository")
    public Map<String, Object> push(
            String repositoryPath,
            String remote,
            String branch,
            Optional<String> username,
            Optional<String> password) {
        logger.debug("MCP Tool invoked: push with repositoryPath={}, remote={}, branch={}", repositoryPath, remote, branch);
        try {
            Credentials credentials = (username.isPresent() && password.isPresent())
                ? new Credentials(username.get(), password.get())
                : null;
            
            remoteService.push(Paths.get(repositoryPath), remote, branch, credentials);
            logger.info("Pushed to {}/{} from repository at {}", remote, branch, repositoryPath);
            return Map.of(
                "success", true,
                "remote", remote,
                "branch", branch
            );
        } catch (Exception e) {
            logger.error("Failed to push to {}/{} from repository at {}", remote, branch, repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Pull changes from a remote repository")
    public Map<String, Object> pull(
            String repositoryPath,
            String remote,
            String branch,
            Optional<String> username,
            Optional<String> password) {
        logger.debug("MCP Tool invoked: pull with repositoryPath={}, remote={}, branch={}", repositoryPath, remote, branch);
        try {
            Credentials credentials = (username.isPresent() && password.isPresent())
                ? new Credentials(username.get(), password.get())
                : null;
            
            remoteService.pull(Paths.get(repositoryPath), remote, branch, credentials);
            logger.info("Pulled from {}/{} to repository at {}", remote, branch, repositoryPath);
            return Map.of(
                "success", true,
                "remote", remote,
                "branch", branch
            );
        } catch (Exception e) {
            logger.error("Failed to pull from {}/{} to repository at {}", remote, branch, repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Fetch changes from a remote repository without merging")
    public Map<String, Object> fetch(
            String repositoryPath,
            String remote,
            Optional<String> username,
            Optional<String> password) {
        logger.debug("MCP Tool invoked: fetch with repositoryPath={}, remote={}", repositoryPath, remote);
        try {
            Credentials credentials = (username.isPresent() && password.isPresent())
                ? new Credentials(username.get(), password.get())
                : null;
            
            remoteService.fetch(Paths.get(repositoryPath), remote, credentials);
            logger.info("Fetched from {} to repository at {}", remote, repositoryPath);
            return Map.of(
                "success", true,
                "remote", remote
            );
        } catch (Exception e) {
            logger.error("Failed to fetch from {} to repository at {}", remote, repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "List all configured remote repositories")
    public Map<String, Object> listRemotes(String repositoryPath) {
        logger.debug("MCP Tool invoked: list-remotes with repositoryPath={}", repositoryPath);
        try {
            List<RemoteInfo> remotes = remoteService.listRemotes(Paths.get(repositoryPath));
            logger.info("Retrieved {} remotes from repository at {}", remotes.size(), repositoryPath);
            return Map.of(
                "remotes", remotes.stream().map(r -> Map.of(
                    "name", r.name(),
                    "url", r.url(),
                    "type", r.type().toString()
                )).toList()
            );
        } catch (Exception e) {
            logger.error("Failed to list remotes for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    @McpTool(description = "Add a new remote repository")
    public Map<String, Object> addRemote(String repositoryPath, String name, String url) {
        logger.debug("MCP Tool invoked: add-remote with repositoryPath={}, name={}, url={}", repositoryPath, name, url);
        try {
            remoteService.addRemote(Paths.get(repositoryPath), name, url);
            logger.info("Added remote {} with URL {} to repository at {}", name, url, repositoryPath);
            return Map.of(
                "success", true,
                "name", name,
                "url", url
            );
        } catch (Exception e) {
            logger.error("Failed to add remote {} to repository at {}", name, repositoryPath, e);
            throw e;
        }
    }

    // Commit Message Generation

    @McpTool(description = "Generate a standardized commit message based on staged changes and branch context")
    public Map<String, Object> generateCommitMessage(
            String repositoryPath,
            String summary,
            Optional<String> description) {
        logger.debug("MCP Tool invoked: generate-commit-message with repositoryPath={}, summary={}", repositoryPath, summary);
        try {
            String message = commitMessageService.generateCommitMessage(
                Paths.get(repositoryPath),
                summary,
                description.orElse("")
            );
            logger.info("Generated commit message for repository at {}", repositoryPath);
            return Map.of(
                "message", message
            );
        } catch (Exception e) {
            logger.error("Failed to generate commit message for repository at {}", repositoryPath, e);
            throw e;
        }
    }

    // Helper Methods

    private Map<String, Object> commitToMap(CommitInfo commit) {
        return Map.of(
            "hash", commit.hash(),
            "shortHash", commit.shortHash(),
            "message", commit.message(),
            "author", Map.of(
                "name", commit.author().name(),
                "email", commit.author().email()
            ),
            "timestamp", commit.timestamp().toString(),
            "changedFiles", commit.changedFiles(),
            "stats", Map.of(
                "insertions", commit.stats().insertions(),
                "deletions", commit.stats().deletions(),
                "filesChanged", commit.stats().filesChanged()
            )
        );
    }
}
