package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.jgit.RepositoryLockManager;
import io.sandboxdev.gitmcp.model.RepositoryInfo;
import io.sandboxdev.gitmcp.model.RepositoryStatus;
import io.sandboxdev.gitmcp.service.GitCommitService;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitCommitServiceImpl;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for staging files functionality.
 * Feature: git-mcp-server, Property 8: Staging files adds them to staged list
 * Validates: Requirements 3.1
 */
public class StagingFilesProperty {
    
    private static final Logger logger = LoggerFactory.getLogger(StagingFilesProperty.class);
    
    private final GitRepositoryService repositoryService;
    private final GitCommitService commitService;
    
    public StagingFilesProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        RepositoryLockManager lockManager = new RepositoryLockManager();
        
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
        this.commitService = new GitCommitServiceImpl(repositoryManager, commandExecutor, lockManager);
    }
    
    // Feature: git-mcp-server, Property 8: Staging files adds them to staged list
    @Property(tries = 100)
    void stagingFilesAddsThemToStagedList(@ForAll("validRepositoryPath") Path repositoryPath,
                                        @ForAll("validFileNames") List<String> fileNames) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And files exist in the working directory
            for (String fileName : fileNames) {
                try {
                    Path filePath = repositoryPath.resolve(fileName);
                    Files.writeString(filePath, "Content for " + fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create file: " + fileName, e);
                }
            }
            
            // Get initial status to verify files are untracked
            RepositoryStatus initialStatus = repositoryService.getStatus(repositoryPath);
            assertThat(initialStatus.untrackedFiles()).containsAll(fileNames);
            assertThat(initialStatus.stagedFiles()).doesNotContainAnyElementsOf(fileNames);
            
            // When staging the files
            commitService.stageFiles(repositoryPath, fileNames);
            
            // Then the files should appear in the staged list
            RepositoryStatus statusAfterStaging = repositoryService.getStatus(repositoryPath);
            
            // All staged files should be in the staged list
            assertThat(statusAfterStaging.stagedFiles()).containsAll(fileNames);
            
            // Files should no longer be in the untracked list
            assertThat(statusAfterStaging.untrackedFiles()).doesNotContainAnyElementsOf(fileNames);
            
            // Repository should have uncommitted changes
            assertThat(statusAfterStaging.hasUncommittedChanges()).isTrue();
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Staging modified files adds them to staged list
    @Property(tries = 50)
    void stagingModifiedFilesAddsThemToStagedList(@ForAll("validRepositoryPath") Path repositoryPath,
                                                @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists with an initial commit containing a file
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            
            // Create and commit initial file
            Path filePath = repositoryPath.resolve(fileName);
            try {
                Files.writeString(filePath, "Initial content");
                
                Repository repository = new FileRepositoryBuilder()
                    .setGitDir(repositoryPath.resolve(".git").toFile())
                    .build();
                
                try (Git git = new Git(repository)) {
                    git.add().addFilepattern(fileName).call();
                    git.commit()
                        .setMessage("Initial commit with " + fileName)
                        .setAuthor("Test User", "test@example.com")
                        .setCommitter("Test User", "test@example.com")
                        .call();
                }
                repository.close();
                
                // And the file is modified
                Files.writeString(filePath, "Modified content for " + fileName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create and modify file: " + fileName, e);
            }
            
            // Verify file is in unstaged changes
            RepositoryStatus initialStatus = repositoryService.getStatus(repositoryPath);
            assertThat(initialStatus.unstagedFiles()).contains(fileName);
            assertThat(initialStatus.stagedFiles()).doesNotContain(fileName);
            
            // When staging the modified file
            commitService.stageFiles(repositoryPath, List.of(fileName));
            
            // Then the file should appear in the staged list
            RepositoryStatus statusAfterStaging = repositoryService.getStatus(repositoryPath);
            
            // File should be in the staged list
            assertThat(statusAfterStaging.stagedFiles()).contains(fileName);
            
            // File should no longer be in the unstaged list
            assertThat(statusAfterStaging.unstagedFiles()).doesNotContain(fileName);
            
            // Repository should have uncommitted changes
            assertThat(statusAfterStaging.hasUncommittedChanges()).isTrue();
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Staging empty file list has no effect
    @Property(tries = 30)
    void stagingEmptyFileListHasNoEffect(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // Get initial status
            RepositoryStatus initialStatus = repositoryService.getStatus(repositoryPath);
            
            // When staging an empty list of files
            commitService.stageFiles(repositoryPath, List.of());
            
            // Then the status should remain unchanged
            RepositoryStatus statusAfterStaging = repositoryService.getStatus(repositoryPath);
            
            assertThat(statusAfterStaging.stagedFiles()).isEqualTo(initialStatus.stagedFiles());
            assertThat(statusAfterStaging.unstagedFiles()).isEqualTo(initialStatus.unstagedFiles());
            assertThat(statusAfterStaging.untrackedFiles()).isEqualTo(initialStatus.untrackedFiles());
            assertThat(statusAfterStaging.hasUncommittedChanges()).isEqualTo(initialStatus.hasUncommittedChanges());
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Staging files multiple times is idempotent
    @Property(tries = 50)
    void stagingFilesMultipleTimesIsIdempotent(@ForAll("validRepositoryPath") Path repositoryPath,
                                             @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And a file exists in the working directory
            try {
                Path filePath = repositoryPath.resolve(fileName);
                Files.writeString(filePath, "Content for " + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + fileName, e);
            }
            
            // When staging the file multiple times
            commitService.stageFiles(repositoryPath, List.of(fileName));
            RepositoryStatus statusAfterFirstStaging = repositoryService.getStatus(repositoryPath);
            
            commitService.stageFiles(repositoryPath, List.of(fileName));
            RepositoryStatus statusAfterSecondStaging = repositoryService.getStatus(repositoryPath);
            
            commitService.stageFiles(repositoryPath, List.of(fileName));
            RepositoryStatus statusAfterThirdStaging = repositoryService.getStatus(repositoryPath);
            
            // Then the status should be the same after each staging operation
            assertThat(statusAfterSecondStaging.stagedFiles()).isEqualTo(statusAfterFirstStaging.stagedFiles());
            assertThat(statusAfterThirdStaging.stagedFiles()).isEqualTo(statusAfterFirstStaging.stagedFiles());
            
            assertThat(statusAfterSecondStaging.unstagedFiles()).isEqualTo(statusAfterFirstStaging.unstagedFiles());
            assertThat(statusAfterThirdStaging.unstagedFiles()).isEqualTo(statusAfterFirstStaging.unstagedFiles());
            
            assertThat(statusAfterSecondStaging.untrackedFiles()).isEqualTo(statusAfterFirstStaging.untrackedFiles());
            assertThat(statusAfterThirdStaging.untrackedFiles()).isEqualTo(statusAfterFirstStaging.untrackedFiles());
            
            // File should still be staged
            assertThat(statusAfterThirdStaging.stagedFiles()).contains(fileName);
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Null repository path throws exception
    @Property(tries = 20)
    void stagingFilesWithNullRepositoryPathThrowsException(@ForAll("validFileNames") List<String> fileNames) {
        // When attempting to stage files with null repository path
        // Then it should throw an exception
        assertThatThrownBy(() -> commitService.stageFiles(null, fileNames))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                // Log the exception for verification
                logger.info("Expected exception for null repository path: {} - {}", 
                    exception.getClass().getSimpleName(), exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Null file list throws exception
    @Property(tries = 20)
    void stagingFilesWithNullFileListThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a valid repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When attempting to stage files with null file list
            // Then it should throw an exception
            assertThatThrownBy(() -> commitService.stageFiles(repositoryPath, null))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    // Log the exception for verification
                    logger.info("Expected exception for null file list: {} - {}", 
                        exception.getClass().getSimpleName(), exception.getMessage());
                });
                
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Non-existent repository throws exception
    @Property(tries = 30)
    void stagingFilesInNonExistentRepositoryThrowsException(@ForAll("nonExistentRepositoryPath") Path nonExistentPath,
                                                          @ForAll("validFileNames") List<String> fileNames) {
        // When attempting to stage files in a non-existent repository
        // Then it should throw an exception
        assertThatThrownBy(() -> commitService.stageFiles(nonExistentPath, fileNames))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                // Log the exception for verification
                logger.info("Expected exception for non-existent repository: {} - {}", 
                    exception.getClass().getSimpleName(), exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Invalid repository (no .git) throws exception
    @Property(tries = 30)
    void stagingFilesInInvalidRepositoryThrowsException(@ForAll("invalidRepositoryPath") Path invalidPath,
                                                      @ForAll("validFileNames") List<String> fileNames) {
        try {
            // When attempting to stage files in an invalid repository (directory without .git)
            // Then it should throw an exception
            assertThatThrownBy(() -> commitService.stageFiles(invalidPath, fileNames))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    // Log the exception for verification
                    logger.info("Expected exception for invalid repository: {} - {}", 
                        exception.getClass().getSimpleName(), exception.getMessage());
                });
                
        } finally {
            cleanupRepository(invalidPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Non-existent files are handled gracefully
    @Property(tries = 30)
    void stagingNonExistentFilesHandledGracefully(@ForAll("validRepositoryPath") Path repositoryPath,
                                                @ForAll("nonExistentFileNames") List<String> nonExistentFiles) {
        try {
            // Given a valid repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When attempting to stage non-existent files
            // Then it should either succeed (Git ignores non-existent files) or throw a specific exception
            try {
                commitService.stageFiles(repositoryPath, nonExistentFiles);
                // If no exception is thrown, verify the repository status is still valid
                RepositoryStatus status = repositoryService.getStatus(repositoryPath);
                assertThat(status).isNotNull();
                logger.info("Staging non-existent files succeeded (Git behavior: ignores non-existent files)");
            } catch (Exception e) {
                // If an exception is thrown, log it for verification
                logger.info("Expected exception for non-existent files: {} - {}", 
                    e.getClass().getSimpleName(), e.getMessage());
                // Verify it's a reasonable exception type
                assertThat(e).isInstanceOfAny(
                    RuntimeException.class,
                    IllegalArgumentException.class,
                    io.sandboxdev.gitmcp.exception.GitMcpException.class
                );
            }
            
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - File path with invalid characters
    @Property(tries = 20)
    void stagingFilesWithInvalidCharactersHandledGracefully(@ForAll("validRepositoryPath") Path repositoryPath,
                                                          @ForAll("invalidFileNames") List<String> invalidFileNames) {
        try {
            // Given a valid repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When attempting to stage files with invalid characters in names
            // Then it should either succeed or throw a specific exception
            try {
                commitService.stageFiles(repositoryPath, invalidFileNames);
                // If no exception is thrown, verify the repository status is still valid
                RepositoryStatus status = repositoryService.getStatus(repositoryPath);
                assertThat(status).isNotNull();
                logger.info("Staging files with special characters succeeded");
            } catch (Exception e) {
                // If an exception is thrown, log it for verification
                logger.info("Expected exception for invalid file names: {} - {}", 
                    e.getClass().getSimpleName(), e.getMessage());
                // Verify it's a reasonable exception type
                assertThat(e).isInstanceOfAny(
                    RuntimeException.class,
                    IllegalArgumentException.class,
                    io.sandboxdev.gitmcp.exception.GitMcpException.class
                );
            }
            
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Corrupted repository may throw exception or handle gracefully
    @Property(tries = 20)
    void stagingFilesInCorruptedRepositoryHandledGracefully(@ForAll("validRepositoryPath") Path repositoryPath,
                                                          @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And the repository is corrupted by removing critical .git files
            try {
                Path gitDir = repositoryPath.resolve(".git");
                Path headFile = gitDir.resolve("HEAD");
                if (Files.exists(headFile)) {
                    Files.delete(headFile);
                }
                Path configFile = gitDir.resolve("config");
                if (Files.exists(configFile)) {
                    Files.delete(configFile);
                }
            } catch (IOException e) {
                // If we can't corrupt the repository, skip this test iteration
                return;
            }
            
            // Create a test file to stage
            try {
                Path filePath = repositoryPath.resolve(fileName);
                Files.writeString(filePath, "Test content");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create test file", e);
            }
            
            // When attempting to stage files in the corrupted repository
            // Then it should either throw an exception or handle gracefully
            try {
                commitService.stageFiles(repositoryPath, List.of(fileName));
                // If no exception is thrown, the implementation is robust and handles corruption gracefully
                logger.info("Staging in corrupted repository succeeded (robust implementation)");
            } catch (Exception e) {
                // If an exception is thrown, log it for verification
                logger.info("Expected exception for corrupted repository: {} - {}", 
                    e.getClass().getSimpleName(), e.getMessage());
                // Verify it's a reasonable exception type
                assertThat(e).isInstanceOfAny(
                    RuntimeException.class,
                    IllegalArgumentException.class,
                    io.sandboxdev.gitmcp.exception.GitMcpException.class
                );
            }
                
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 8: Failure conditions - Very long file paths
    @Property(tries = 20)
    void stagingFilesWithVeryLongPathsHandledGracefully(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a valid repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // Create a very long file path (close to filesystem limits)
            StringBuilder longPath = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                longPath.append("very-long-directory-name-").append(i).append("/");
            }
            longPath.append("very-long-file-name.txt");
            
            String longFileName = longPath.toString();
            
            // When attempting to stage a file with a very long path
            // Then it should either succeed or throw a specific exception
            try {
                commitService.stageFiles(repositoryPath, List.of(longFileName));
                // If no exception is thrown, verify the repository status is still valid
                RepositoryStatus status = repositoryService.getStatus(repositoryPath);
                assertThat(status).isNotNull();
                logger.info("Staging file with very long path succeeded");
            } catch (Exception e) {
                // If an exception is thrown, log it for verification
                logger.info("Expected exception for very long file path: {} - {}", 
                    e.getClass().getSimpleName(), e.getMessage());
                // Verify it's a reasonable exception type
                assertThat(e).isInstanceOfAny(
                    RuntimeException.class,
                    IllegalArgumentException.class,
                    io.sandboxdev.gitmcp.exception.GitMcpException.class
                );
            }
            
        } finally {
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
                    return Files.createTempDirectory("git-staging-test-" + randomName + "-");
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
    Arbitrary<List<String>> validFileNames() {
        return validFileName().list().ofMinSize(1).ofMaxSize(5);
    }
    
    @Provide
    Arbitrary<Path> nonExistentRepositoryPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(10)
            .ofMaxLength(30)
            .map(randomName -> Path.of("/tmp/non-existent-repo-" + randomName + "-" + System.currentTimeMillis()));
    }
    
    @Provide
    Arbitrary<Path> invalidRepositoryPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(5)
            .ofMaxLength(15)
            .map(randomName -> {
                try {
                    // Create a directory but don't initialize it as a Git repository
                    Path tempDir = Files.createTempDirectory("git-invalid-test-" + randomName + "-");
                    // Create some dummy files to make it look like a directory with content
                    Files.writeString(tempDir.resolve("dummy.txt"), "This is not a git repository");
                    return tempDir;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create invalid repository directory", e);
                }
            });
    }
    
    @Provide
    Arbitrary<List<String>> nonExistentFileNames() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(name -> "non-existent-" + name + ".txt")
            .list()
            .ofMinSize(1)
            .ofMaxSize(3);
    }
    
    @Provide
    Arbitrary<List<String>> invalidFileNames() {
        return Arbitraries.oneOf(
            // Files with special characters that might cause issues
            Arbitraries.of(
                List.of("file\0with\0null.txt"),
                List.of("file\nwith\nnewline.txt"),
                List.of("file\twith\ttab.txt"),
                List.of("file with spaces.txt"),
                List.of("file|with|pipes.txt"),
                List.of("file<with>brackets.txt"),
                List.of("file\"with\"quotes.txt"),
                List.of("file*with*asterisk.txt"),
                List.of("file?with?question.txt"),
                List.of("file:with:colon.txt"),
                List.of("../../../etc/passwd"),
                List.of("..\\..\\..\\windows\\system32\\config"),
                List.of("CON"), // Windows reserved name
                List.of("PRN"), // Windows reserved name
                List.of("AUX"), // Windows reserved name
                List.of("NUL")  // Windows reserved name
            ),
            // Very long file names
            Arbitraries.strings()
                .alpha()
                .ofMinLength(255)
                .ofMaxLength(300)
                .map(longName -> longName + ".txt")
                .map(List::of)
        );
    }
    
    private void createInitialCommit(Path repositoryPath) {
        try {
            // Create a simple file to commit
            Path testFile = repositoryPath.resolve("README.md");
            Files.writeString(testFile, "# Test Repository\n\nThis is a test repository for staging testing.");
            
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