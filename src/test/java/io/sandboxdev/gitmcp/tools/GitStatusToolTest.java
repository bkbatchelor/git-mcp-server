package io.sandboxdev.gitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.model.GitStatus;
import io.sandboxdev.gitmcp.model.GitStatusToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import io.sandboxdev.gitmcp.security.GitInputValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitStatusToolTest {

    @Mock
    private JGitRepositoryManager repositoryManager;

    @Mock
    private GitInputValidator validator;

    private GitStatusTool gitStatusTool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        gitStatusTool = new GitStatusTool(repositoryManager, objectMapper, validator);
    }

    @Test
    void execute_ValidRepository_ReturnsGitStatus() throws Exception {
        // Arrange
        Path repoPath = Paths.get("/valid/repo");
        GitStatusToolSchema schema = new GitStatusToolSchema(repoPath.toString());
        GitStatus expectedStatus = new GitStatus(
                List.of("modified.txt"),
                List.of("staged.txt"),
                List.of("untracked.txt"),
                false);

        when(repositoryManager.isValidRepository(repoPath)).thenReturn(true);
        when(repositoryManager.getStatus(repoPath)).thenReturn(expectedStatus);

        // Act
        ToolResult result = gitStatusTool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();
        assertThat(result.content()).isNotNull();

        GitStatus actualStatus = objectMapper.convertValue(result.content(), GitStatus.class);
        assertThat(actualStatus.modifiedFiles()).containsExactly("modified.txt");
        assertThat(actualStatus.stagedFiles()).containsExactly("staged.txt");
        assertThat(actualStatus.untrackedFiles()).containsExactly("untracked.txt");
        assertThat(actualStatus.isClean()).isFalse();
    }

    @Test
    void execute_InvalidRepository_ReturnsError() {
        // Arrange
        Path repoPath = Paths.get("/invalid/repo");
        GitStatusToolSchema schema = new GitStatusToolSchema(repoPath.toString());

        when(repositoryManager.isValidRepository(repoPath)).thenReturn(false);

        // Act
        ToolResult result = gitStatusTool.execute(schema);

        // Assert
        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).isPresent();
        assertThat(result.errorMessage().get()).contains("Repository not found");
    }

    @Test
    void execute_CleanRepository_ReturnsCleanStatus() throws Exception {
        // Arrange
        Path repoPath = Paths.get("/clean/repo");
        GitStatusToolSchema schema = new GitStatusToolSchema(repoPath.toString());
        GitStatus expectedStatus = new GitStatus(
                List.of(),
                List.of(),
                List.of(),
                true);

        when(repositoryManager.isValidRepository(repoPath)).thenReturn(true);
        when(repositoryManager.getStatus(repoPath)).thenReturn(expectedStatus);

        // Act
        ToolResult result = gitStatusTool.execute(schema);

        // Assert
        assertThat(result.isError()).isFalse();

        GitStatus actualStatus = objectMapper.convertValue(result.content(), GitStatus.class);
        assertThat(actualStatus.modifiedFiles()).isEmpty();
        assertThat(actualStatus.stagedFiles()).isEmpty();
        assertThat(actualStatus.untrackedFiles()).isEmpty();
        assertThat(actualStatus.isClean()).isTrue();
    }
}
