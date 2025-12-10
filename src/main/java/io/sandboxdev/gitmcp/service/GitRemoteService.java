package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.Credentials;
import io.sandboxdev.gitmcp.model.RemoteInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Service interface for Git remote repository operations.
 * 
 * <p>This service handles all remote repository interactions including push, pull,
 * fetch operations, and remote configuration management. It supports authentication
 * through credentials and handles network-related errors gracefully.</p>
 * 
 * @author Git MCP Server
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GitRemoteService {
    
    /**
     * Pushes local commits to a remote repository.
     * 
     * <p>Uploads local commits from the specified branch to the remote repository.
     * This operation requires appropriate permissions and may require authentication
     * credentials for private repositories.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param remote the name of the remote repository (e.g., "origin")
     * @param branch the name of the branch to push
     * @param credentials optional credentials for authentication (can be null for public repos)
     * @throws GitMcpException if the repository or remote is not found, authentication fails, or push is rejected
     * @throws IllegalArgumentException if repositoryPath, remote, or branch is null/empty
     */
    void push(Path repositoryPath, String remote, String branch, Credentials credentials);
    
    /**
     * Pulls changes from a remote repository and merges them into the current branch.
     * 
     * <p>Fetches commits from the remote repository and merges them into the
     * current local branch. This operation may create merge commits if there
     * are conflicting changes.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param remote the name of the remote repository (e.g., "origin")
     * @param branch the name of the remote branch to pull from
     * @param credentials optional credentials for authentication (can be null for public repos)
     * @throws GitMcpException if the repository or remote is not found, authentication fails, or merge conflicts occur
     * @throws IllegalArgumentException if repositoryPath, remote, or branch is null/empty
     */
    void pull(Path repositoryPath, String remote, String branch, Credentials credentials);
    
    /**
     * Fetches changes from a remote repository without merging.
     * 
     * <p>Downloads commits and references from the remote repository without
     * modifying the working directory or current branch. This allows inspection
     * of remote changes before merging.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param remote the name of the remote repository (e.g., "origin")
     * @param credentials optional credentials for authentication (can be null for public repos)
     * @throws GitMcpException if the repository or remote is not found, or authentication fails
     * @throws IllegalArgumentException if repositoryPath or remote is null/empty
     */
    void fetch(Path repositoryPath, String remote, Credentials credentials);
    
    /**
     * Retrieves a list of all configured remote repositories.
     * 
     * <p>Returns information about all remote repositories configured for this
     * local repository, including their names, URLs, and types (fetch/push).</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @return list of {@link RemoteInfo} objects representing configured remotes
     * @throws GitMcpException if the repository is not found or remote listing fails
     * @throws IllegalArgumentException if repositoryPath is null
     */
    List<RemoteInfo> listRemotes(Path repositoryPath);
    
    /**
     * Adds a new remote repository configuration.
     * 
     * <p>Configures a new remote repository with the specified name and URL.
     * The remote can then be used for push, pull, and fetch operations.</p>
     * 
     * @param repositoryPath the path to the Git repository
     * @param name the name for the remote (e.g., "origin", "upstream")
     * @param url the URL of the remote repository (HTTP, HTTPS, or SSH)
     * @throws GitMcpException if the repository is not found, remote name already exists, or URL is invalid
     * @throws IllegalArgumentException if any parameter is null/empty
     */
    void addRemote(Path repositoryPath, String name, String url);
}
