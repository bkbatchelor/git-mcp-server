package io.sandboxdev.gitmcp.jgit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages repository-level locks for concurrent access control.
 * Provides read-write locks to ensure repository integrity during concurrent operations.
 */
@Component
public class RepositoryLockManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryLockManager.class);
    
    private final ConcurrentHashMap<Path, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();
    
    /**
     * Acquires a read lock for the specified repository.
     * Multiple threads can hold read locks simultaneously.
     * 
     * @param repositoryPath Path to the repository
     */
    public void acquireReadLock(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        ReentrantReadWriteLock lock = locks.computeIfAbsent(normalizedPath, k -> new ReentrantReadWriteLock(true));
        
        logger.debug("Acquiring read lock for repository: {}", normalizedPath);
        lock.readLock().lock();
        logger.debug("Read lock acquired for repository: {}", normalizedPath);
    }
    
    /**
     * Releases a read lock for the specified repository.
     * 
     * @param repositoryPath Path to the repository
     */
    public void releaseReadLock(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        ReentrantReadWriteLock lock = locks.get(normalizedPath);
        
        if (lock != null) {
            lock.readLock().unlock();
            logger.debug("Read lock released for repository: {}", normalizedPath);
            
            // Clean up lock if no threads are waiting
            if (!lock.hasQueuedThreads() && lock.getReadLockCount() == 0 && !lock.isWriteLocked()) {
                locks.remove(normalizedPath);
                logger.debug("Removed unused lock for repository: {}", normalizedPath);
            }
        }
    }
    
    /**
     * Acquires a write lock for the specified repository.
     * Only one thread can hold a write lock, and no read locks can be held.
     * 
     * @param repositoryPath Path to the repository
     */
    public void acquireWriteLock(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        ReentrantReadWriteLock lock = locks.computeIfAbsent(normalizedPath, k -> new ReentrantReadWriteLock(true));
        
        logger.debug("Acquiring write lock for repository: {}", normalizedPath);
        lock.writeLock().lock();
        logger.debug("Write lock acquired for repository: {}", normalizedPath);
    }
    
    /**
     * Releases a write lock for the specified repository.
     * 
     * @param repositoryPath Path to the repository
     */
    public void releaseWriteLock(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        ReentrantReadWriteLock lock = locks.get(normalizedPath);
        
        if (lock != null) {
            lock.writeLock().unlock();
            logger.debug("Write lock released for repository: {}", normalizedPath);
            
            // Clean up lock if no threads are waiting
            if (!lock.hasQueuedThreads() && lock.getReadLockCount() == 0 && !lock.isWriteLocked()) {
                locks.remove(normalizedPath);
                logger.debug("Removed unused lock for repository: {}", normalizedPath);
            }
        }
    }
    
    /**
     * Executes a read operation with automatic lock management.
     * 
     * @param repositoryPath Path to the repository
     * @param operation Operation to execute
     * @param <T> Return type
     * @return Result of the operation
     */
    public <T> T executeWithReadLock(Path repositoryPath, RepositoryOperation<T> operation) {
        acquireReadLock(repositoryPath);
        try {
            return operation.execute();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Operation failed", e);
        } finally {
            releaseReadLock(repositoryPath);
        }
    }
    
    /**
     * Executes a write operation with automatic lock management.
     * 
     * @param repositoryPath Path to the repository
     * @param operation Operation to execute
     * @param <T> Return type
     * @return Result of the operation
     */
    public <T> T executeWithWriteLock(Path repositoryPath, RepositoryOperation<T> operation) {
        acquireWriteLock(repositoryPath);
        try {
            return operation.execute();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Operation failed", e);
        } finally {
            releaseWriteLock(repositoryPath);
        }
    }
    
    /**
     * Executes a void write operation with automatic lock management.
     * 
     * @param repositoryPath Path to the repository
     * @param operation Operation to execute
     */
    public void executeWithWriteLock(Path repositoryPath, VoidRepositoryOperation operation) {
        acquireWriteLock(repositoryPath);
        try {
            operation.execute();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Operation failed", e);
        } finally {
            releaseWriteLock(repositoryPath);
        }
    }
    
    /**
     * Functional interface for repository operations that return a value.
     */
    @FunctionalInterface
    public interface RepositoryOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Functional interface for repository operations that don't return a value.
     */
    @FunctionalInterface
    public interface VoidRepositoryOperation {
        void execute() throws Exception;
    }
}
