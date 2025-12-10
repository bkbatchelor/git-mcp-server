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

/**
 * Property-based tests for commit metadata completeness.
 */
public class CommitMetadataCompletenessProperty {
    
    private final GitRepositoryService repositoryService;
    
    public CommitMetadataCompletenessProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 5: Commit info contains complete metadata
    @Property(tries = 100)
    void commitInfoContainsCompleteMetadata(@ForAll("repositoryWithCommits") Path repositoryPath) {
        try {
            // When getting commit history from a repository with commits
            List<CommitInfo> history = repositoryService.getHistory(repositoryPath, 10);
            
            // Then each commit should contain complete metadata
            assertThat(history).isNotEmpty();
            
            for (CommitInfo commit : history) {
                // Verify all required fields are present and valid
                
                // Hash should be present and non-empty
                assertThat(commit.hash()).isNotNull();
                assertThat(commit.hash()).isNotEmpty();
                assertThat(commit.hash()).hasSize(40); // Full SHA-1 hash length
                assertThat(commit.hash()).matches("^[a-f0-9]{40}$"); // Valid hex string
                
                // Short hash should be present and valid
                assertThat(commit.shortHash()).isNotNull();
                assertThat(commit.shortHash()).isNotEmpty();
                assertThat(commit.shortHash()).hasSizeLessThanOrEqualTo(40);
                assertThat(commit.shortHash()).matches("^[a-f0-9]+$"); // Valid hex string
                assertThat(commit.hash()).startsWith(commit.shortHash()); // Short hash should be prefix of full hash
                
                // Message should be present (can be empty but not null)
                assertThat(commit.message()).isNotNull();
                
                // Author information should be complete
                AuthorInfo author = commit.author();
                assertThat(author).isNotNull();
                assertThat(author.name()).isNotNull();
                assertThat(author.name()).isNotEmpty();
                assertThat(author.email()).isNotNull();
                assertThat(author.email()).isNotEmpty();
                assertThat(author.email()).contains("@"); // Basic email validation
                
                // Timestamp should be present and valid
                assertThat(commit.timestamp()).isNotNull();
                assertThat(commit.timestamp()).isBefore(Instant.now().plusSeconds(60)); // Should not be in future
                assertThat(commit.timestamp()).isAfter(Instant.parse("2000-01-01T00:00:00Z")); // Reasonable past date
                
                // Changed files list should be present (can be empty but not null)
                assertThat(commit.changedFiles()).isNotNull();
                
                // Diff stats should be present and valid
                DiffStats stats = commit.stats();
                assertThat(stats).isNotNull();
                assertThat(stats.filesChanged()).isGreaterThanOrEqualTo(0);
                assertThat(stats.insertions()).isGreaterThanOrEqualTo(0);
                assertThat(stats.deletions()).isGreaterThanOrEqualTo(0);
            }
            
            // When getting specific commit details
            String firstCommitHash = history.get(0).hash();
            CommitInfo commitDetails = repositoryService.getCommitDetails(repositoryPath, firstCommitHash);
            
            // Then the detailed commit info should also contain complete metadata
            assertThat(commitDetails).isNotNull();
            
            // Verify all the same metadata requirements for detailed commit
            assertThat(commitDetails.hash()).isNotNull();
            assertThat(commitDetails.hash()).isNotEmpty();
            assertThat(commitDetails.hash()).hasSize(40);
            assertThat(commitDetails.hash()).matches("^[a-f0-9]{40}$");
            
            assertThat(commitDetails.shortHash()).isNotNull();
            assertThat(commitDetails.shortHash()).isNotEmpty();
            assertThat(commitDetails.hash()).startsWith(commitDetails.shortHash());
            
            assertThat(commitDetails.message()).isNotNull();
            
            AuthorInfo detailAuthor = commitDetails.author();
            assertThat(detailAuthor).isNotNull();
            assertThat(detailAuthor.name()).isNotNull();
            assertThat(detailAuthor.name()).isNotEmpty();
            assertThat(detailAuthor.email()).isNotNull();
            assertThat(detailAuthor.email()).isNotEmpty();
            assertThat(detailAuthor.email()).contains("@");
            
            assertThat(commitDetails.timestamp()).isNotNull();
            assertThat(commitDetails.timestamp()).isBefore(Instant.now().plusSeconds(60));
            assertThat(commitDetails.timestamp()).isAfter(Instant.parse("2000-01-01T00:00:00Z"));
            
            assertThat(commitDetails.changedFiles()).isNotNull();
            
            DiffStats detailStats = commitDetails.stats();
            assertThat(detailStats).isNotNull();
            assertThat(detailStats.filesChanged()).isGreaterThanOrEqualTo(0);
            assertThat(detailStats.insertions()).isGreaterThanOrEqualTo(0);
            assertThat(detailStats.deletions()).isGreaterThanOrEqualTo(0);
            
            // The detailed commit should have the same hash as the one from history
            assertThat(commitDetails.hash()).isEqualTo(firstCommitHash);
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    @Provide
    Arbitrary<Path> repositoryWithCommits() {
        return Arbitraries.integers().between(1, 5).map(numCommits -> {
            try {
                Path tempDir = Files.createTempDirectory("git-test-");
                Git git = Git.init().setDirectory(tempDir.toFile()).call();
                
                // Configure author for commits
                PersonIdent author = new PersonIdent("Test Author", "test@example.com");
                
                // Create multiple commits with different content
                for (int i = 1; i <= numCommits; i++) {
                    // Create a file with unique content
                    Path file = tempDir.resolve("file" + i + ".txt");
                    Files.writeString(file, "Content for commit " + i + "\nLine 2\nLine 3");
                    
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
}