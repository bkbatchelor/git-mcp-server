package io.sandboxdev.gitmcp.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.time.Instant;

/**
 * Unit tests for GitCommitInfo record (TDD RED phase).
 * Tests the data model for Git commit information.
 */
class GitCommitInfoTest {

    @Test
    void shouldCreateGitCommitInfoWithAllFields() {
        // Given
        String hash = "a1b2c3d4e5f6789012345678901234567890abcd";
        String shortHash = "a1b2c3d";
        String author = "John Doe";
        String email = "john.doe@example.com";
        Instant timestamp = Instant.parse("2023-12-24T10:30:00Z");
        String message = "Add new feature implementation";
        
        // When
        GitCommitInfo commit = new GitCommitInfo(hash, shortHash, author, email, timestamp, message);
        
        // Then
        assertThat(commit.hash()).isEqualTo(hash);
        assertThat(commit.shortHash()).isEqualTo(shortHash);
        assertThat(commit.author()).isEqualTo(author);
        assertThat(commit.email()).isEqualTo(email);
        assertThat(commit.timestamp()).isEqualTo(timestamp);
        assertThat(commit.message()).isEqualTo(message);
    }

    @Test
    void shouldHandleMultilineCommitMessage() {
        // Given
        String message = "Fix critical bug\n\nThis commit addresses the issue where...\n\nCloses #123";
        
        // When
        GitCommitInfo commit = new GitCommitInfo(
            "hash123", "hash123", "author", "email@test.com", Instant.now(), message
        );
        
        // Then
        assertThat(commit.message()).isEqualTo(message);
        assertThat(commit.message()).contains("\n");
    }

    @Test
    void shouldBeImmutable() {
        // Given
        GitCommitInfo commit = new GitCommitInfo(
            "hash", "short", "author", "email", Instant.now(), "message"
        );
        
        // When/Then - all fields should be immutable
        assertThat(commit.hash()).isNotNull();
        assertThat(commit.shortHash()).isNotNull();
        assertThat(commit.author()).isNotNull();
        assertThat(commit.email()).isNotNull();
        assertThat(commit.timestamp()).isNotNull();
        assertThat(commit.message()).isNotNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        Instant timestamp = Instant.now();
        GitCommitInfo commit1 = new GitCommitInfo("hash", "short", "author", "email", timestamp, "message");
        GitCommitInfo commit2 = new GitCommitInfo("hash", "short", "author", "email", timestamp, "message");
        
        // Then
        assertThat(commit1).isEqualTo(commit2);
        assertThat(commit1.hashCode()).isEqualTo(commit2.hashCode());
    }
}
