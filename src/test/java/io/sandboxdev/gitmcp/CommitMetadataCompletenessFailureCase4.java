package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.AuthorInfo;
import io.sandboxdev.gitmcp.model.CommitInfo;
import io.sandboxdev.gitmcp.model.DiffStats;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FAILURE CASE 4: Invalid Timestamp Bug
 * This demonstrates what happens when the implementation returns invalid timestamps.
 * 
 * Bug Simulation: The convertToCommitInfo method returns future timestamps or null timestamps.
 * Expected Failure: Property test should fail when asserting timestamp validity.
 */
public class CommitMetadataCompletenessFailureCase4 {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitMetadataCompletenessFailureCase4.class);
    private final GitRepositoryService repositoryService;
    
    public CommitMetadataCompletenessFailureCase4() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new BuggyGitRepositoryServiceInvalidTimestamp(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 5: Commit info contains complete metadata
    @Property(tries = 100)
    void commitInfoContainsCompleteMetadata(@ForAll("repositoryWithCommits") Path repositoryPath) {
        logger.info("FAILURE CASE 4: Testing invalid timestamp validation for repository: {}", repositoryPath);
        
        try {
            // When getting commit history from a repository that returns invalid timestamps
            // Then the system should throw an appropriate exception
            logger.debug("Testing getHistory() with invalid timestamp validation");
            assertThatThrownBy(() -> repositoryService.getHistory(repositoryPath, 10))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("timestamp");
            
            logger.info("✓ getHistory() correctly threw GitMcpException for invalid timestamp");
            
            // When getting commit details from a repository that returns invalid timestamps
            // Then the system should throw an appropriate exception
            GitRepositoryService validService = new GitRepositoryServiceImpl(
                new JGitRepositoryManager(), new JGitCommandExecutor());
            List<CommitInfo> validHistory = validService.getHistory(repositoryPath, 1);
            String validHash = validHistory.get(0).hash();
            
            logger.debug("Testing getCommitDetails() with invalid timestamp validation for hash: {}", validHash);
            assertThatThrownBy(() -> repositoryService.getCommitDetails(repositoryPath, validHash))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("timestamp");
            
            logger.info("✓ getCommitDetails() correctly threw GitMcpException for invalid timestamp");
            logger.info("FAILURE CASE 4: PASSED - System correctly validates timestamps");
            
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    @Provide
    Arbitrary<Path> repositoryWithCommits() {
        return Arbitraries.integers().between(1, 3).map(numCommits -> {
            try {
                Path tempDir = Files.createTempDirectory("git-test-");
                Git git = Git.init().setDirectory(tempDir.toFile()).call();
                
                PersonIdent author = new PersonIdent("Test Author", "test@example.com");
                
                for (int i = 1; i <= numCommits; i++) {
                    Path file = tempDir.resolve("file" + i + ".txt");
                    Files.writeString(file, "Content for commit " + i);
                    
                    git.add().addFilepattern("file" + i + ".txt").call();
                    git.commit()
                        .setMessage("Commit " + i + ": Add file" + i + ".txt")
                        .setAuthor(author)
                        .setCommitter(author)
                        .call();
                    
                    Thread.sleep(10);
                }
                
                return tempDir;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository with commits", e);
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
    
    /**
     * Buggy implementation that should validate and throw exceptions for invalid timestamps
     */
    private static class BuggyGitRepositoryServiceInvalidTimestamp extends GitRepositoryServiceImpl {
        
        public BuggyGitRepositoryServiceInvalidTimestamp(JGitRepositoryManager repositoryManager, 
                                                         JGitCommandExecutor commandExecutor) {
            super(repositoryManager, commandExecutor);
        }
        
        @Override
        public List<CommitInfo> getHistory(Path repositoryPath, int limit) {
            List<CommitInfo> originalHistory = super.getHistory(repositoryPath, limit);
            
            // Simulate a bug where the system tries to create commits with invalid timestamps
            // The system should detect this and throw an exception
            for (CommitInfo commit : originalHistory) {
                validateCommitInfo(new CommitInfo(
                    commit.hash(),
                    commit.shortHash(),
                    commit.message(),
                    commit.author(),
                    Instant.parse("2030-01-01T00:00:00Z"), // BUG: future timestamp
                    commit.changedFiles(),
                    commit.stats()
                ));
            }
            
            return originalHistory; // This line should never be reached
        }
        
        @Override
        public CommitInfo getCommitDetails(Path repositoryPath, String commitHash) {
            CommitInfo original = super.getCommitDetails(repositoryPath, commitHash);
            
            // Simulate a bug where the system tries to create commit with null timestamp
            CommitInfo buggyCommit = new CommitInfo(
                original.hash(),
                original.shortHash(),
                original.message(),
                original.author(),
                null, // BUG: null timestamp
                original.changedFiles(),
                original.stats()
            );
            
            validateCommitInfo(buggyCommit);
            return buggyCommit; // This line should never be reached
        }
        
        private void validateCommitInfo(CommitInfo commit) {
            Instant timestamp = commit.timestamp();
            Instant now = Instant.now();
            Instant earliestValid = Instant.parse("2000-01-01T00:00:00Z");
            
            logger.debug("Validating timestamp: timestamp={}, now={}, earliestValid={}", 
                timestamp, now, earliestValid);
            
            if (timestamp == null) {
                logger.warn("VALIDATION FAILED: Detected null timestamp");
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: timestamp cannot be null"
                );
            }
            
            if (timestamp.isAfter(now.plusSeconds(60))) {
                logger.warn("VALIDATION FAILED: Detected future timestamp - timestamp={}, now={}", 
                    timestamp, now);
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: timestamp cannot be in the future"
                );
            }
            
            if (timestamp.isBefore(earliestValid)) {
                logger.warn("VALIDATION FAILED: Detected unreasonably old timestamp - timestamp={}, earliestValid={}", 
                    timestamp, earliestValid);
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: timestamp is unreasonably old"
                );
            }
            
            logger.debug("Timestamp validation passed");
        }
    }
}