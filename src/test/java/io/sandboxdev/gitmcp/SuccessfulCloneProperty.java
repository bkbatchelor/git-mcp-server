package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.Credentials;
import io.sandboxdev.gitmcp.model.RepositoryInfo;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for successful clone operations.
 * Feature: git-mcp-server, Property 3: Successful clone returns repository information
 * Validates: Requirements 1.5
 */
public class SuccessfulCloneProperty {
    
    private final GitRepositoryService repositoryService;
    
    public SuccessfulCloneProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 3: Successful clone returns repository information
    @Property(tries = 100)
    void successfulCloneReturnsRepositoryInformation(@ForAll("validRepositoryPath") Path sourcePath,
                                                   @ForAll("validRepositoryPath") Path targetPath) {
        try {
            // Given a source repository exists
            RepositoryInfo sourceRepo = repositoryService.initRepository(sourcePath);
            assertThat(sourceRepo).isNotNull();
            
            // Create an initial commit in the source repository to make it clonable
            createInitialCommit(sourcePath);
            
            // When cloning the repository
            String sourceUrl = sourcePath.toUri().toString();
            RepositoryInfo clonedRepo = repositoryService.cloneRepository(sourceUrl, targetPath, null);
            
            // Then the clone operation should return repository information
            assertThat(clonedRepo).isNotNull();
            assertThat(clonedRepo.path()).isEqualTo(targetPath);
            assertThat(clonedRepo.defaultBranch()).isNotNull();
            assertThat(clonedRepo.defaultBranch()).isNotEmpty();
            
            // And the target path should contain a valid Git repository
            Path gitDir = targetPath.resolve(".git");
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
                
                // Verify the default branch matches what was returned
                String actualBranch = repository.getBranch();
                assertThat(actualBranch).isEqualTo(clonedRepo.defaultBranch());
                
                repository.close();
            } catch (IOException e) {
                fail("Failed to open cloned repository: " + e.getMessage());
            }
            
        } finally {
            // Cleanup
            cleanupRepository(sourcePath);
            cleanupRepository(targetPath);
        }
    }
    
    // Feature: git-mcp-server, Property 3: Successful clone returns repository information
    @Property(tries = 50)
    void successfulCloneWithCredentialsReturnsRepositoryInformation(@ForAll("validRepositoryPath") Path sourcePath,
                                                                  @ForAll("validRepositoryPath") Path targetPath,
                                                                  @ForAll("validCredentials") Credentials credentials) {
        try {
            // Given a source repository exists
            RepositoryInfo sourceRepo = repositoryService.initRepository(sourcePath);
            assertThat(sourceRepo).isNotNull();
            
            // Create an initial commit in the source repository to make it clonable
            createInitialCommit(sourcePath);
            
            // When cloning the repository with credentials (for local clone, credentials are ignored but should not cause errors)
            String sourceUrl = sourcePath.toUri().toString();
            RepositoryInfo clonedRepo = repositoryService.cloneRepository(sourceUrl, targetPath, credentials);
            
            // Then the clone operation should return repository information
            assertThat(clonedRepo).isNotNull();
            assertThat(clonedRepo.path()).isEqualTo(targetPath);
            assertThat(clonedRepo.defaultBranch()).isNotNull();
            assertThat(clonedRepo.defaultBranch()).isNotEmpty();
            
            // And the target path should contain a valid Git repository
            Path gitDir = targetPath.resolve(".git");
            assertThat(gitDir).exists();
            assertThat(gitDir).isDirectory();
            
            // And the default branch should be valid
            try {
                Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir.toFile())
                    .build();
                
                String actualBranch = repository.getBranch();
                assertThat(actualBranch).isEqualTo(clonedRepo.defaultBranch());
                
                repository.close();
            } catch (IOException e) {
                fail("Failed to verify cloned repository: " + e.getMessage());
            }
            
        } finally {
            // Cleanup
            cleanupRepository(sourcePath);
            cleanupRepository(targetPath);
        }
    }
    
    // Feature: git-mcp-server, Property 3: Failed clone operations throw exceptions and do not return repository information
    @Property(tries = 30)
    void failedCloneOperationsThrowExceptions(@ForAll("validRepositoryPath") Path targetPath,
                                            @ForAll("invalidRepositoryUrl") String invalidUrl) {
        try {
            // When attempting to clone from an invalid URL
            // Then it should throw an exception and not return repository information
            assertThatThrownBy(() -> repositoryService.cloneRepository(invalidUrl, targetPath, null))
                .isInstanceOf(Exception.class);
            
            // And the target path should not contain a Git repository
            Path gitDir = targetPath.resolve(".git");
            assertThat(gitDir).doesNotExist();
            
        } finally {
            // Cleanup
            cleanupRepository(targetPath);
        }
    }
    
    // Feature: git-mcp-server, Property 3: Clone to existing repository fails and does not return repository information
    @Property(tries = 30)
    void cloneToExistingRepositoryFails(@ForAll("validRepositoryPath") Path sourcePath,
                                      @ForAll("validRepositoryPath") Path targetPath) {
        try {
            // Given a source repository exists
            RepositoryInfo sourceRepo = repositoryService.initRepository(sourcePath);
            assertThat(sourceRepo).isNotNull();
            createInitialCommit(sourcePath);
            
            // And a target repository already exists
            RepositoryInfo existingRepo = repositoryService.initRepository(targetPath);
            assertThat(existingRepo).isNotNull();
            
            // When attempting to clone to the existing repository path
            String sourceUrl = sourcePath.toUri().toString();
            
            // Then it should throw an exception and not return repository information
            assertThatThrownBy(() -> repositoryService.cloneRepository(sourceUrl, targetPath, null))
                .isInstanceOf(Exception.class);
            
            // And the existing repository should remain unchanged
            Path gitDir = targetPath.resolve(".git");
            assertThat(gitDir).exists();
            assertThat(gitDir).isDirectory();
            
            // Verify the original repository is still intact
            try {
                Repository repository = new FileRepositoryBuilder()
                    .setGitDir(gitDir.toFile())
                    .build();
                
                assertThat(repository).isNotNull();
                repository.close();
            } catch (IOException e) {
                fail("Original repository was corrupted: " + e.getMessage());
            }
            
        } finally {
            // Cleanup
            cleanupRepository(sourcePath);
            cleanupRepository(targetPath);
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
                    return Files.createTempDirectory("git-clone-test-" + randomName + "-");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            });
    }
    
    @Provide
    Arbitrary<Credentials> validCredentials() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(3)
            .ofMaxLength(15)
            .flatMap(username -> 
                Arbitraries.strings()
                    .alpha()
                    .numeric()
                    .ofMinLength(3)
                    .ofMaxLength(15)
                    .map(password -> new Credentials(username, password))
            );
    }
    
    @Provide
    Arbitrary<String> invalidRepositoryUrl() {
        return Arbitraries.oneOf(
            // Non-existent local paths
            Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(10)
                .ofMaxLength(30)
                .map(path -> "file:///nonexistent/" + path),
            // Invalid URLs
            Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(20)
                .map(path -> "invalid-protocol://" + path),
            // Non-existent remote URLs (these will fail due to network/DNS)
            Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(15)
                .map(path -> "https://nonexistent-domain-" + path + ".invalid/repo.git"),
            // Empty repository paths
            Arbitraries.just(""),
            // Malformed URLs
            Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(10)
                .map(path -> "://malformed-" + path)
        );
    }
    
    private void createInitialCommit(Path repositoryPath) {
        try {
            // Create a simple file to commit
            Path testFile = repositoryPath.resolve("README.md");
            Files.writeString(testFile, "# Test Repository\n\nThis is a test repository for cloning.");
            
            // Open the repository and create an initial commit
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.add().addFilepattern("README.md").call();
                git.commit()
                    .setMessage("Initial commit")
                    .setAuthor("Test User", "test@example.com")
                    .setCommitter("Test User", "test@example.com")
                    .call();
            }
            
            repository.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create initial commit", e);
        }
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