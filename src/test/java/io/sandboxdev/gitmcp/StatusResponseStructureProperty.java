package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.RepositoryInfo;
import io.sandboxdev.gitmcp.model.RepositoryStatus;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for status response structure.
 * Feature: git-mcp-server, Property 4: Status response contains all required fields
 * Validates: Requirements 2.1
 */
public class StatusResponseStructureProperty {
    
    private final GitRepositoryService repositoryService;
    
    public StatusResponseStructureProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 4: Status response contains all required fields
    @Property(tries = 100)
    void statusResponseContainsAllRequiredFields(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            
            // Create an initial commit to establish a proper repository state
            createInitialCommit(repositoryPath);
            
            // When getting the repository status
            RepositoryStatus status = repositoryService.getStatus(repositoryPath);
            
            // Then the status response should contain all required fields
            assertThat(status).isNotNull();
            
            // Current branch should be present and non-null
            assertThat(status.currentBranch()).isNotNull();
            assertThat(status.currentBranch()).isNotEmpty();
            
            // Staged files list should be present (can be empty but not null)
            assertThat(status.stagedFiles()).isNotNull();
            
            // Unstaged files list should be present (can be empty but not null)
            assertThat(status.unstagedFiles()).isNotNull();
            
            // Untracked files list should be present (can be empty but not null)
            assertThat(status.untrackedFiles()).isNotNull();
            
            // hasUncommittedChanges should be a valid boolean (not null)
            assertThat(status.hasUncommittedChanges()).isNotNull();
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 4: Status response contains all required fields with staged changes
    @Property(tries = 50)
    void statusResponseContainsAllRequiredFieldsWithStagedChanges(@ForAll("validRepositoryPath") Path repositoryPath,
                                                                @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And a new file is created and staged
            try {
                Path newFile = repositoryPath.resolve(fileName);
                Files.writeString(newFile, "New file content");
                
                Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repositoryPath.resolve(".git").toFile())
                    .build();
                
                try (Git git = new Git(repository)) {
                    git.add().addFilepattern(fileName).call();
                }
                repository.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to stage file", e);
            }
            
            // When getting the repository status
            RepositoryStatus status = repositoryService.getStatus(repositoryPath);
            
            // Then the status response should contain all required fields
            assertThat(status).isNotNull();
            
            // Current branch should be present and non-null
            assertThat(status.currentBranch()).isNotNull();
            assertThat(status.currentBranch()).isNotEmpty();
            
            // Staged files list should be present and contain the staged file
            assertThat(status.stagedFiles()).isNotNull();
            assertThat(status.stagedFiles()).contains(fileName);
            
            // Unstaged files list should be present (can be empty but not null)
            assertThat(status.unstagedFiles()).isNotNull();
            
            // Untracked files list should be present (can be empty but not null)
            assertThat(status.untrackedFiles()).isNotNull();
            
            // hasUncommittedChanges should be true since we have staged changes
            assertThat(status.hasUncommittedChanges()).isTrue();
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 4: Status response contains all required fields with untracked files
    @Property(tries = 50)
    void statusResponseContainsAllRequiredFieldsWithUntrackedFiles(@ForAll("validRepositoryPath") Path repositoryPath,
                                                                 @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And a new untracked file is created
            try {
                Path newFile = repositoryPath.resolve(fileName);
                Files.writeString(newFile, "Untracked file content");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create untracked file", e);
            }
            
            // When getting the repository status
            RepositoryStatus status = repositoryService.getStatus(repositoryPath);
            
            // Then the status response should contain all required fields
            assertThat(status).isNotNull();
            
            // Current branch should be present and non-null
            assertThat(status.currentBranch()).isNotNull();
            assertThat(status.currentBranch()).isNotEmpty();
            
            // Staged files list should be present (empty in this case)
            assertThat(status.stagedFiles()).isNotNull();
            assertThat(status.stagedFiles()).isEmpty();
            
            // Unstaged files list should be present (empty in this case)
            assertThat(status.unstagedFiles()).isNotNull();
            assertThat(status.unstagedFiles()).isEmpty();
            
            // Untracked files list should be present and contain the untracked file
            assertThat(status.untrackedFiles()).isNotNull();
            assertThat(status.untrackedFiles()).contains(fileName);
            
            // hasUncommittedChanges should be false since untracked files don't count as uncommitted changes
            assertThat(status.hasUncommittedChanges()).isFalse();
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 4: Status response contains all required fields with modified files
    @Property(tries = 50)
    void statusResponseContainsAllRequiredFieldsWithModifiedFiles(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And the existing file is modified
            try {
                Path existingFile = repositoryPath.resolve("README.md");
                Files.writeString(existingFile, "# Modified Test Repository\n\nThis content has been modified.");
            } catch (IOException e) {
                throw new RuntimeException("Failed to modify file", e);
            }
            
            // When getting the repository status
            RepositoryStatus status = repositoryService.getStatus(repositoryPath);
            
            // Then the status response should contain all required fields
            assertThat(status).isNotNull();
            
            // Current branch should be present and non-null
            assertThat(status.currentBranch()).isNotNull();
            assertThat(status.currentBranch()).isNotEmpty();
            
            // Staged files list should be present (empty in this case)
            assertThat(status.stagedFiles()).isNotNull();
            assertThat(status.stagedFiles()).isEmpty();
            
            // Unstaged files list should be present and contain the modified file
            assertThat(status.unstagedFiles()).isNotNull();
            assertThat(status.unstagedFiles()).contains("README.md");
            
            // Untracked files list should be present (empty in this case)
            assertThat(status.untrackedFiles()).isNotNull();
            assertThat(status.untrackedFiles()).isEmpty();
            
            // hasUncommittedChanges should be true since we have unstaged changes
            assertThat(status.hasUncommittedChanges()).isTrue();
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 4: Failed conditions - Invalid repository paths throw exceptions
    @Property(tries = 30)
    void statusRequestOnInvalidRepositoryThrowsException(@ForAll("invalidRepositoryPath") Path invalidPath) {
        // When requesting status from an invalid repository path
        // Then it should throw an exception and not return a status response
        assertThatThrownBy(() -> repositoryService.getStatus(invalidPath))
            .isInstanceOf(Exception.class);
        
        // Cleanup (if any directory was created)
        cleanupRepository(invalidPath);
    }
    
    // Feature: git-mcp-server, Property 4: Failed conditions - Non-existent paths throw exceptions
    @Property(tries = 30)
    void statusRequestOnNonExistentPathThrowsException(@ForAll("nonExistentPath") Path nonExistentPath) {
        // When requesting status from a non-existent path
        // Then it should throw an exception and not return a status response
        assertThatThrownBy(() -> repositoryService.getStatus(nonExistentPath))
            .isInstanceOf(Exception.class);
    }
    
    // Feature: git-mcp-server, Property 4: Failed conditions - Empty directories without .git throw exceptions
    @Property(tries = 30)
    void statusRequestOnEmptyDirectoryThrowsException(@ForAll("validRepositoryPath") Path emptyPath) {
        try {
            // Given an empty directory exists (no .git folder)
            Files.createDirectories(emptyPath);
            
            // When requesting status from the empty directory
            // Then it should throw an exception and not return a status response
            assertThatThrownBy(() -> repositoryService.getStatus(emptyPath))
                .isInstanceOf(Exception.class);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty directory", e);
        } finally {
            // Cleanup
            cleanupRepository(emptyPath);
        }
    }
    
    // Feature: git-mcp-server, Property 4: Failed conditions - Completely missing .git directory throws exceptions
    @Property(tries = 20)
    void statusRequestOnMissingGitDirectoryThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            
            // And the entire .git directory is removed
            Path gitDir = repositoryPath.resolve(".git");
            if (Files.exists(gitDir)) {
                Files.walk(gitDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore individual file deletion errors
                        }
                    });
            }
            
            // When requesting status from the directory without .git
            // Then it should throw an exception and not return a status response
            assertThatThrownBy(() -> repositoryService.getStatus(repositoryPath))
                .isInstanceOf(Exception.class);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove .git directory", e);
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 4: Failed conditions - File instead of directory throws exceptions
    @Property(tries = 20)
    void statusRequestOnFilePathThrowsException(@ForAll("validRepositoryPath") Path basePath) {
        try {
            // Given a file exists instead of a directory
            final Path filePath = basePath.resolve("not-a-directory.txt");
            Files.createDirectories(basePath);
            Files.writeString(filePath, "This is a file, not a directory");
            
            // When requesting status from the file path
            // Then it should throw an exception and not return a status response
            assertThatThrownBy(() -> repositoryService.getStatus(filePath))
                .isInstanceOf(Exception.class);
                
            // Cleanup the file
            Files.deleteIfExists(filePath);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test file", e);
        } finally {
            // Cleanup
            cleanupRepository(basePath);
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
                    return Files.createTempDirectory("git-status-test-" + randomName + "-");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            });
    }
    
