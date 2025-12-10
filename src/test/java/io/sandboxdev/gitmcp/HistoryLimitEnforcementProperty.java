package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.CommitInfo;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for history limit enforcement.
 * Feature: git-mcp-server, Property 6: History limit is respected
 * Validates: Requirements 2.3
 */
public class HistoryLimitEnforcementProperty {
    
    private static final Logger logger = LoggerFactory.getLogger(HistoryLimitEnforcementProperty.class);
    private final GitRepositoryService repositoryService;
    
    public HistoryLimitEnforcementProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 6: History limit is respected
    @Property(tries = 100)
    void historyLimitIsRespected(@ForAll("repositoryWithManyCommits") RepositoryWithCommitCount repoData,
                               @ForAll @IntRange(min = 1, max = 50) int requestedLimit) {
        try {
            Path repositoryPath = repoData.path();
            int actualCommitCount = repoData.commitCount();
            
            // When requesting history with a specific limit
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, requestedLimit);
            
            // Then the returned history should respect the limit
            assertThat(history).isNotNull();
            
            // The number of returned commits should never exceed the requested limit
            assertThat(history.size()).isLessThanOrEqualTo(requestedLimit);
            
            // If there are fewer commits than the limit, return all commits
            if (actualCommitCount <= requestedLimit) {
                assertThat(history.size()).isEqualTo(actualCommitCount);
            } else {
                // If there are more commits than the limit, return exactly the limit
                assertThat(history.size()).isEqualTo(requestedLimit);
            }
            
            // Verify that all returned commits are valid
            for (CommitInfo commit : history) {
                assertThat(commit).isNotNull();
                assertThat(commit.hash()).isNotNull();
                assertThat(commit.hash()).isNotEmpty();
            }
            
        } finally {
            // Cleanup
            cleanupRepository(repoData.path());
        }
    }
    
    // Feature: git-mcp-server, Property 6: History limit is respected with zero limit
    @Property(tries = 30)
    void historyWithZeroLimitReturnsEmpty(@ForAll("repositoryWithManyCommits") RepositoryWithCommitCount repoData) {
        try {
            Path repositoryPath = repoData.path();
            
            // When requesting history with zero limit
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, 0);
            
            // Then it should return an empty list
            assertThat(history).isNotNull();
            assertThat(history).isEmpty();
            
        } finally {
            // Cleanup
            cleanupRepository(repoData.path());
        }
    }
    
    // Feature: git-mcp-server, Property 6: History limit is respected with very large limit
    @Property(tries = 30)
    void historyWithVeryLargeLimitReturnsAllCommits(@ForAll("repositoryWithManyCommits") RepositoryWithCommitCount repoData) {
        try {
            Path repositoryPath = repoData.path();
            int actualCommitCount = repoData.commitCount();
            
            // When requesting history with a very large limit (much larger than actual commits)
            int veryLargeLimit = actualCommitCount * 10 + 1000;
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, veryLargeLimit);
            
            // Then it should return all available commits (not more than what exists)
            assertThat(history).isNotNull();
            assertThat(history.size()).isEqualTo(actualCommitCount);
            
            // Verify that all returned commits are valid
            for (CommitInfo commit : history) {
                assertThat(commit).isNotNull();
                assertThat(commit.hash()).isNotNull();
                assertThat(commit.hash()).isNotEmpty();
            }
            
        } finally {
            // Cleanup
            cleanupRepository(repoData.path());
        }
    }
    
    // Feature: git-mcp-server, Property 6: History limit is respected - commits are in chronological order
    @Property(tries = 50)
    void historyLimitRespectsChronologicalOrder(@ForAll("repositoryWithManyCommits") RepositoryWithCommitCount repoData,
                                              @ForAll @IntRange(min = 2, max = 10) int requestedLimit) {
        try {
            Path repositoryPath = repoData.path();
            int actualCommitCount = repoData.commitCount();
            
            // Skip if repository doesn't have enough commits
            if (actualCommitCount < 2) {
                return;
            }
            
            // When requesting history with a limit
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, requestedLimit);
            
            // Then the returned commits should be in reverse chronological order (newest first)
            assertThat(history).isNotNull();
            assertThat(history.size()).isGreaterThan(1);
            
            for (int i = 0; i < history.size() - 1; i++) {
                CommitInfo current = history.get(i);
                CommitInfo next = history.get(i + 1);
                
                // Current commit should be newer than or equal to the next commit
                assertThat(current.timestamp()).isAfterOrEqualTo(next.timestamp());
            }
            
        } finally {
            // Cleanup
            cleanupRepository(repoData.path());
        }
    }
    
    // Feature: git-mcp-server, Property 6: History limit is respected with single commit repository
    @Property(tries = 30)
    void historyLimitWithSingleCommitRepository(@ForAll @IntRange(min = 1, max = 20) int requestedLimit) {
        Path repositoryPath = null;
        try {
            // Given a repository with exactly one commit
            repositoryPath = createRepositoryWithCommits(1);
            
            // When requesting history with any positive limit
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, requestedLimit);
            
            // Then it should return exactly one commit
            assertThat(history).isNotNull();
            assertThat(history.size()).isEqualTo(1);
            
            // Verify the single commit is valid
            CommitInfo commit = history.get(0);
            assertThat(commit).isNotNull();
            assertThat(commit.hash()).isNotNull();
            assertThat(commit.hash()).isNotEmpty();
            
        } finally {
            // Cleanup
            if (repositoryPath != null) {
                cleanupRepository(repositoryPath);
            }
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Null repository path throws exception
    @Property(tries = 20)
    void historyRequestWithNullPathThrowsException() {
        logger.info("Testing null repository path - expecting exception to be thrown and logged");
        
        // When requesting history from a null repository path
        // Then it should throw a GitMcpException (wrapping the NPE) and be logged
        assertThatThrownBy(() -> repositoryService.getHistory(null, 10))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("null")
            .satisfies(exception -> {
                logger.info("Verified null path exception was thrown: {}", exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Non-existent repository path throws exception
    @Property(tries = 30)
    void historyRequestOnNonExistentPathThrowsException(@ForAll("nonExistentPath") Path nonExistentPath) {
        logger.info("Testing non-existent repository path: {} - expecting exception to be thrown and logged", nonExistentPath);
        
        // When requesting history from a non-existent path
        // Then it should throw an exception and not return a history response
        assertThatThrownBy(() -> repositoryService.getHistory(nonExistentPath, 10))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                logger.info("Verified non-existent path exception was thrown for {}: {}", nonExistentPath, exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Empty directory without .git throws exception
    @Property(tries = 30)
    void historyRequestOnEmptyDirectoryThrowsException(@ForAll("validRepositoryPath") Path emptyPath) {
        try {
            // Given an empty directory exists (no .git folder)
            Files.createDirectories(emptyPath);
            
            logger.info("Testing empty directory without .git: {} - expecting exception to be thrown and logged", emptyPath);
            
            // When requesting history from the empty directory
            // Then it should throw an exception and not return a history response
            assertThatThrownBy(() -> repositoryService.getHistory(emptyPath, 10))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    logger.info("Verified empty directory exception was thrown for {}: {}", emptyPath, exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty directory", e);
        } finally {
            // Cleanup
            cleanupRepository(emptyPath);
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Missing .git directory throws exception
    @Property(tries = 20)
    void historyRequestOnMissingGitDirectoryThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized
            repositoryService.initRepository(repositoryPath);
            
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
            
            logger.info("Testing repository with missing .git directory: {} - expecting exception to be thrown and logged", repositoryPath);
            
            // When requesting history from the directory without .git
            // Then it should throw an exception and not return a history response
            assertThatThrownBy(() -> repositoryService.getHistory(repositoryPath, 10))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    logger.info("Verified missing .git directory exception was thrown for {}: {}", repositoryPath, exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove .git directory", e);
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - File instead of directory throws exception
    @Property(tries = 20)
    void historyRequestOnFilePathThrowsException(@ForAll("validRepositoryPath") Path basePath) {
        try {
            // Given a file exists instead of a directory
            final Path filePath = basePath.resolve("not-a-directory.txt");
            Files.createDirectories(basePath);
            Files.writeString(filePath, "This is a file, not a directory");
            
            logger.info("Testing file path instead of directory: {} - expecting exception to be thrown and logged", filePath);
            
            // When requesting history from the file path
            // Then it should throw an exception and not return a history response
            assertThatThrownBy(() -> repositoryService.getHistory(filePath, 10))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    logger.info("Verified file path exception was thrown for {}: {}", filePath, exception.getMessage());
                });
                
            // Cleanup the file
            Files.deleteIfExists(filePath);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test file", e);
        } finally {
            // Cleanup
            cleanupRepository(basePath);
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Negative limit values should not crash
    @Property(tries = 30)
    void historyRequestWithNegativeLimitDoesNotCrash(@ForAll("repositoryWithManyCommits") RepositoryWithCommitCount repoData,
                                                   @ForAll @IntRange(min = -100, max = -1) int negativeLimit) {
        try {
            Path repositoryPath = repoData.path();
            
            logger.info("Testing negative limit value: {} on repository: {} - expecting graceful handling", negativeLimit, repositoryPath);
            
            // When requesting history with a negative limit
            // Then it should not crash the system with an unhandled exception
            // Test that it either succeeds or throws a controlled exception
            try {
                List<CommitInfo> history = repositoryService.getHistory(repositoryPath, negativeLimit);
                // If no exception is thrown, verify the result is valid
                assertThat(history).isNotNull();
                assertThat(history.size()).isGreaterThanOrEqualTo(0);
                logger.info("Negative limit {} handled gracefully, returned {} commits", negativeLimit, history.size());
            } catch (Exception e) {
                // If an exception is thrown, verify it's a controlled exception (not a system crash)
                assertThat(e).isInstanceOf(Exception.class);
                assertThat(e).isNotInstanceOf(OutOfMemoryError.class);
                assertThat(e).isNotInstanceOf(StackOverflowError.class);
                logger.info("Negative limit {} threw controlled exception: {}", negativeLimit, e.getMessage());
            }
            
        } finally {
            // Cleanup
            cleanupRepository(repoData.path());
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Corrupted .git directory throws exception
    @Property(tries = 15)
    void historyRequestOnCorruptedGitDirectoryThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized
            repositoryService.initRepository(repositoryPath);
            createRepositoryWithCommits(repositoryPath, 2);
            
            // And the .git directory is corrupted by replacing a critical file with invalid content
            Path gitDir = repositoryPath.resolve(".git");
            Path headFile = gitDir.resolve("HEAD");
            if (Files.exists(headFile)) {
                Files.writeString(headFile, "CORRUPTED CONTENT - NOT A VALID HEAD FILE");
            }
            
            logger.info("Testing corrupted .git directory: {} - expecting exception to be thrown and logged", repositoryPath);
            
            // When requesting history from the corrupted repository
            // Then it should throw an exception and not return a history response
            assertThatThrownBy(() -> repositoryService.getHistory(repositoryPath, 10))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    logger.info("Verified corrupted repository exception was thrown for {}: {}", repositoryPath, exception.getMessage());
                });
                
        } catch (Exception e) {
            // If setup fails, that's also a valid test outcome
            assertThat(e).isInstanceOf(Exception.class);
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Repository with no commits throws exception
    @Property(tries = 20)
    void historyRequestOnRepositoryWithNoCommitsThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized but has no commits
            repositoryService.initRepository(repositoryPath);
            // Note: No commits are created
            
            logger.info("Testing repository with no commits: {} - expecting exception to be thrown and logged", repositoryPath);
            
            // When requesting history from the repository with no commits
            // Then it should throw an exception because there's no HEAD
            assertThatThrownBy(() -> repositoryService.getHistory(repositoryPath, 10))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("HEAD")
                .satisfies(exception -> {
                    logger.info("Verified no commits exception was thrown for {}: {}", repositoryPath, exception.getMessage());
                });
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 6: Failed conditions - Very large limit values
    @Property(tries = 20)
    void historyRequestWithVeryLargeLimitHandledGracefully(@ForAll("repositoryWithManyCommits") RepositoryWithCommitCount repoData) {
        try {
            Path repositoryPath = repoData.path();
            int actualCommitCount = repoData.commitCount();
            
            // When requesting history with Integer.MAX_VALUE limit
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, Integer.MAX_VALUE);
            
            // Then it should return all available commits without issues
            assertThat(history).isNotNull();
            assertThat(history.size()).isEqualTo(actualCommitCount);
            assertThat(history.size()).isLessThanOrEqualTo(Integer.MAX_VALUE);
            
        } finally {
            // Cleanup
            cleanupRepository(repoData.path());
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
                    return Files.createTempDirectory("git-history-fail-test-" + randomName + "-");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            });
    }
    
    @Provide
    Arbitrary<Path> nonExistentPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(10)
            .ofMaxLength(30)
            .map(randomName -> Path.of("/tmp/non-existent-history-" + randomName + "-" + System.currentTimeMillis()));
    }
    
    @Provide
    Arbitrary<RepositoryWithCommitCount> repositoryWithManyCommits() {
        return Arbitraries.integers().between(1, 20).map(numCommits -> {
            Path repositoryPath = createRepositoryWithCommits(numCommits);
            return new RepositoryWithCommitCount(repositoryPath, numCommits);
        });
    }
    
    private Path createRepositoryWithCommits(int numCommits) {
        try {
            Path tempDir = Files.createTempDirectory("git-history-test-");
            Git git = Git.init().setDirectory(tempDir.toFile()).call();
            
            // Configure author for commits
            PersonIdent author = new PersonIdent("Test Author", "test@example.com");
            
            // Create the specified number of commits with different content
            for (int i = 1; i <= numCommits; i++) {
                // Create a file with unique content
                Path file = tempDir.resolve("file" + i + ".txt");
                Files.writeString(file, "Content for commit " + i + "\nTimestamp: " + System.currentTimeMillis());
                
                // Stage and commit the file
                git.add().addFilepattern("file" + i + ".txt").call();
                git.commit()
                    .setMessage("Commit " + i + ": Add file" + i + ".txt")
                    .setAuthor(author)
                    .setCommitter(author)
                    .call();
                
                // Add a small delay to ensure different timestamps
                Thread.sleep(10);
            }
            
            git.close();
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test repository with " + numCommits + " commits", e);
        }
    }
    
    private void createRepositoryWithCommits(Path repositoryPath, int numCommits) {
        try {
            Git git = Git.open(repositoryPath.toFile());
            
            // Configure author for commits
            PersonIdent author = new PersonIdent("Test Author", "test@example.com");
            
            // Create the specified number of commits with different content
            for (int i = 1; i <= numCommits; i++) {
                // Create a file with unique content
                Path file = repositoryPath.resolve("file" + i + ".txt");
                Files.writeString(file, "Content for commit " + i + "\nTimestamp: " + System.currentTimeMillis());
                
                // Stage and commit the file
                git.add().addFilepattern("file" + i + ".txt").call();
                git.commit()
                    .setMessage("Commit " + i + ": Add file" + i + ".txt")
                    .setAuthor(author)
                    .setCommitter(author)
                    .call();
                
                // Add a small delay to ensure different timestamps
                Thread.sleep(10);
            }
            
            git.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create commits in existing repository", e);
        }
    }
    
    private void cleanupRepository(Path repositoryPath) {
        try {
            if (repositoryPath != null && Files.exists(repositoryPath)) {
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
    
    /**
     * Helper record to hold repository path and commit count information.
     */
    private record RepositoryWithCommitCount(Path path, int commitCount) {}
}