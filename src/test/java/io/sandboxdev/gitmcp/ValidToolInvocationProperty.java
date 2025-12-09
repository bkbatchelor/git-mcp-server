package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import io.sandboxdev.gitmcp.service.impl.*;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for valid tool invocation responses.
 */
public class ValidToolInvocationProperty {
    
    private final GitMcpToolProvider toolProvider;
    
    public ValidToolInvocationProperty() {
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
    }
    
    // Feature: git-mcp-server, Property 21: Valid tool invocations return MCP-formatted responses
    @Property(tries = 100)
    void validToolInvocationsReturnMcpFormattedResponses(@ForAll("validToolInvocation") ToolInvocation invocation) {
        // Given a valid tool invocation
        // When we invoke the tool
        Map<String, Object> response = invocation.invoke(toolProvider);
        
        // Then the response should be MCP-formatted
        assertThat(response)
            .as("Response should not be null")
            .isNotNull();
        
        assertThat(response)
            .as("Response should be a non-empty map")
            .isNotEmpty();
        
        // MCP responses should contain either result data or error information
        // For successful operations, we expect specific fields based on the operation
        // All responses should be valid Map<String, Object> structures
        assertThat(response)
            .as("Response should contain valid MCP fields")
            .isInstanceOf(Map.class);
        
        // Verify all values are serializable (basic MCP requirement)
        response.forEach((key, value) -> {
            assertThat(key)
                .as("All keys should be non-null strings")
                .isNotNull()
                .isInstanceOf(String.class);
            
            assertThat(value)
                .as("All values should be serializable types")
                .satisfiesAnyOf(
                    v -> assertThat(v).isInstanceOf(String.class),
                    v -> assertThat(v).isInstanceOf(Number.class),
                    v -> assertThat(v).isInstanceOf(Boolean.class),
                    v -> assertThat(v).isInstanceOf(List.class),
                    v -> assertThat(v).isInstanceOf(Map.class)
                );
        });
        
        // Cleanup if needed
        invocation.cleanup();
    }
    
    @Provide
    Arbitrary<ToolInvocation> validToolInvocation() {
        return Arbitraries.oneOf(
            initRepositoryInvocation(),
            getStatusInvocation(),
            getCurrentBranchInvocation(),
            stageFilesInvocation(),
            listBranchesInvocation(),
            listRemotesInvocation()
        );
    }
    
    private Arbitrary<ToolInvocation> initRepositoryInvocation() {
        return Arbitraries.just(new ToolInvocation() {
            private Path tempDir;
            
            @Override
            public Map<String, Object> invoke(GitMcpToolProvider provider) {
                try {
                    tempDir = Files.createTempDirectory("git-test-init-");
                    return provider.initRepository(tempDir.toString());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            }
            
            @Override
            public void cleanup() {
                cleanupDirectory(tempDir);
            }
        });
    }
    
    private Arbitrary<ToolInvocation> getStatusInvocation() {
        return Arbitraries.just(new ToolInvocation() {
            private Path tempDir;
            
            @Override
            public Map<String, Object> invoke(GitMcpToolProvider provider) {
                try {
                    tempDir = Files.createTempDirectory("git-test-status-");
                    Git.init().setDirectory(tempDir.toFile()).call();
                    return provider.getStatus(tempDir.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create test repository", e);
                }
            }
            
            @Override
            public void cleanup() {
                cleanupDirectory(tempDir);
            }
        });
    }
    
    private Arbitrary<ToolInvocation> getCurrentBranchInvocation() {
        return Arbitraries.just(new ToolInvocation() {
            private Path tempDir;
            
            @Override
            public Map<String, Object> invoke(GitMcpToolProvider provider) {
                try {
                    tempDir = Files.createTempDirectory("git-test-branch-");
                    Git git = Git.init().setDirectory(tempDir.toFile()).call();
                    
                    // Create initial commit so we have a branch
                    Path file = tempDir.resolve("initial.txt");
                    Files.writeString(file, "initial");
                    git.add().addFilepattern("initial.txt").call();
                    git.commit().setMessage("Initial commit").call();
                    
                    return provider.getCurrentBranch(tempDir.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create test repository", e);
                }
            }
            
            @Override
            public void cleanup() {
                cleanupDirectory(tempDir);
            }
        });
    }
    
    private Arbitrary<ToolInvocation> stageFilesInvocation() {
        return Arbitraries.just(new ToolInvocation() {
            private Path tempDir;
            
            @Override
            public Map<String, Object> invoke(GitMcpToolProvider provider) {
                try {
                    tempDir = Files.createTempDirectory("git-test-stage-");
                    Git.init().setDirectory(tempDir.toFile()).call();
                    
                    // Create a file to stage
                    Path file = tempDir.resolve("test.txt");
                    Files.writeString(file, "test content");
                    
                    return provider.stageFiles(tempDir.toString(), List.of("test.txt"));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create test repository", e);
                }
            }
            
            @Override
            public void cleanup() {
                cleanupDirectory(tempDir);
            }
        });
    }
    
    private Arbitrary<ToolInvocation> listBranchesInvocation() {
        return Arbitraries.just(new ToolInvocation() {
            private Path tempDir;
            
            @Override
            public Map<String, Object> invoke(GitMcpToolProvider provider) {
                try {
                    tempDir = Files.createTempDirectory("git-test-branches-");
                    Git git = Git.init().setDirectory(tempDir.toFile()).call();
                    
                    // Create initial commit so we have a branch
                    Path file = tempDir.resolve("initial.txt");
                    Files.writeString(file, "initial");
                    git.add().addFilepattern("initial.txt").call();
                    git.commit().setMessage("Initial commit").call();
                    
                    return provider.listBranches(tempDir.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create test repository", e);
                }
            }
            
            @Override
            public void cleanup() {
                cleanupDirectory(tempDir);
            }
        });
    }
    
    private Arbitrary<ToolInvocation> listRemotesInvocation() {
        return Arbitraries.just(new ToolInvocation() {
            private Path tempDir;
            
            @Override
            public Map<String, Object> invoke(GitMcpToolProvider provider) {
                try {
                    tempDir = Files.createTempDirectory("git-test-remotes-");
                    Git.init().setDirectory(tempDir.toFile()).call();
                    
                    return provider.listRemotes(tempDir.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create test repository", e);
                }
            }
            
            @Override
            public void cleanup() {
                cleanupDirectory(tempDir);
            }
        });
    }
    
    private void cleanupDirectory(Path directory) {
        if (directory != null && Files.exists(directory)) {
            try {
                Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    /**
     * Interface representing a tool invocation that can be executed and cleaned up.
     */
    private interface ToolInvocation {
        Map<String, Object> invoke(GitMcpToolProvider provider);
        void cleanup();
    }
}