    @Provide
    Arbitrary<String> validFileName() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(3)
            .ofMaxLength(15)
            .map(name -> name + ".txt");
    }
    
    @Provide
    Arbitrary<Path> invalidRepositoryPath() {
        return Arbitraries.oneOf(
            // Paths that look like repositories but aren't (fake .git file)
            Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(15)
                .map(name -> {
                    try {
                        Path tempDir = Files.createTempDirectory("git-fake-test-" + name + "-");
                        // Create a fake .git file instead of directory
                        Files.writeString(tempDir.resolve(".git"), "This is not a git directory");
                        return tempDir;
                    } catch (IOException e) {
                        return Path.of("/tmp/fake-git-" + name);
                    }
                }),
            // Very long paths that might cause issues
            Arbitraries.strings()
                .alpha()
                .ofMinLength(100)
                .ofMaxLength(200)
                .map(longName -> {
                    try {
                        return Files.createTempDirectory("git-long-test-").resolve(longName);
                    } catch (IOException e) {
                        return Path.of("/tmp/" + longName);
                    }
                }),
            // Paths with spaces and special characters (but valid for filesystem)
            Arbitraries.strings()
                .withChars(' ', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '=', '+')
                .ofMinLength(3)
                .ofMaxLength(10)
                .map(specialName -> {
                    try {
                        Path tempDir = Files.createTempDirectory("git-special-test-");
                        // Create a fake .git file with special characters in path
                        Path specialPath = tempDir.resolve(specialName);
                        Files.createDirectories(specialPath);
                        Files.writeString(specialPath.resolve(".git"), "fake git");
                        return specialPath;
                    } catch (IOException e) {
                        return Path.of("/tmp/special-" + specialName.replaceAll("[^a-zA-Z0-9]", "_"));
                    }
                })
        );
    }
    
    @Provide
    Arbitrary<Path> nonExistentPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(10)
            .ofMaxLength(30)
            .map(randomName -> Path.of("/tmp/non-existent-" + randomName + "-" + System.currentTimeMillis()));
    }
    
    private void createInitialCommit(Path repositoryPath) {
        try {
            // Create a simple file to commit
            Path testFile = repositoryPath.resolve("README.md");
            Files.writeString(testFile, "# Test Repository\n\nThis is a test repository for status testing.");
            
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