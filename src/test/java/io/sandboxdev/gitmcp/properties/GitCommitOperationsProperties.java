package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitCommitInfo;
import io.sandboxdev.gitmcp.model.GitCommitToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.GitCommitTool;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Property-based tests for Git commit operations.

     Property 5: Git Commit Operations
     For any valid Git repository with staged changes, the git_commit tool should
     create a commit
     with the provided message and return commit information, and return
     appropriate errors for
     repositories without staged changes or invalid commit messages.

     Validates: Requirements 4.1, 4.3, 4.4
 */
@Tag("git-mcp-server")
@Tag("property-5")
@Tag("git-commit-operations")
class GitCommitOperationsProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     Property 5: Git Commit Operations (Req 4.1, 4.3)
     Creates commit with staged changes
     */
    @Property
    @Tag("property-5")
    @Tag("git-commit-operations")
    void gitCommitCreatesCommitWithStagedChanges(@ForAll("repositoryWithStagedChanges") RepositoryWithChanges repoState,
            @ForAll("validCommitMessages") String commitMessage) {
        // Arrange
        GitCommitTool tool = new GitCommitTool();
        GitCommitToolSchema schema = new GitCommitToolSchema(repoState.path().toString(), commitMessage);

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).isNotNull();

        // Deserialize JSON content to GitCommitInfo
        GitCommitInfo commitInfo = objectMapper.convertValue(result.content(), GitCommitInfo.class);
        assertThat(commitInfo.hash()).isNotNull().hasSize(40); // Full SHA-1 hash
        assertThat(commitInfo.shortHash()).isNotNull().hasSizeBetween(7, 12);
        assertThat(commitInfo.message()).isEqualTo(commitMessage);
        assertThat(commitInfo.author()).isNotNull();
        assertThat(commitInfo.email()).isNotNull();
        assertThat(commitInfo.timestamp()).isNotNull();
    }

    @Property
    @Tag("property-5")
    @Tag("git-commit-operations")
    void gitCommitReturnsErrorForRepositoryWithoutStagedChanges(
            @ForAll("repositoryWithoutStagedChanges") Path repositoryPath,
            @ForAll("validCommitMessages") String commitMessage) {
        // Arrange
        GitCommitTool tool = new GitCommitTool();
        GitCommitToolSchema schema = new GitCommitToolSchema(repositoryPath.toString(), commitMessage);

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("nothing to commit");
    }

    @Property
    @Tag("property-5")
    @Tag("git-commit-operations")
    void gitCommitReturnsErrorForInvalidCommitMessage(
            @ForAll("repositoryWithStagedChanges") RepositoryWithChanges repoState,
            @ForAll("invalidCommitMessages") String invalidMessage) {
        // Arrange
        GitCommitTool tool = new GitCommitTool();
        GitCommitToolSchema schema = new GitCommitToolSchema(repoState.path().toString(), invalidMessage);

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("Invalid commit message");
    }

    @Provide
    Arbitrary<RepositoryWithChanges> repositoryWithStagedChanges() {
        return Arbitraries.create(() -> {
            try {
                Path tempDir = Files.createTempDirectory("git-commit-test");

                // Initialize git repository
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Configure git user for commits
                pb = new ProcessBuilder("git", "config", "user.name", "Test User");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Create and stage a file
                Path testFile = tempDir.resolve("test.txt");
                Files.writeString(testFile, "test content");
                pb = new ProcessBuilder("git", "add", "test.txt");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                return new RepositoryWithChanges(tempDir, "test.txt");
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository with staged changes", e);
            }
        });
    }

    @Provide
    Arbitrary<Path> repositoryWithoutStagedChanges() {
        return Arbitraries.create(() -> {
            try {
                Path tempDir = Files.createTempDirectory("git-commit-test");

                // Initialize git repository
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Configure git user for commits
                pb = new ProcessBuilder("git", "config", "user.name", "Test User");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                return tempDir;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository without staged changes", e);
            }
        });
    }

    @Provide
    Arbitrary<String> validCommitMessages() {
        return Arbitraries.oneOf(
                Arbitraries.just("Initial commit"),
                Arbitraries.just("Add new feature"),
                Arbitraries.just("Fix bug in authentication"),
                Arbitraries.just("Update documentation"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(10).ofMaxLength(50));
    }

    @Provide
    Arbitrary<String> invalidCommitMessages() {
        return Arbitraries.oneOf(
                Arbitraries.just(""), // Empty message
                Arbitraries.just("   "), // Whitespace only
                Arbitraries.just("\n\n\n"), // Newlines only
                Arbitraries.strings().withCharRange('\u0000', '\u001F').ofMinLength(1).ofMaxLength(10) // Control
                                                                                                       // characters
        );
    }

    public record RepositoryWithChanges(
            Path path,
            String stagedFile) {
    }
}
