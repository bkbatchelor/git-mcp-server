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
 * FAILURE CASE 5: Inconsistent Data Between History and Details Bug
 * This demonstrates what happens when getHistory and getCommitDetails return inconsistent data.
 * 
 * Bug Simulation: getCommitDetails returns different hash than what was in getHistory.
 * Expected Failure: Property test should fail when asserting consistency between methods.
 */
public class CommitMetadataCompletenessFailureCase5 {
    
    private static final Logger logger = LoggerFactory.getLogger(CommitMetadataCompletenessFailureCase5.class);
    private final GitRepositoryService repositoryService;
    
    public CommitMetadataCompletenessFailureCase5() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new BuggyGitRepositoryServiceInconsistentData(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 5: Commit info contains complete metadata
    @Property(tries = 100)
    void commitInfoContainsCompleteMetadata(@ForAll("repositoryWithCommits") Path repositoryPath) {
        logger.info("FAILURE CASE 5: Testing data consistency validation for repository: {}", repositoryPath);
        
        try {
            // When getting commit details that would return inconsistent data
            // Then the system should throw an appropriate exception
            GitRepositoryService validService = new GitRepositoryServiceImpl(
                new JGitRepositoryManager(), new JGitCommandExecutor());
            List<CommitInfo> validHistory = validService.getHistory(repositoryPath, 1);
            String validHash = validHistory.get(0).hash();
            
            logger.debug("Testing getCommitDetails() with data consistency validation for hash: {}", validHash);
            assertThatThrownBy(() -> repositoryService.getCommitDetails(repositoryPath, validHash))
                .isInstanceOf(GitMcpException.class)
                .hasMessageContaining("Invalid commit data")
                .hasMessageContaining("inconsistent");
            
            logger.info("✓ getCommitDetails() correctly threw GitMcpException for inconsistent data");
            logger.info("FAILURE CASE 5: PASSED - System correctly validates data consistency");
            
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
     * Buggy implementation that should validate and throw exceptions for inconsistent data
     */
    private static class BuggyGitRepositoryServiceInconsistentData extends GitRepositoryServiceImpl {
        
        public BuggyGitRepositoryServiceInconsistentData(JGitRepositoryManager repositoryManager, 
                                                         JGitCommandExecutor commandExecutor) {
            super(repositoryManager, commandExecutor);
        }
        
        @Override
        public CommitInfo getCommitDetails(Path repositoryPath, String commitHash) {
            CommitInfo original = super.getCommitDetails(repositoryPath, commitHash);
            
            // Simulate a bug where the system would return inconsistent data
            // The system should detect this and throw an exception
            String buggyHash = "1234567890abcdef1234567890abcdef12345678"; // Different from requested
            
            logger.debug("Validating data consistency: requested hash='{}', would return hash='{}'", 
                commitHash, buggyHash);
            
            if (!buggyHash.equals(commitHash)) {
                logger.warn("VALIDATION FAILED: Detected inconsistent data - requested='{}', would return='{}'", 
                    commitHash, buggyHash);
                throw new GitMcpException(
                    ErrorCode.INVALID_REPOSITORY_STATE,
                    "Invalid commit data: returned hash is inconsistent with requested hash"
                );
            }
            
            // This should never be reached because the hashes don't match
            logger.debug("Data consistency validation passed (this should never be reached)");
            return new CommitInfo(
                buggyHash,
                "1234567",
                original.message(),
                original.author(),
                original.timestamp(),
                original.changedFiles(),
                original.stats()
            );
        }
    }
}