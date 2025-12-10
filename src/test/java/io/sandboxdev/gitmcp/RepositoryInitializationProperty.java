package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.exception.GitMcpException;
import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.RepositoryInfo;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for repository initialization.
 * Feature: git-mcp-server, Property 1: Repository initialization creates valid Git repository
 * Validates: Requirements 1.1
 */
public class RepositoryInitializationProperty {
    
    private final GitRepositoryService repositoryService;
    
    public RepositoryInitializationProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 1: Repository initialization creates valid Git repository
    @Property(tries = 100)
    void repositoryInitializationCreatesValidGitRepository(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // When initializing a repository at a valid path
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            
            // Then the repository should be created successfully
            assertThat(repoInfo).isNotNull();
            assertThat(repoInfo.path()).isEqualTo(repositoryPath);
            
            // And the .git directory should exist
            Path gitDir = repositoryPath.resolve(".git");
            assertThat(gitDir).exists();
            assertThat(gitDir).isDirectory();
            
            // And the .git directory should contain proper Git configuration files
            assertThat(gitDir.resolve("config")).exists();
            assertThat(gitDir.resolve("HEAD")).exists();
            assertThat(gitDir.resolve("objects")).exists();
            assertThat(gitDir.resolve("refs")).exists();
            
            // And we should be able to open it as a valid Git repository
            File gitDirFile = gitDir.toFile();
            try {
                Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDirFile)
                    .build();
                
                assertThat(repository).isNotNull();
                assertThat(repository.getDirectory()).isEqualTo(gitDirFile);
                
                // Verify the repository has a valid object database
                assertThat(repository.getObjectDatabase()).isNotNull();
                
                // Verify the repository has a valid ref database
                assertThat(repository.getRefDatabase()).isNotNull();
                
                repository.close();
            } catch (IOException e) {
                fail("Failed to open initialized repository: " + e.getMessage());
            }
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 2: Duplicate initialization is rejected
    @Property(tries = 100)
    void duplicateInitializationIsRejected(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is already initialized at the path
            RepositoryInfo firstInit = repositoryService.initRepository(repositoryPath);
            assertThat(firstInit).isNotNull();
            
            // Store the original state of the repository
            Path gitDir = repositoryPath.resolve(".git");
            assertThat(gitDir).exists();
            long originalConfigSize = Files.size(gitDir.resolve("config"));
            
            // When attempting to initialize again at the same path
            // Then it should throw a GitMcpException and leave the existing repository unchanged
            assertThatThrownBy(() -> repositoryService.initRepository(repositoryPath))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("already exists");
            
            // And the existing repository should remain unchanged
            assertThat(gitDir).exists();
            assertThat(gitDir).isDirectory();
            assertThat(gitDir.resolve("config")).exists();
            assertThat(gitDir.resolve("HEAD")).exists();
            assertThat(gitDir.resolve("objects")).exists();
            assertThat(gitDir.resolve("refs")).exists();
            
            // Verify the config file size hasn't changed (indicating no modification)
            long currentConfigSize = Files.size(gitDir.resolve("config"));
            assertThat(currentConfigSize).isEqualTo(originalConfigSize);
            
            // Verify we can still open the repository normally
            File gitDirFile = gitDir.toFile();
            try {
                Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDirFile)
                    .build();
                
                assertThat(repository).isNotNull();
                repository.close();
            } catch (IOException e) {
                fail("Original repository was corrupted after duplicate initialization attempt: " + e.getMessage());
            }
            
        } catch (IOException e) {
            fail("Failed to check file sizes: " + e.getMessage());
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    @Provide
    Arbitrary<Path> validRepositoryPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(randomName -> {
                try {
                    return Files.createTempDirectory("git-test-" + randomName + "-");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            });
    }
    
    private void cleanupRepository(Path repositoryPath) {
        try {
            if (Files.exists(repositoryPath)) {
                Files.walk(repositoryPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
}
