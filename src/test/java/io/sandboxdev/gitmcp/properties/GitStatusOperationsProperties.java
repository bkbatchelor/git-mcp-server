package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitStatus;
import io.sandboxdev.gitmcp.model.GitStatusToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.GitStatusTool;
import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Property-based tests for Git status operations.
     
     Property 4: Git Status Operations
     For any valid Git repository, the git_status tool should return accurate
     working tree status
     with files correctly categorized as modified, staged, or untracked, and
     return appropriate
     errors for invalid repository paths.
     
     Validates: Requirements 3.1, 3.2, 3.4
 */
@Tag("git-mcp-server")
@Tag("property-4")
@Tag("git-status-operations")
class GitStatusOperationsProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Property
    @Tag("property-4")
    @Tag("git-status-operations")
    void gitStatusReturnsAccurateWorkingTreeStatus(@ForAll("validGitRepositories") Path repositoryPath) {
        // Arrange
        GitStatusTool tool = new GitStatusTool();
        GitStatusToolSchema schema = new GitStatusToolSchema(repositoryPath.toString());

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).isNotNull();

        // Deserialize JSON content to GitStatus
        GitStatus status = objectMapper.convertValue(result.content(), GitStatus.class);
        assertThat(status.modifiedFiles()).isNotNull();
        assertThat(status.stagedFiles()).isNotNull();
        assertThat(status.untrackedFiles()).isNotNull();

        // Verify clean status consistency
        boolean hasChanges = !status.modifiedFiles().isEmpty() ||
                !status.stagedFiles().isEmpty() ||
                !status.untrackedFiles().isEmpty();
        assertThat(status.isClean()).isEqualTo(!hasChanges);
    }

    @Property
    @Tag("property-4")
    @Tag("git-status-operations")
    void gitStatusReturnsErrorForInvalidRepositoryPath(@ForAll("invalidRepositoryPaths") String invalidPath) {
        // Arrange
        GitStatusTool tool = new GitStatusTool();
        GitStatusToolSchema schema = new GitStatusToolSchema(invalidPath);

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("Repository not found");
    }

    @Property
    @Tag("property-4")
    @Tag("git-status-operations")
    void gitStatusCategorizesFilesCorrectly(@ForAll("repositoryWithKnownChanges") RepositoryState repoState) {
        // Arrange
        GitStatusTool tool = new GitStatusTool();
        GitStatusToolSchema schema = new GitStatusToolSchema(repoState.path().toString());

        // Act
        ToolResult result = tool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();

        // Deserialize JSON content to GitStatus
        GitStatus status = objectMapper.convertValue(result.content(), GitStatus.class);

        // Verify expected files are in correct categories
        assertThat(status.modifiedFiles()).containsExactlyInAnyOrderElementsOf(repoState.expectedModified());
        assertThat(status.stagedFiles()).containsExactlyInAnyOrderElementsOf(repoState.expectedStaged());
        assertThat(status.untrackedFiles()).containsExactlyInAnyOrderElementsOf(repoState.expectedUntracked());
    }

    @Provide
    Arbitrary<Path> validGitRepositories() {
        return Arbitraries.create(() -> {
            try {
                Path tempDir = Files.createTempDirectory("git-test");
                // Initialize git repository
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();
                return tempDir;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository", e);
            }
        });
    }

    @Provide
    Arbitrary<String> invalidRepositoryPaths() {
        return Arbitraries.oneOf(
                Arbitraries.just("/nonexistent/path"),
                Arbitraries.just(""),
                Arbitraries.just("/tmp/not-a-git-repo"),
                Arbitraries.just("../../../etc/passwd"));
    }

    @Provide
    Arbitrary<RepositoryState> repositoryWithKnownChanges() {
        return Arbitraries.create(() -> {
            try {
                Path tempDir = Files.createTempDirectory("git-test");

                // Initialize git repository
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Create and stage a file
                Path stagedFile = tempDir.resolve("staged.txt");
                Files.writeString(stagedFile, "staged content");
                pb = new ProcessBuilder("git", "add", "staged.txt");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();

                // Create a modified file
                Path modifiedFile = tempDir.resolve("modified.txt");
                Files.writeString(modifiedFile, "initial content");
                pb = new ProcessBuilder("git", "add", "modified.txt");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();
                pb = new ProcessBuilder("git", "commit", "-m", "initial commit");
                pb.directory(tempDir.toFile());
                pb.start().waitFor();
                Files.writeString(modifiedFile, "modified content");

                // Create an untracked file
                Path untrackedFile = tempDir.resolve("untracked.txt");
                Files.writeString(untrackedFile, "untracked content");

                return new RepositoryState(
                        tempDir,
                        List.of("modified.txt"),
                        List.of("staged.txt"),
                        List.of("untracked.txt"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository with changes", e);
            }
        });
    }

    public record RepositoryState(
            Path path,
            List<String> expectedModified,
            List<String> expectedStaged,
            List<String> expectedUntracked) {
    }
}
