package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitCommitInfo;
import io.sandboxdev.gitmcp.model.GitCommitToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
     Unit tests for GitCommitTool.
 */
class GitCommitToolTest {

    private GitCommitTool gitCommitTool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JSR310 module for Java 8 time types
        gitCommitTool = new GitCommitTool(repositoryManager, objectMapper);
    }

    /**
     Property 5: Git Commit Operations (Req 4.1, 4.3, 4.4)
     Validates Git commit tool execution and error handling
     */
    @Test
    void executeCreatesCommitWithStagedChanges(@TempDir Path tempDir) throws Exception {
        // Arrange - Create git repository with staged changes
        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(tempDir.toFile());
        pb.start().waitFor();

        // Configure git user
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

        GitCommitToolSchema schema = new GitCommitToolSchema(tempDir.toString(), "Initial commit");

        // Act
        ToolResult result = gitCommitTool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).isNotNull();

        GitCommitInfo commitInfo = objectMapper.convertValue(result.content(), GitCommitInfo.class);
        assertThat(commitInfo.hash()).isNotNull().hasSize(40);
        assertThat(commitInfo.shortHash()).isNotNull().hasSizeBetween(7, 12);
        assertThat(commitInfo.message()).isEqualTo("Initial commit");
        assertThat(commitInfo.author()).isEqualTo("Test User");
        assertThat(commitInfo.email()).isEqualTo("test@example.com");
        assertThat(commitInfo.timestamp()).isNotNull();
    }

    /**
     Property 5: Git Commit Operations (Req 4.1, 4.3, 4.4)
     Validates Git commit tool execution and error handling
     */
    @Test
    void executeReturnsErrorForRepositoryWithoutStagedChanges(@TempDir Path tempDir) throws Exception {
        // Arrange - Create empty git repository
        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(tempDir.toFile());
        pb.start().waitFor();

        // Configure git user
        pb = new ProcessBuilder("git", "config", "user.name", "Test User");
        pb.directory(tempDir.toFile());
        pb.start().waitFor();

        pb = new ProcessBuilder("git", "config", "user.email", "test@example.com");
        pb.directory(tempDir.toFile());
        pb.start().waitFor();

        GitCommitToolSchema schema = new GitCommitToolSchema(tempDir.toString(), "Empty commit");

        // Act
        ToolResult result = gitCommitTool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("nothing to commit");
    }

    /**
     Property 5: Git Commit Operations (Req 4.1, 4.3, 4.4)
     Validates Git commit tool execution and error handling
     */
    @Test
    void executeReturnsErrorForInvalidCommitMessage(@TempDir Path tempDir) throws Exception {
        // Arrange - Create git repository with staged changes
        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(tempDir.toFile());
        pb.start().waitFor();

        // Configure git user
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

        GitCommitToolSchema schema = new GitCommitToolSchema(tempDir.toString(), ""); // Empty message

        // Act
        ToolResult result = gitCommitTool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("Invalid commit message");
    }

    /**
     Property 5: Git Commit Operations (Req 4.1, 4.3, 4.4)
     Validates Git commit tool execution and error handling
     */
    @Test
    void executeReturnsErrorForInvalidRepositoryPath() {
        // Arrange
        GitCommitToolSchema schema = new GitCommitToolSchema("/nonexistent/path", "Test commit");

        // Act
        ToolResult result = gitCommitTool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("Repository not found");
    }
}
