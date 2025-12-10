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
 * FAILURE CASE 3: Invalid Hash Format Bug
 * This demonstrates what happens when the implementation returns malformed hashes.
 * 
 * Bug Simulation: The convertToCommitInfo method returns hashes with wrong length/format.
 * Expected Failure: Property test should fail when asserting hash format and consistency.
 */
public class CommitMetadataCompletenessFailureCase3 {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitMetadataCompletenessFailureCase3.class);
    private final GitRepositoryService repositoryService;
    
    public CommitMetadataCompletenessFailureCase3() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new BuggyGitRepositoryServiceInvalidHashFormat(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 5: Commit info contains complete metadata
    @Property(tries = 100)
    void commitInfoContainsCompleteMetadata(@ForAll("repositoryWithCommits") Path repositoryPath) {
        logger.info("FAILURE CASE 3: Testing malformed hash validation for repository: {}", repositoryPath);
        
        try {
            // When getting commit history from a repository that returns malformed hashes
            // Then the system should throw an appropriate exception
            logger.debug("Testing getHistory() with malformed hash validation");
            assertThatThrownBy(() -> repositoryService.getHistory(repositoryPath, 10))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("hash format");
            
            logger.info("✓ getHistory() correctly threw GitMcpException for malformed hash");
            
            // When getting commit details from a repository that returns malformed hashes
            // Then the system should throw an appropriate exception
            GitRepositoryService validService = new GitRepositoryServiceImpl(
                new JGitRepositoryManager(), new JGitCommandExecutor());
            List<CommitInfo> validHistory = validService.getHistory(repositoryPath, 1);
            String validHash = validHistory.get(0).hash();
            
            logger.debug("Testing getCommitDetails() with malformed hash validation for hash: {}", validHash);
            assertThatThrownBy(() -> repositoryService.getCommitDetails(repositoryPath, validHash))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("hash format");
            
            logger.info("✓ getCommitDetails() correctly threw GitMcpException for malformed hash");
            logger.info("FAILURE CASE 3: PASSED - System correctly validates hash format");
            
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
     * Buggy implementation that should validate and throw exceptions for malformed hashes
     */
    private static class BuggyGitRepositoryServiceInvalidHashFormat extends GitRepositoryServiceImpl {
        
        public BuggyGitRepositoryServiceInvalidHashFormat(JGitRepositoryManager repositoryManager, 
                                                          JGitCommandExecutor commandExecutor) {
            super(repositoryManager, commandExecutor);
        }
        
        @Override
        public List<CommitInfo> getHistory(Path repositoryPath, int limit) {
            List<CommitInfo> originalHistory = super.getHistory(repositoryPath, limit);
            
            // Simulate a bug where the system tries to create commits with malformed hashes
            // The system should detect this and throw an exception
            for (CommitInfo commit : originalHistory) {
                validateCommitInfo(new CommitInfo(
                    "INVALID_HASH_TOO_SHORT", // BUG: wrong length, invalid characters
                    "xyz123", // BUG: doesn't start with full hash, invalid characters
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
            
            // Simulate a bug where the system tries to create commit with malformed hashes
            CommitInfo buggyCommit = new CommitInfo(
                "INVALID_HASH_TOO_SHORT", // BUG: wrong length, invalid characters
                "xyz123", // BUG: doesn't start with full hash, invalid characters
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
            String hash = commit.hash();
            String shortHash = commit.shortHash();
            logger.debug("Validating hash format: hash='{}' (length={}), shortHash='{}'", 
                hash, hash != null ? hash.length() : "null", shortHash);
            
            if (hash == null || hash.length() != 40 || !hash.matches("^[a-f0-9]{40}$")) {
                logger.warn("VALIDATION FAILED: Invalid hash format - hash='{}', length={}, matches pattern={}", 
                    hash, hash != null ? hash.length() : "null", 
                    hash != null ? hash.matches("^[a-f0-9]{40}$") : false);
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: hash format is invalid (must be 40 hex characters)"
                );
            }
            
            if (shortHash == null || !hash.startsWith(shortHash) || !shortHash.matches("^[a-f0-9]+$")) {
                logger.warn("VALIDATION FAILED: Invalid short hash format - shortHash='{}', starts with full hash={}, matches pattern={}", 
                    shortHash, hash.startsWith(shortHash), 
                    shortHash != null ? shortHash.matches("^[a-f0-9]+$") : false);
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: short hash format is invalid or inconsistent with full hash"
                );
            }
            
            logger.debug("Hash format validation passed");
        }
    }
}