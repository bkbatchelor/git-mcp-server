package io.sandboxdev.gitmcp.properties;

import io.sandboxdev.gitmcp.model.ResourceContent;
import io.sandboxdev.gitmcp.registry.GitResourceRegistry;
import io.sandboxdev.gitmcp.service.JGitRepositoryManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("git-mcp-server")
@Tag("resource-access")
class ResourceAccessPropertiesTest {

    private final JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
    private final GitResourceRegistry resourceRegistry = new GitResourceRegistry(repositoryManager);

    @RepeatedTest(10)
    void resourceAccessWorkflow() throws Exception {
        Path tempDir = Files.createTempDirectory("git-resource-test");
        try {
            // 1. Init Repo
            setupRepository(tempDir);

            // 2. Construct URI for a file
            // Scheme: git://{absolute_path}/file/{filename}
            // Note: In real implementation we might use a safer scheme, but for now using
            // path
            String repoPath = tempDir.toAbsolutePath().toString();
            String fileUri = "git://" + repoPath + "/file/README.md";

            // 3. Read Resource
            ResourceContent content = resourceRegistry.readResource(fileUri);

            assertThat(content.uri()).isEqualTo(fileUri);
            assertThat(content.mimeType()).isEqualTo("text/plain");
            assertThat(content.text()).isEqualTo("Hello World");

        } finally {
            repositoryManager.closeAll();
            FileSystemUtils.deleteRecursively(tempDir);
        }
    }

    private void setupRepository(Path dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "init");
        pb.directory(dir.toFile());
        pb.start().waitFor();

        runGit(dir, "config", "user.email", "test@example.com");
        runGit(dir, "config", "user.name", "Test User");

        Files.writeString(dir.resolve("README.md"), "Hello World");
        runGit(dir, "add", "README.md");
        runGit(dir, "commit", "-m", "Initial commit");
    }

    private void runGit(Path dir, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git");
        for (String arg : args)
            pb.command().add(arg);
        pb.directory(dir.toFile());
        pb.start().waitFor();
    }
}
