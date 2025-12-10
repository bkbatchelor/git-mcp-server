package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import io.sandboxdev.gitmcp.mcp.McpErrorHandler;
import io.sandboxdev.gitmcp.service.impl.*;
import net.jqwik.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for invalid parameter error handling.
 * Feature: git-mcp-server, Property 22: Invalid parameters return MCP error format
 * Validates: Requirements 7.4
 */
public class InvalidParameterErrorProperty {
    
    private final GitMcpToolProvider toolProvider;
    private final McpErrorHandler errorHandler;
    
    public InvalidParameterErrorProperty() {
        // Create services manually for testing
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        io.sandboxdev.gitmcp.jgit.RepositoryLockManager lockManager = new io.sandboxdev.gitmcp.jgit.RepositoryLockManager();
        
        GitRepositoryServiceImpl repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
        GitCommitServiceImpl commitService = new GitCommitServiceImpl(repositoryManager, commandExecutor, lockManager);
        GitBranchServiceImpl branchService = new GitBranchServiceImpl(repositoryManager, commandExecutor, lockManager);
        GitRemoteServiceImpl remoteService = new GitRemoteServiceImpl(repositoryManager, commandExecutor);
        CommitMessageGeneratorServiceImpl commitMessageService = new CommitMessageGeneratorServiceImpl(repositoryManager);
        
        this.toolProvider = new GitMcpToolProvider(
            repositoryService,
            commitService,
            branchService,
            remoteService,
            commitMessageService
        );
        
        this.errorHandler = new McpErrorHandler();
    }
    
    // Feature: git-mcp-server, Property 22: Invalid parameters return MCP error format
    @Property(tries = 100)
    void invalidParametersReturnMcpErrorFormat(@ForAll("invalidToolInvocation") InvalidInvocation invocation) {
        // Given an invalid tool invocation
        // When we invoke the tool with invalid parameters and catch the exception
        Map<String, Object> response = invocation.invoke(toolProvider, errorHandler);
        
        // Then the response should be in MCP error format
        assertThat(response)
            .as("Response should not be null")
            .isNotNull();
        
        // MCP error format requires an "error" field
        assertThat(response)
            .as("Response should contain 'error' field for invalid parameters")
            .containsKey("error");
        
        Object errorObj = response.get("error");
        assertThat(errorObj)
            .as("Error field should be a Map")
            .isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) errorObj;
        
        // MCP error format requires "code" and "message" fields
        assertThat(error)
            .as("Error should contain 'code' field")
            .containsKey("code");
        
        assertThat(error)
            .as("Error should contain 'message' field")
            .containsKey("message");
        
        assertThat(error.get("code"))
            .as("Error code should be a non-empty string")
            .isInstanceOf(String.class)
            .asString()
            .isNotEmpty();
        
        assertThat(error.get("message"))
            .as("Error message should be a non-empty string")
            .isInstanceOf(String.class)
            .asString()
            .isNotEmpty();
        
        // Optional "data" field may contain additional error details
        if (error.containsKey("data")) {
            assertThat(error.get("data"))
                .as("Error data should be a Map if present")
                .isInstanceOf(Map.class);
        }
    }
    
    @Provide
    Arbitrary<InvalidInvocation> invalidToolInvocation() {
        return Arbitraries.oneOf(
            nonExistentRepositoryPath(),
            invalidCommitHash(),
            invalidBranchName(),
            invalidDiffType()
        );
    }
    
    /**
     * Test with non-existent repository path
     */
    private Arbitrary<InvalidInvocation> nonExistentRepositoryPath() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(randomPath -> (InvalidInvocation) (provider, errorHandler) -> {
                try {
                    provider.getStatus("/nonexistent/path/" + randomPath);
                    // If no exception, this is not an error case
                    return Map.of("success", true);
                } catch (Exception e) {
                    // Convert exception to MCP error format using error handler
                    return errorHandler.formatError(e, "get-status");
                }
            });
    }
    
    /**
     * Test with invalid commit hash
     */
    private Arbitrary<InvalidInvocation> invalidCommitHash() {
        return Arbitraries.strings()
            .withCharRange('g', 'z')
            .ofLength(40)
            .map(invalidHash -> (InvalidInvocation) (provider, errorHandler) -> {
                try {
                    provider.getCommitDetails("/nonexistent/repo/path", invalidHash);
                    return Map.of("success", true);
                } catch (Exception e) {
                    return errorHandler.formatError(e, "get-commit-details");
                }
            });
    }
    
    /**
     * Test with invalid branch name (empty or with invalid characters)
     */
    private Arbitrary<InvalidInvocation> invalidBranchName() {
        return Arbitraries.of("..", "branch..name", "branch~name", "branch name")
            .map(invalidBranch -> (InvalidInvocation) (provider, errorHandler) -> {
                try {
                    provider.createBranch("/nonexistent/repo/path", invalidBranch);
                    return Map.of("success", true);
                } catch (Exception e) {
                    return errorHandler.formatError(e, "create-branch");
                }
            });
    }
    
    /**
     * Test with invalid diff type
     */
    private Arbitrary<InvalidInvocation> invalidDiffType() {
        return Arbitraries.of("INVALID", "invalid", "unknown", "123")
            .map(invalidType -> (InvalidInvocation) (provider, errorHandler) -> {
                try {
                    provider.getDiff("/nonexistent/repo/path", invalidType, 
                        java.util.Optional.empty(), java.util.Optional.empty());
                    return Map.of("success", true);
                } catch (Exception e) {
                    return errorHandler.formatError(e, "get-diff");
                }
            });
    }
    
    /**
     * Interface representing an invalid tool invocation.
     */
    @FunctionalInterface
    private interface InvalidInvocation {
        Map<String, Object> invoke(GitMcpToolProvider provider, McpErrorHandler errorHandler);
    }
}
