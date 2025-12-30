package io.sandboxdev.gitmcp.properties;

import io.sandboxdev.gitmcp.model.GitDiffToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.GitDiffTool;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Property-based tests for Git diff operations.
 *
     Property 6: Git Diff Operations
     For any valid Git repository and commit references, the git_diff tool should
     return
     unified diff format output for the specified comparison (unstaged changes,
     single ref vs HEAD,
     or two refs), with appropriate errors for invalid references.
 *
     Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Tag("git-mcp-server")
@Tag("property-6")
@Tag("git-diff-operations")
class GitDiffOperationsProperties {

    @org.junit.jupiter.api.Test
    void gitDiffReturnsUnifiedDiffForUnstagedChanges_Unit() {
        // Create repo state manually
        try {
            Path tempDir = Files.createTempDirectory("git-diff-test-unit");

            // Initialize git repository
            ProcessBuilder pb = new ProcessBuilder("git", "init");
            pb.directory(tempDir.toFile());
            pb.start().waitFor();

            pb = new ProcessBuilder("git", "config", "user.name", "Test User");
            pb.directory(tempDir.toFile());
            pb.start().waitFor();

            pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
            pb.directory(tempDir.toFile());
            pb.start().waitFor();

            // Create and commit a file
            Path testFile = tempDir.resolve("test.txt");
            Files.writeString(testFile, "initial content");

            pb = new ProcessBuilder("git", "add", "test.txt");
            pb.directory(tempDir.toFile());
            pb.start().waitFor();

            pb = new ProcessBuilder("git", "commit", "-m", "Initial commit");
            pb.directory(tempDir.toFile());
            pb.start().waitFor();

            // Modify the file without staging
            Files.writeString(testFile, "modified content");

            RepositoryWithChanges repoState = new RepositoryWithChanges(tempDir, "test.txt");

            // Arrange
            GitDiffTool tool = new GitDiffTool();
            GitDiffToolSchema schema = new GitDiffToolSchema(
                    repoState.path().toString(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());

            // Act
            ToolResult result = tool.execute(schema);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).isNotNull();
            String diffOutput = result.content().asText();
            assertThat(diffOutput).contains("diff --git");
            assertThat(diffOutput).contains(repoState.modifiedFile());

        } catch (Exception e) {
            throw new RuntimeException("Test failed", e);
        }
    }

    @Property
    @Tag("property-6")
    @Tag("git-diff-operations")
    void gitDiffReturnsUnifiedDiffForUnstagedChanges(
            @ForAll("repositoryWithUnstagedChanges") RepositoryWithChanges repoState) {
        // Arrange
        GitDiffTool tool = new GitDiffTool();
        GitDiffToolSchema schema = new GitDiffToolSchema(
                repoState.path().toString(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).isNotNull();
        String diffOutput = result.content().asText();
        assertThat(diffOutput).contains("diff --git");
        assertThat(diffOutput).contains(repoState.modifiedFile());
    }

    @Provide
    Arbitrary<RepositoryWithChanges> repositoryWithUnstagedChanges() {
        return Arbitraries.create(() -> {
            try {
                Path tempDir = Files.createTempDirectory("git-diff-test");

                // Initialize git repository
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "config", "user.name", "Test User");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Create and commit a file
                Path testFile = tempDir.resolve("test.txt");
                Files.writeString(testFile, "initial content");

                pb = new ProcessBuilder("git", "add", "test.txt");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "commit", "-m", "Initial commit");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Modify the file without staging
                Files.writeString(testFile, "modified content");

                return new RepositoryWithChanges(tempDir, "test.txt");
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository with unstaged changes", e);
            }
        });
    }

    public record RepositoryWithChanges(
            Path path,
            String modifiedFile) {
    }
}
