package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.Credentials;
import io.sandboxdev.gitmcp.model.RemoteInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for Git remote operations.
 */
public interface GitRemoteService {
    
    /**
     * Pushes commits to a remote repository.
     */
    void push(Path repositoryPath, String remote, String branch, Credentials credentials);
    
    /**
     * Pulls changes from a remote repository.
     */
    void pull(Path repositoryPath, String remote, String branch, Credentials credentials);
    
    /**
     * Fetches changes from a remote repository without merging.
     */
    void fetch(Path repositoryPath, String remote, Credentials credentials);
    
    /**
     * Lists all configured remotes.
     */
    List<RemoteInfo> listRemotes(Path repositoryPath);
    
    /**
     * Adds a new remote.
     */
    void addRemote(Path repositoryPath, String name, String url);
}
