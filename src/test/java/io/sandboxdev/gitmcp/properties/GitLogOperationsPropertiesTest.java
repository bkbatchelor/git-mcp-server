package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.sandboxdev.gitmcp.model.GitCommitInfo;
import io.sandboxdev.gitmcp.model.GitLogToolSchema;
import io.sandboxdev.gitmcp.model.ToolResult;
import io.sandboxdev.gitmcp.tools.GitLogTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("git-mcp-server")
@Tag("git-log-operations")
class GitLogOperationsPropertiesTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        @RepeatedTest(10)
        @DisplayName("Property 8: Git Log Operations - Returns commit history with limit and file filtering (Req 7.1, 7.2, 7.3)")
        void gitLogOperationsWorkflow() throws Exception {
                // Setup repo with history
                Path repositoryPath = createInitializedRepositoryWithHistory();
                GitLogTool logTool = new GitLogTool();

        // 1. Full Log
        ToolResult result = logTool.execute(new GitLogToolSchema(
                repositoryPath.toString(), Optional.empty(), Optional.empty()));
        if (result.isError()) {
            System.err.println("Git Log Error: " + result.errorMessage().orElse("Unknown error"));
        }
        assertThat(result.isError()).isFalse();
        List<GitCommitInfo> fullHistory = objectMapper.convertValue(
                result.content(), new TypeReference<List<GitCommitInfo>>() {
                });
        assertThat(fullHistory).hasSize(3); // Initial + 2 updates
        assertThat(fullHistory.get(0).message()).contains("Update 2"); // Reverse chronological

        // 2. Limit Log
        result = logTool.execute(new GitLogToolSchema(
                repositoryPath.toString(), Optional.of(1), Optional.empty()));
        assertThat(result.isError()).isFalse();
        List<GitCommitInfo> limitedHistory = objectMapper.convertValue(
                result.content(), new TypeReference<List<GitCommitInfo>>() {
                });
        assertThat(limitedHistory).hasSize(1);
        assertThat(limitedHistory.get(0).message()).contains("Update 2");

        // 3. File Log
        result = logTool.execute(new GitLogToolSchema(
                repositoryPath.toString(), Optional.empty(), Optional.of("file1.txt")));
        assertThat(result.isError()).isFalse();
        List<GitCommitInfo> fileHistory = objectMapper.convertValue(
                result.content(), new TypeReference<List<GitCommitInfo>>() {
                });
        // file1.txt was committed in Initial and Update 1, but NOT Update 2 (which was
        // file2.txt)
        assertThat(fileHistory).hasSize(2)
                .extracting(info -> info.message().trim())
                .containsExactly("Update 1", "Initial commit");
    }

    private Path createInitializedRepositoryWithHistory() throws Exception {
        Path tempDir = Files.createTempDirectory("git-log-test");
        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(tempDir.toFile());
        pb.start().waitFor();

        // Config
        runGit(tempDir, "config", "user.email", "test@example.com");
        runGit(tempDir, "config", "user.name", "Test User");

        // Commit 1: Initial
        Files.writeString(tempDir.resolve("file1.txt"), "v1");
        runGit(tempDir, "add", "file1.txt");
        runGit(tempDir, "commit", "-m", "Initial commit");

        // Commit 2: Update 1 (modifies file1)
        Files.writeString(tempDir.resolve("file1.txt"), "v2");
        runGit(tempDir, "add", "file1.txt");
        runGit(tempDir, "commit", "-m", "Update 1");

        // Commit 3: Update 2 (adds file2, touches file1 not)
        Files.writeString(tempDir.resolve("file2.txt"), "new file");
        runGit(tempDir, "add", "file2.txt");
        runGit(tempDir, "commit", "-m", "Update 2");

        return tempDir;
    }

    private void runGit(Path dir, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git");
        for (String arg : args)
            pb.command().add(arg);
        pb.directory(dir.toFile());
        pb.start().waitFor();
    }
}
