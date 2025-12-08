package io.sandboxdev.gitmcp.jgit;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import org.eclipse.jgit.api.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Wraps JGit API calls and translates exceptions to GitMcpException.
 */
@Component
public class JGitCommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JGitCommandExecutor.class);
    
    /**
     * Translates JGit exceptions to GitMcpException.
     */
    public GitMcpException translateException(Exception e, String operation) {
        logger.error("Git operation failed: {}", operation, e);
        
        if (e instanceof RefNotFoundException) {
            return new GitMcpException(
                ErrorCode.BRANCH_NOT_FOUND,
                "Branch or reference not found: " + e.getMessage(),
                e
            );
        }
        
        if (e instanceof InvalidRemoteException || e instanceof TransportException) {
            return new GitMcpException(
                ErrorCode.NETWORK_ERROR,
                "Network error during Git operation: " + e.getMessage(),
                e
            );
        }
        
        if (e instanceof WrongRepositoryStateException) {
            return new GitMcpException(
                ErrorCode.INVALID_REPOSITORY_STATE,
                "Invalid repository state: " + e.getMessage(),
                e
            );
        }
        
        if (e instanceof CheckoutConflictException) {
            return new GitMcpException(
                ErrorCode.UNCOMMITTED_CHANGES,
                "Uncommitted changes prevent operation: " + e.getMessage(),
                e
            );
        }
        
        if (e instanceof NoHeadException) {
            return new GitMcpException(
                ErrorCode.INVALID_REPOSITORY_STATE,
                "Repository has no HEAD: " + e.getMessage(),
                e
            );
        }
        
        if (e instanceof ConcurrentRefUpdateException) {
            return new GitMcpException(
                ErrorCode.CONCURRENT_MODIFICATION,
                "Concurrent modification detected: " + e.getMessage(),
                e
            );
        }
        
        if (e instanceof IOException) {
            return new GitMcpException(
                ErrorCode.OPERATION_FAILED,
                "I/O error during Git operation: " + e.getMessage(),
                e
            );
        }
        
        // Generic Git API exception
        if (e instanceof GitAPIException) {
            return new GitMcpException(
                ErrorCode.OPERATION_FAILED,
                "Git operation failed: " + e.getMessage(),
                e
            );
        }
        
        // Unknown exception
        return new GitMcpException(
            ErrorCode.OPERATION_FAILED,
            "Unexpected error during " + operation + ": " + e.getMessage(),
            e,
            Map.of("operation", operation)
        );
    }
}
