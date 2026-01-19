package io.sandboxdev.gitmcpserver.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void shouldListBranches() throws IOException, InterruptedException {
        // Initialize a git repo in tempDir
        new ProcessBuilder("git", "init").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "config", "user.email", "test@example.com").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "config", "user.name", "test").directory(tempDir.toFile()).start().waitFor();
        
        // Create and checkout main
        new ProcessBuilder("git", "checkout", "-b", "main").directory(tempDir.toFile()).start().waitFor();
        
        // Commit something so branch actually exists
        Path testFile = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(testFile, "test");
        new ProcessBuilder("git", "add", ".").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "commit", "-m", "initial").directory(tempDir.toFile()).start().waitFor();

        GitService gitService = new GitService(tempDir);
        List<String> branches = gitService.listBranches();
        
        assertThat(branches).contains("main");
    }

    @Test
    void shouldGetLog() throws IOException, InterruptedException {
        // Initialize a git repo in tempDir
        new ProcessBuilder("git", "init").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "config", "user.email", "test@example.com").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "config", "user.name", "test").directory(tempDir.toFile()).start().waitFor();
        
        Path testFile = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(testFile, "test");
        new ProcessBuilder("git", "add", ".").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "commit", "-m", "initial").directory(tempDir.toFile()).start().waitFor();

        GitService gitService = new GitService(tempDir);
        List<Map<String, String>> log = gitService.getLog(10);
        
        assertThat(log).hasSize(1);
        assertThat(log.get(0).get("message")).isEqualTo("initial");
        assertThat(log.get(0)).containsKey("hash");
        assertThat(log.get(0)).containsKey("author");
        assertThat(log.get(0)).containsKey("date");
    }

    @Test
    void shouldThrowExceptionForInvalidRepo() {
        // tempDir is empty, not a git repo
        GitService gitService = new GitService(tempDir);
        
        // listBranches uses getGit() which should fail
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            gitService.listBranches();
        });
    }

    @Test
    void shouldCheckoutBranch() throws IOException, InterruptedException {
        // Initialize a git repo in tempDir
        new ProcessBuilder("git", "init").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "config", "user.email", "test@example.com").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "config", "user.name", "test").directory(tempDir.toFile()).start().waitFor();
        
        // Create and checkout main
        new ProcessBuilder("git", "checkout", "-b", "main").directory(tempDir.toFile()).start().waitFor();
        
        // Commit something so branch exists
        Path testFile = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(testFile, "test");
        new ProcessBuilder("git", "add", ".").directory(tempDir.toFile()).start().waitFor();
        new ProcessBuilder("git", "commit", "-m", "initial").directory(tempDir.toFile()).start().waitFor();

        // Create a new branch
        new ProcessBuilder("git", "branch", "feature").directory(tempDir.toFile()).start().waitFor();

        GitService gitService = new GitService(tempDir);
        gitService.checkoutBranch("feature");

        // Verify current branch is feature by reading HEAD
        String head = java.nio.file.Files.readString(tempDir.resolve(".git/HEAD")).trim();
        assertThat(head).isEqualTo("ref: refs/heads/feature");
    }
}
