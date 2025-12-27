package io.sandboxdev.gitmcp.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.Optional;

/**
 * Unit tests for Git Tool Schema records (TDD RED phase).
 * Tests the data models for all Git operation tool schemas.
 */
class GitToolSchemasTest {

    @Test
    void shouldCreateGitStatusToolSchema() {
        // Given
        String repositoryPath = "/path/to/repo";

        // When
        GitStatusToolSchema schema = new GitStatusToolSchema(repositoryPath);

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
    }

    @Test
    void shouldCreateGitCommitToolSchema() {
        // Given
        String repositoryPath = "/path/to/repo";
        String message = "Add new feature";

        // When
        GitCommitToolSchema schema = new GitCommitToolSchema(repositoryPath, message);

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.message()).isEqualTo(message);
    }

    @Test
    void shouldCreateGitDiffToolSchemaWithoutRefs() {
        // Given
        String repositoryPath = "/path/to/repo";

        // When
        GitDiffToolSchema schema = new GitDiffToolSchema(repositoryPath, Optional.empty(), Optional.empty(),
                Optional.empty());

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.fromRef()).isEmpty();
        assertThat(schema.toRef()).isEmpty();
    }

    @Test
    void shouldCreateGitDiffToolSchemaWithRefs() {
        // Given
        String repositoryPath = "/path/to/repo";
        String fromRef = "HEAD~1";
        String toRef = "HEAD";

        // When
        GitDiffToolSchema schema = new GitDiffToolSchema(repositoryPath, Optional.of(fromRef), Optional.of(toRef),
                Optional.empty());

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.fromRef()).contains(fromRef);
        assertThat(schema.toRef()).contains(toRef);
    }

    @Test
    void shouldCreateGitBranchListToolSchema() {
        // Given
        String repositoryPath = "/path/to/repo";

        // When
        GitBranchListToolSchema schema = new GitBranchListToolSchema(repositoryPath);

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
    }

    @Test
    void shouldCreateGitBranchCreateToolSchema() {
        // Given
        String repositoryPath = "/path/to/repo";
        String branchName = "feature/new-api";

        // When
        GitBranchCreateToolSchema schema = new GitBranchCreateToolSchema(repositoryPath, branchName);

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.branchName()).isEqualTo(branchName);
    }

    @Test
    void shouldCreateGitCheckoutToolSchema() {
        // Given
        String repositoryPath = "/path/to/repo";
        String branchName = "main";

        // When
        GitCheckoutToolSchema schema = new GitCheckoutToolSchema(repositoryPath, branchName);

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.branchName()).isEqualTo(branchName);
    }

    @Test
    void shouldCreateGitLogToolSchemaWithDefaults() {
        // Given
        String repositoryPath = "/path/to/repo";

        // When
        GitLogToolSchema schema = new GitLogToolSchema(repositoryPath, Optional.empty(), Optional.empty());

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.limit()).isEmpty();
        assertThat(schema.filePath()).isEmpty();
    }

    @Test
    void shouldCreateGitLogToolSchemaWithLimitAndPath() {
        // Given
        String repositoryPath = "/path/to/repo";
        int limit = 10;
        String filePath = "src/main/App.java";

        // When
        GitLogToolSchema schema = new GitLogToolSchema(repositoryPath, Optional.of(limit), Optional.of(filePath));

        // Then
        assertThat(schema.repositoryPath()).isEqualTo(repositoryPath);
        assertThat(schema.limit()).contains(limit);
        assertThat(schema.filePath()).contains(filePath);
    }

    @Test
    void shouldAllSchemasBeImmutable() {
        // Given/When/Then - All schemas should be records (immutable)
        GitStatusToolSchema statusSchema = new GitStatusToolSchema("/repo");
        GitCommitToolSchema commitSchema = new GitCommitToolSchema("/repo", "message");
        GitDiffToolSchema diffSchema = new GitDiffToolSchema("/repo", Optional.empty(), Optional.empty(),
                Optional.empty());
        GitBranchListToolSchema branchListSchema = new GitBranchListToolSchema("/repo");
        GitBranchCreateToolSchema branchCreateSchema = new GitBranchCreateToolSchema("/repo", "branch");
        GitCheckoutToolSchema checkoutSchema = new GitCheckoutToolSchema("/repo", "branch");
        GitLogToolSchema logSchema = new GitLogToolSchema("/repo", Optional.empty(), Optional.empty());

        // All should be non-null and immutable
        assertThat(statusSchema).isNotNull();
        assertThat(commitSchema).isNotNull();
        assertThat(diffSchema).isNotNull();
        assertThat(branchListSchema).isNotNull();
        assertThat(branchCreateSchema).isNotNull();
        assertThat(checkoutSchema).isNotNull();
        assertThat(logSchema).isNotNull();
    }
}
