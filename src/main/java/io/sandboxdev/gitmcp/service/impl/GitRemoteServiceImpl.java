package io.sandboxdev.gitmcp.service.impl;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.Credentials;
import io.sandboxdev.gitmcp.model.RemoteInfo;
import io.sandboxdev.gitmcp.model.RemoteType;
import io.sandboxdev.gitmcp.service.GitRemoteService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class GitRemoteServiceImpl implements GitRemoteService {
    private static final Logger logger = LoggerFactory.getLogger(GitRemoteServiceImpl.class);
    
    private final JGitRepositoryManager repositoryManager;
    private final JGitCommandExecutor commandExecutor;
    
    public GitRemoteServiceImpl(JGitRepositoryManager repositoryManager,
                               JGitCommandExecutor commandExecutor) {
        this.repositoryManager = repositoryManager;
        this.commandExecutor = commandExecutor;
    }

    
    @Override
    public void push(Path repositoryPath, String remote, String branch, Credentials credentials) {
        logger.debug("Pushing to remote {} branch {} in {}", remote, branch, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                var pushCommand = git.push()
                    .setRemote(remote)
                    .add(branch);
                
                if (credentials != null && !credentials.isEmpty()) {
                    pushCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(
                            credentials.username(),
                            credentials.password()
                        )
                    );
                }
                
                pushCommand.call();
                logger.info("Pushed to remote {} branch {}", remote, branch);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "push");
        }
    }
    
    @Override
    public void pull(Path repositoryPath, String remote, String branch, Credentials credentials) {
        logger.debug("Pulling from remote {} branch {} in {}", remote, branch, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                var pullCommand = git.pull()
                    .setRemote(remote)
                    .setRemoteBranchName(branch);
                
                if (credentials != null && !credentials.isEmpty()) {
                    pullCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(
                            credentials.username(),
                            credentials.password()
                        )
                    );
                }
                
                pullCommand.call();
                logger.info("Pulled from remote {} branch {}", remote, branch);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "pull");
        }
    }

    
    @Override
    public void fetch(Path repositoryPath, String remote, Credentials credentials) {
        logger.debug("Fetching from remote {} in {}", remote, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                var fetchCommand = git.fetch()
                    .setRemote(remote);
                
                if (credentials != null && !credentials.isEmpty()) {
                    fetchCommand.setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(
                            credentials.username(),
                            credentials.password()
                        )
                    );
                }
                
                fetchCommand.call();
                logger.info("Fetched from remote {}", remote);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "fetch");
        }
    }
    
    @Override
    public List<RemoteInfo> listRemotes(Path repositoryPath) {
        logger.debug("Listing remotes in {}", repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            StoredConfig config = repository.getConfig();
            Set<String> remoteNames = config.getSubsections("remote");
            
            List<RemoteInfo> remotes = new ArrayList<>();
            
            for (String remoteName : remoteNames) {
                String url = config.getString("remote", remoteName, "url");
                if (url != null) {
                    remotes.add(new RemoteInfo(remoteName, url, RemoteType.BOTH));
                }
            }
            
            logger.debug("Found {} remotes", remotes.size());
            return remotes;
            
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "list remotes");
        }
    }

    
    @Override
    public void addRemote(Path repositoryPath, String name, String url) {
        logger.debug("Adding remote {} with URL {} in {}", name, url, repositoryPath);
        
        try {
            Repository repository = repositoryManager.openRepository(repositoryPath);
            try (Git git = new Git(repository)) {
                git.remoteAdd()
                    .setName(name)
                    .setUri(new URIish(url))
                    .call();
                
                logger.info("Added remote {} with URL {}", name, url);
            }
        } catch (Exception e) {
            throw commandExecutor.translateException(e, "add remote");
        }
    }
}
