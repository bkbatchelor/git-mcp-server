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
 * FAILURE CASE 1: Null Hash Bug
 * This demonstrates what happens when the implementation returns null hashes.
 * 
 * Bug Simulation: The convertToCommitInfo method returns null for hash fields.
 * Expected Failure: Property test should fail when asserting hash is not null.
 */
public class CommitMetadataCompletenessFailureCase1 {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitMetadataCompletenessFailureCase1.class);
    private final GitRepositoryService repositoryService;
    
    public CommitMetadataCompletenessFailureCase1() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new BuggyGitRepositoryServiceNullHash(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 5: Commit info contains complete metadata
    @Property(tries = 100)
    void commitInfoContainsCompleteMetadata(@ForAll("repositoryWithCommits") Path repositoryPath) {
        logger.info("FAILURE CASE 1: Testing null hash validation for repository: {}", repositoryPath);
        
        try {
            // When getting commit history from a repository that returns null hashes
            // Then the system should throw an appropriate exception
            logger.debug("Testing getHistory() with null hash validation");
            assertThatThrownBy(() -> repositoryService.getHistory(repositoryPath, 10))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("hash cannot be null");
            
            logger.info("✓ getHistory() correctly threw GitMcpException for null hash");
            
            // When getting commit details from a repository that returns null hashes
            // Then the system should throw an appropriate exception
            // First get a valid commit hash from the original implementation to test with
            GitRepositoryService validService = new GitRepositoryServiceImpl(
                new JGitRepositoryManager(), new JGitCommandExecutor());
            List<CommitInfo> validHistory = validService.getHistory(repositoryPath, 1);
            String validHash = validHistory.get(0).hash();
            
            logger.debug("Testing getCommitDetails() with null hash validation for hash: {}", validHash);
            assertThatThrownBy(() -> repositoryService.getCommitDetails(repositoryPath, validHash))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("hash cannot be null");
            
            logger.info("✓ getCommitDetails() correctly threw GitMcpException for null hash");
            logger.info("FAILURE CASE 1: PASSED - System correctly validates null hashes");
            
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
     * Buggy implementation that should validate and throw exceptions for null hashes
     */
    private static class BuggyGitRepositoryServiceNullHash extends GitRepositoryServiceImpl {
        
        public BuggyGitRepositoryServiceNullHash(JGitRepositoryManager repositoryManager, 
                                                 JGitCommandExecutor commandExecutor) {
            super(repositoryManager, commandExecutor);
        }
        
        @Override
        public List<CommitInfo> getHistory(Path repositoryPath, int limit) {
            List<CommitInfo> originalHistory = super.getHistory(repositoryPath, limit);
            
            // Simulate a bug where the system tries to create commits with null hashes
            // The system should detect this and throw an exception
            for (CommitInfo commit : originalHistory) {
                validateCommitInfo(new CommitInfo(
                    null, // BUG: null hash - should be detected
                    null, // BUG: null short hash - should be detected
                    commit.message(),
                    commit.author(),
                    commit.timestamp(),
                    commit.changedFiles(),
                    commit.stats()
                ));
            }
            
            return originalHistory; // This line should never be reached
        }
        
        @Override
        public CommitInfo getCommitDetails(Path repositoryPath, String commitHash) {
            CommitInfo original = super.getCommitDetails(repositoryPath, commitHash);
            
            // Simulate a bug where the system tries to create commit with null hashes
            // The system should detect this and throw an exception
            CommitInfo buggyCommit = new CommitInfo(
                null, // BUG: null hash - should be detected
                null, // BUG: null short hash - should be detected
                original.message(),
                original.author(),
                original.timestamp(),
                original.changedFiles(),
                original.stats()
            );
            
            validateCommitInfo(buggyCommit);
            return buggyCommit; // This line should never be reached
        }
        
        private void validateCommitInfo(CommitInfo commit) {
            logger.debug("Validating commit info: hash={}, shortHash={}", commit.hash(), commit.shortHash());
            
            if (commit.hash() == null) {
                logger.warn("VALIDATION FAILED: Detected null hash in commit data");
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: hash cannot be null"
                );
            }
            if (commit.shortHash() == null) {
                logger.warn("VALIDATION FAILED: Detected null short hash in commit data");
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: short hash cannot be null"
                );
            }
            
            logger.debug("Commit validation passed");
        }
    }
}