package io.sandboxdev.gitmcp.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.*;
import io.sandboxdev.gitmcp.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * MCP Resource Provider that exposes Git repository information as MCP resources.
 * 
 * <p>This class provides read-only access to Git repository information through
 * the MCP resource protocol. Resources are identified by URIs and provide
 * structured access to repository state, branch information, and commit history.</p>
 * 
 * <p>Available resources:</p>
 * <ul>
 *   <li>{@code git://repository/{path}/status} - Current repository status</li>
 *   <li>{@code git://repository/{path}/branches} - All branches in the repository</li>
 *   <li>{@code git://repository/{path}/history} - Recent commit history</li>
 *   <li>{@code git://repository/{path}/current-branch} - Currently checked-out branch</li>
 * </ul>
 * 
 * <p>All resources return JSON-formatted data that can be consumed by MCP clients
 * for read-only access to repository information. This complements the tool
 * provider by offering passive data access without triggering operations.</p>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class GitMcpResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GitMcpResourceProvider.class);

    private final GitRepositoryService repositoryService;
    private final GitBranchService branchService;
    private final ObjectMapper objectMapper;

    public GitMcpResourceProvider(
            GitRepositoryService repositoryService,
            GitBranchService branchService,
            ObjectMapper objectMapper) {
        this.repositoryService = repositoryService;
        this.branchService = branchService;
        this.objectMapper = objectMapper;
    }

    /**
     * Expose repository status as an MCP resource.
     * Resource URI: git://repository/{repositoryPath}/status
     */
    @McpResource(uri = "git://repository/{repositoryPath}/status", description = "Get the current status of a Git repository")
    public String getRepositoryStatus(String repositoryPath) {
        logger.debug("MCP Resource accessed: repository status for {}", repositoryPath);
        try {
            RepositoryStatus status = repositoryService.getStatus(Paths.get(repositoryPath));
            logger.info("Retrieved repository status resource for {}", repositoryPath);
            Map<String, Object> content = Map.of(
                "currentBranch", status.currentBranch(),
                "stagedFiles", status.stagedFiles(),
                "unstagedFiles", status.unstagedFiles(),
                "untrackedFiles", status.untrackedFiles(),
                "hasUncommittedChanges", status.hasUncommittedChanges()
            );
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize repository status for {}", repositoryPath, e);
            throw new RuntimeException("Failed to serialize repository status", e);
        } catch (Exception e) {
            logger.error("Failed to get repository status resource for {}", repositoryPath, e);
            throw e;
        }
    }

    /**
     * Expose branch information as an MCP resource.
     * Resource URI: git://repository/{repositoryPath}/branches
     */
    @McpResource(uri = "git://repository/{repositoryPath}/branches", description = "Get all branches in a Git repository")
    public String getRepositoryBranches(String repositoryPath) {
        logger.debug("MCP Resource accessed: branches for {}", repositoryPath);
        try {
            List<BranchInfo> branches = branchService.listBranches(Paths.get(repositoryPath));
            logger.info("Retrieved branches resource for {}", repositoryPath);
            Map<String, Object> content = Map.of(
                "branches", branches.stream().map(b -> Map.of(
                    "name", b.name(),
                    "commitHash", b.commitHash(),
                    "isCurrent", b.isCurrent()
                )).toList()
            );
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize branches for {}", repositoryPath, e);
            throw new RuntimeException("Failed to serialize branches", e);
        } catch (Exception e) {
            logger.error("Failed to get branches resource for {}", repositoryPath, e);
            throw e;
        }
    }

    /**
     * Expose commit history as an MCP resource.
     * Resource URI: git://repository/{repositoryPath}/history
     */
    @McpResource(uri = "git://repository/{repositoryPath}/history", description = "Get commit history from a Git repository")
    public String getRepositoryHistory(String repositoryPath) {
        logger.debug("MCP Resource accessed: history for {}", repositoryPath);
        try {
            // Default to 10 commits for resource access
            List<CommitInfo> commits = repositoryService.getHistory(Paths.get(repositoryPath), 10);
            logger.info("Retrieved history resource for {}", repositoryPath);
            Map<String, Object> content = Map.of(
                "commits", commits.stream().map(this::commitToMap).toList()
            );
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize history for {}", repositoryPath, e);
            throw new RuntimeException("Failed to serialize history", e);
        } catch (Exception e) {
            logger.error("Failed to get history resource for {}", repositoryPath, e);
            throw e;
        }
    }

    /**
     * Expose current branch as an MCP resource.
     * Resource URI: git://repository/{repositoryPath}/current-branch
     */
    @McpResource(uri = "git://repository/{repositoryPath}/current-branch", description = "Get the currently checked-out branch")
    public String getCurrentBranch(String repositoryPath) {
        logger.debug("MCP Resource accessed: current branch for {}", repositoryPath);
        try {
            String branch = repositoryService.getCurrentBranch(Paths.get(repositoryPath));
            logger.info("Retrieved current branch resource for {}", repositoryPath);
            return branch;
        } catch (Exception e) {
            logger.error("Failed to get current branch resource for {}", repositoryPath, e);
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
