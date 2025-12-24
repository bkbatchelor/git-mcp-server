package io.sandboxdev.gitmcp.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GitBranchInfo record (TDD RED phase).
 * Tests the data model for Git branch information.
 */
class GitBranchInfoTest {

    @Test
    void shouldCreateGitBranchInfoWithAllFields() {
        // Given
        String name = "feature/new-api";
        String commitHash = "a1b2c3d4e5f6789012345678901234567890abcd";
        boolean isCurrent = true;
        boolean isRemote = false;
        
        // When
        GitBranchInfo branch = new GitBranchInfo(name, commitHash, isCurrent, isRemote);
        
        // Then
        assertThat(branch.name()).isEqualTo(name);
        assertThat(branch.commitHash()).isEqualTo(commitHash);
        assertThat(branch.isCurrent()).isTrue();
        assertThat(branch.isRemote()).isFalse();
    }

    @Test
    void shouldCreateRemoteBranch() {
        // Given
        String name = "origin/main";
        String commitHash = "def456789";
        
        // When
        GitBranchInfo branch = new GitBranchInfo(name, commitHash, false, true);
        
        // Then
        assertThat(branch.name()).isEqualTo(name);
        assertThat(branch.isRemote()).isTrue();
        assertThat(branch.isCurrent()).isFalse();
    }

    @Test
    void shouldCreateCurrentLocalBranch() {
        // Given
        String name = "main";
        String commitHash = "abc123456";
        
        // When
        GitBranchInfo branch = new GitBranchInfo(name, commitHash, true, false);
        
        // Then
        assertThat(branch.name()).isEqualTo(name);
        assertThat(branch.isCurrent()).isTrue();
        assertThat(branch.isRemote()).isFalse();
    }

    @Test
    void shouldBeImmutable() {
        // Given
        GitBranchInfo branch = new GitBranchInfo("main", "hash123", true, false);
        
        // When/Then - all fields should be immutable
        assertThat(branch.name()).isNotNull();
        assertThat(branch.commitHash()).isNotNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        GitBranchInfo branch1 = new GitBranchInfo("main", "hash123", true, false);
        GitBranchInfo branch2 = new GitBranchInfo("main", "hash123", true, false);
        
        // Then
        assertThat(branch1).isEqualTo(branch2);
        assertThat(branch1.hashCode()).isEqualTo(branch2.hashCode());
    }
}
