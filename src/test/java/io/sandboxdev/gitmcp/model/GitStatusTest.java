package io.sandboxdev.gitmcp.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.List;

/**
     Unit tests for GitStatus record (TDD RED phase).
     Tests the data model for Git repository status information.
 */
class GitStatusTest {

    /**
     Property 4: Git Status Operations (Req 3.1)
     Validates GitStatus model creation and properties
     */
    @Test
    void shouldCreateGitStatusWithAllFileTypes() {
        // Given
        List<String> modified = List.of("src/main/App.java", "README.md");
        List<String> staged = List.of("src/test/AppTest.java");
        List<String> untracked = List.of("temp.txt", "logs/debug.log");
        
        // When
        GitStatus status = new GitStatus(modified, staged, untracked, false);
        
        // Then
        assertThat(status.modifiedFiles()).isEqualTo(modified);
        assertThat(status.stagedFiles()).isEqualTo(staged);
        assertThat(status.untrackedFiles()).isEqualTo(untracked);
        assertThat(status.isClean()).isFalse();
    }

    /**
     Property 4: Git Status Operations (Req 3.1)
     Validates GitStatus model creation and properties
     */
    @Test
    void shouldCreateCleanGitStatus() {
        // Given
        List<String> emptyList = List.of();
        
        // When
        GitStatus status = new GitStatus(emptyList, emptyList, emptyList, true);
        
        // Then
        assertThat(status.modifiedFiles()).isEmpty();
        assertThat(status.stagedFiles()).isEmpty();
        assertThat(status.untrackedFiles()).isEmpty();
        assertThat(status.isClean()).isTrue();
    }

    /**
     Property 4: Git Status Operations (Req 3.1)
     Validates GitStatus model creation and properties
     */
    @Test
    void shouldBeImmutable() {
        // Given
        List<String> modified = List.of("file1.java");
        GitStatus status = new GitStatus(modified, List.of(), List.of(), false);
        
        // When/Then - should not be able to modify the lists
        assertThat(status.modifiedFiles()).isUnmodifiable();
        assertThat(status.stagedFiles()).isUnmodifiable();
        assertThat(status.untrackedFiles()).isUnmodifiable();
    }

    /**
     Property 4: Git Status Operations (Req 3.1)
     Validates GitStatus model creation and properties
     */
    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        List<String> modified = List.of("file1.java");
        GitStatus status1 = new GitStatus(modified, List.of(), List.of(), false);
        GitStatus status2 = new GitStatus(modified, List.of(), List.of(), false);
        
        // Then
        assertThat(status1).isEqualTo(status2);
        assertThat(status1.hashCode()).isEqualTo(status2.hashCode());
    }
}
