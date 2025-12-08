package io.sandboxdev.gitmcp.jgit;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages JGit Repository instances with caching and lifecycle management.
 * Thread-safe implementation for concurrent repository access.
 */
@Component
public class JGitRepositoryManager {
    private static final Logger logger = LoggerFactory.getLogger(JGitRepositoryManager.class);
    
    private final Map<Path, CachedRepository> repositoryCache = new ConcurrentHashMap<>();
    private final int maxCachedRepositories;
    private final Duration cacheTimeout;
    
    public JGitRepositoryManager() {
        this.maxCachedRepositories = 10;
        this.cacheTimeout = Duration.ofMinutes(30);
    }
    
    /**
     * Opens or retrieves a cached repository.
     */
    public synchronized Repository openRepository(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        
        // Check cache first
        CachedRepository cached = repositoryCache.get(normalizedPath);
        if (cached != null && !cached.isExpired()) {
            cached.updateLastAccess();
            logger.debug("Retrieved repository from cache: {}", normalizedPath);
            return cached.repository;
        }
        
        // Remove expired entry if exists
        if (cached != null) {
            closeRepository(cached.repository);
            repositoryCache.remove(normalizedPath);
        }
        
        // Open new repository
        try {
            File gitDir = findGitDirectory(normalizedPath.toFile());
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build();
            
            // Add to cache
            evictIfNecessary();
            repositoryCache.put(normalizedPath, new CachedRepository(repository));
            
            logger.debug("Opened repository: {}", normalizedPath);
            return repository;
            
        } catch (IOException e) {
            logger.error("Failed to open repository at {}", normalizedPath, e);
            throw new GitMcpException(
                ErrorCode.REPOSITORY_NOT_FOUND,
                "Failed to open repository at: " + normalizedPath,
                e
            );
        }
    }
    
    /**
     * Closes a repository and removes it from cache.
     */
    public synchronized void closeRepository(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        CachedRepository cached = repositoryCache.remove(normalizedPath);
        
        if (cached != null) {
            closeRepository(cached.repository);
            logger.debug("Closed and removed repository from cache: {}", normalizedPath);
        }
    }
    
    /**
     * Closes all cached repositories.
     */
    public synchronized void closeAll() {
        logger.info("Closing all cached repositories ({})", repositoryCache.size());
        
        repositoryCache.values().forEach(cached -> closeRepository(cached.repository));
        repositoryCache.clear();
    }
    
    /**
     * Finds the .git directory for a given path.
     */
    private File findGitDirectory(File path) throws IOException {
        if (path.getName().equals(".git") && path.isDirectory()) {
            return path;
        }
        
        File gitDir = new File(path, ".git");
        if (gitDir.exists() && gitDir.isDirectory()) {
            return gitDir;
        }
        
        // Check if the path itself is a git directory (bare repository)
        if (new File(path, "HEAD").exists() && new File(path, "objects").exists()) {
            return path;
        }
        
        throw new IOException("Not a git repository: " + path);
    }
    
    /**
     * Closes a repository instance.
     */
    private void closeRepository(Repository repository) {
        if (repository != null) {
            repository.close();
        }
    }
    
    /**
     * Evicts oldest entry if cache is full.
     */
    private void evictIfNecessary() {
        if (repositoryCache.size() >= maxCachedRepositories) {
            // Find oldest entry
            Path oldestPath = null;
            Instant oldestAccess = Instant.now();
            
            for (Map.Entry<Path, CachedRepository> entry : repositoryCache.entrySet()) {
                if (entry.getValue().lastAccess.isBefore(oldestAccess)) {
                    oldestAccess = entry.getValue().lastAccess;
                    oldestPath = entry.getKey();
                }
            }
            
            if (oldestPath != null) {
                CachedRepository removed = repositoryCache.remove(oldestPath);
                closeRepository(removed.repository);
                logger.debug("Evicted repository from cache: {}", oldestPath);
            }
        }
    }
    
    /**
     * Cached repository with access tracking.
     */
    private class CachedRepository {
        final Repository repository;
        Instant lastAccess;
        
        CachedRepository(Repository repository) {
            this.repository = repository;
            this.lastAccess = Instant.now();
        }
        
        void updateLastAccess() {
            this.lastAccess = Instant.now();
        }
        
        boolean isExpired() {
            return Duration.between(lastAccess, Instant.now()).compareTo(cacheTimeout) > 0;
        }
    }
}
