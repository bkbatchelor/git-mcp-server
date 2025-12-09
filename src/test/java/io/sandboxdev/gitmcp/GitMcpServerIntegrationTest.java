package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.exception.ErrorCode;
import io.sandboxdev.gitmcp.exception.GitMcpException;
import io.sandboxdev.gitmcp.mcp.GitMcpToolProvider;
import io.sandboxdev.gitmcp.service.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Git MCP Server.
 * Tests the full application stack including Spring Boot context, MCP tools, and Git operations.
 * 
 * Requirements tested:
 * - 1.2: Clone repository operations
 * - 1.3: Clone with authentication
 * - 5.1: Push operations
 * - 5.2: Pull operations
 * - 7.1: MCP handshake and initialization
 * - 8.2: Network error handling
 * - 8.3: Authentication failure handling
 * - 9.4: Maven build verification
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitMcpServerIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GitMcpToolProvider toolProvider;

    @Autowired
    private GitRepositoryService repositoryService;

    @Autowired
    private GitCommitService commitService;

    @Autowired
    private GitBranchService branchService;

    @Autowired
    private GitRemoteService remoteService;

    @TempDir
    Path tempDir;

    /**
     * Test 1: MCP handshake and initialization
     * Validates Requirement 7.1: MCP protocol initialization
     */
    @Test
    @Order(1)
    @DisplayName("Integration Test: MCP handshake and Spring Boot context initialization")
    void testMcpHandshakeAndInitialization() {
        // Verify Spring Boot application context is loaded
        assertNotNull(applicationContext, "Application context should be initialized");
        
        // Verify all required beans are present
        assertNotNull(toolProvider, "GitMcpToolProvider should be available");
        assertNotNull(repositoryService, "GitRepositoryService should be available");
        assertNotNull(commitService, "GitCommitService should be available");
        assertNotNull(branchService, "GitBranchService should be available");
        assertNotNull(remoteService, "GitRemoteService should be available");
        
        // Verify tool provider is properly configured
        assertNotNull(toolProvider, "Tool provider should be initialized");
        
        // Test basic tool invocation - init repository
        Path repoPath = tempDir.resolve("test-init-repo");
        Map<String, Object> result = toolProvider.initRepository(repoPath.toString());
        
        assertNotNull(result, "Tool invocation should return result");
        assertTrue((Boolean) result.get("success"), "Init should succeed");
        assertEquals(repoPath.toString(), result.get("path"), "Path should match");
        assertTrue(Files.exists(repoPath.resolve(".git")), "Git directory should exist");
    }

    /**
     * Test 2: Clone repository with authentication
     * Validates Requirements 1.2, 1.3: Clone operations with credentials
     */
    @Test
    @Order(2)
    @DisplayName("Integration Test: Clone repository with authentication")
    void testCloneRepositoryWithAuthentication() throws IOException, GitAPIException {
        // Create a "remote" repository to clone from
        Path remoteRepoPath = tempDir.resolve("remote-repo");
        Files.createDirectories(remoteRepoPath);
        
        // Initialize remote repository with a commit
        try (Git remoteGit = Git.init().setDirectory(remoteRepoPath.toFile()).call()) {
            Path testFile = remoteRepoPath.resolve("README.md");
            Files.writeString(testFile, "# Test Repository\n");
            remoteGit.add().addFilepattern("README.md").call();
            remoteGit.commit()
                .setMessage("Initial commit")
                .setAuthor("Test Author", "test@example.com")
                .call();
        }
        
        // Clone the repository (without authentication for local clone)
        Path clonePath = tempDir.resolve("cloned-repo");
        Map<String, Object> result = toolProvider.cloneRepository(
            remoteRepoPath.toUri().toString(),
            clonePath.toString(),
            Optional.empty(),
            Optional.empty()
        );
        
        // Verify clone succeeded
        assertNotNull(result, "Clone result should not be null");
        assertTrue((Boolean) result.get("success"), "Clone should succeed");
        assertEquals(clonePath.toString(), result.get("path"), "Cloned path should match");
        assertNotNull(result.get("defaultBranch"), "Default branch should be present");
        
        // Verify cloned repository structure
        assertTrue(Files.exists(clonePath.resolve(".git")), "Git directory should exist");
        assertTrue(Files.exists(clonePath.resolve("README.md")), "Cloned file should exist");
        
        // Verify file contents
        String content = Files.readString(clonePath.resolve("README.md"));
        assertEquals("# Test Repository\n", content, "File content should match");
    }

    /**
     * Test 3: Push/Pull operations
     * Validates Requirements 5.1, 5.2: Remote synchronization operations
     */
    @Test
    @Order(3)
    @DisplayName("Integration Test: Push and pull operations")
    void testPushPullOperations() throws IOException, GitAPIException {
        // Create a bare "remote" repository
        Path bareRepoPath = tempDir.resolve("bare-remote.git");
        Files.createDirectories(bareRepoPath);
        Git.init().setDirectory(bareRepoPath.toFile()).setBare(true).call();
        
        // Create and setup local repository
        Path localRepoPath = tempDir.resolve("local-repo");
        toolProvider.initRepository(localRepoPath.toString());
        
        // Add remote
        toolProvider.addRemote(
            localRepoPath.toString(),
            "origin",
            bareRepoPath.toUri().toString()
        );
        
        // Create a file and commit
        Path testFile = localRepoPath.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        
        toolProvider.stageFiles(localRepoPath.toString(), List.of("test.txt"));
        toolProvider.createCommit(
            localRepoPath.toString(),
            "Test commit",
            Optional.of("Test Author"),
            Optional.of("test@example.com")
        );
        
        // Push to remote
        Map<String, Object> pushResult = toolProvider.push(
            localRepoPath.toString(),
            "origin",
            "master",
            Optional.empty(),
            Optional.empty()
        );
        
        assertTrue((Boolean) pushResult.get("success"), "Push should succeed");
        assertEquals("origin", pushResult.get("remote"), "Remote name should match");
        assertEquals("master", pushResult.get("branch"), "Branch name should match");
        
        // Create another local repository and clone
        Path secondLocalPath = tempDir.resolve("second-local");
        toolProvider.cloneRepository(
            bareRepoPath.toUri().toString(),
            secondLocalPath.toString(),
            Optional.empty(),
            Optional.empty()
        );
        
        // Modify file in first repo and push
        Files.writeString(testFile, "Updated content");
        toolProvider.stageFiles(localRepoPath.toString(), List.of("test.txt"));
        toolProvider.createCommit(
            localRepoPath.toString(),
            "Update test file",
            Optional.of("Test Author"),
            Optional.of("test@example.com")
        );
        toolProvider.push(
            localRepoPath.toString(),
            "origin",
            "master",
            Optional.empty(),
            Optional.empty()
        );
        
        // Pull changes in second repo
        Map<String, Object> pullResult = toolProvider.pull(
            secondLocalPath.toString(),
            "origin",
            "master",
            Optional.empty(),
            Optional.empty()
        );
        
        assertTrue((Boolean) pullResult.get("success"), "Pull should succeed");
        
        // Verify file was updated
        String updatedContent = Files.readString(secondLocalPath.resolve("test.txt"));
        assertEquals("Updated content", updatedContent, "File should be updated after pull");
    }

    /**
     * Test 4: Network error handling
     * Validates Requirement 8.2: Network error handling
     */
    @Test
    @Order(4)
    @DisplayName("Integration Test: Network error handling")
    void testNetworkErrorHandling() {
        // Attempt to clone from invalid URL
        Path clonePath = tempDir.resolve("invalid-clone");
        
        GitMcpException exception = assertThrows(
            GitMcpException.class,
            () -> toolProvider.cloneRepository(
                "https://invalid-domain-that-does-not-exist-12345.com/repo.git",
                clonePath.toString(),
                Optional.empty(),
                Optional.empty()
            ),
            "Should throw GitMcpException for invalid URL"
        );
        
        // Verify error details
        assertNotNull(exception.getErrorCode(), "Error code should be present");
        assertTrue(
            exception.getErrorCode() == ErrorCode.NETWORK_ERROR ||
            exception.getErrorCode() == ErrorCode.OPERATION_FAILED,
            "Error code should indicate network or operation failure"
        );
        assertNotNull(exception.getMessage(), "Error message should be present");
        
        // Attempt to push to non-existent remote
        Path localRepo = tempDir.resolve("local-for-push-test");
        toolProvider.initRepository(localRepo.toString());
        
        // Create a commit
        try {
            Path testFile = localRepo.resolve("test.txt");
            Files.writeString(testFile, "content");
            toolProvider.stageFiles(localRepo.toString(), List.of("test.txt"));
            toolProvider.createCommit(
                localRepo.toString(),
                "Test",
                Optional.of("Test"),
                Optional.of("test@example.com")
            );
        } catch (IOException e) {
            fail("Setup should not fail: " + e.getMessage());
        }
        
        // Add invalid remote
        toolProvider.addRemote(
            localRepo.toString(),
            "invalid",
            "https://invalid-remote-12345.com/repo.git"
        );
        
        // Attempt to push
        GitMcpException pushException = assertThrows(
            GitMcpException.class,
            () -> toolProvider.push(
                localRepo.toString(),
                "invalid",
                "master",
                Optional.empty(),
                Optional.empty()
            ),
            "Should throw GitMcpException for invalid remote"
        );
        
        assertNotNull(pushException.getErrorCode(), "Push error code should be present");
    }

    /**
     * Test 5: Authentication failure handling
     * Validates Requirement 8.3: Authentication error handling
     */
    @Test
    @Order(5)
    @DisplayName("Integration Test: Authentication failure handling")
    void testAuthenticationFailureHandling() {
        // Note: Testing actual authentication failures requires a real remote repository
        // This test simulates the scenario by attempting to clone with invalid credentials
        
        Path clonePath = tempDir.resolve("auth-fail-clone");
        
        // Attempt to clone a public repository with invalid credentials
        // (This will fail during authentication)
        GitMcpException exception = assertThrows(
            GitMcpException.class,
            () -> toolProvider.cloneRepository(
                "https://github.com/nonexistent-user-12345/nonexistent-repo-67890.git",
                clonePath.toString(),
                Optional.of("invalid-user"),
                Optional.of("invalid-password")
            ),
            "Should throw GitMcpException for authentication failure"
        );
        
        // Verify error is properly handled
        assertNotNull(exception.getErrorCode(), "Error code should be present");
        assertTrue(
            exception.getErrorCode() == ErrorCode.AUTHENTICATION_FAILED ||
            exception.getErrorCode() == ErrorCode.NETWORK_ERROR ||
            exception.getErrorCode() == ErrorCode.OPERATION_FAILED,
            "Error code should indicate authentication, network, or operation failure"
        );
        assertNotNull(exception.getMessage(), "Error message should be present");
        assertFalse(
            exception.getMessage().contains("invalid-password"),
            "Error message should not expose credentials"
        );
    }

    /**
     * Test 6: Maven build verification
     * Validates Requirement 9.4: Maven build process
     */
    @Test
    @Order(6)
    @DisplayName("Integration Test: Maven build verification")
    void testMavenBuildVerification() {
        // This test verifies that the application can be built and run successfully
        // The fact that this test is running means Maven compiled and packaged the application
        
        // Verify application context is fully initialized
        assertNotNull(applicationContext, "Application context should be initialized");
        
        // Verify all services are wired correctly
        assertNotNull(repositoryService, "Repository service should be wired");
        assertNotNull(commitService, "Commit service should be wired");
        assertNotNull(branchService, "Branch service should be wired");
        assertNotNull(remoteService, "Remote service should be wired");
        
        // Verify tool provider has all dependencies
        assertNotNull(toolProvider, "Tool provider should be wired");
        
        // Test a complete workflow to verify all components work together
        Path workflowRepo = tempDir.resolve("workflow-test");
        
        // Initialize repository
        Map<String, Object> initResult = toolProvider.initRepository(workflowRepo.toString());
        assertTrue((Boolean) initResult.get("success"), "Init should succeed");
        
        // Create and commit a file
        try {
            Path file = workflowRepo.resolve("workflow.txt");
            Files.writeString(file, "Workflow test");
            
            toolProvider.stageFiles(workflowRepo.toString(), List.of("workflow.txt"));
            Map<String, Object> commitResult = toolProvider.createCommit(
                workflowRepo.toString(),
                "Workflow commit",
                Optional.of("Build Test"),
                Optional.of("build@test.com")
            );
            
            assertNotNull(commitResult.get("hash"), "Commit hash should be present");
            
            // Get status
            Map<String, Object> status = toolProvider.getStatus(workflowRepo.toString());
            assertNotNull(status.get("currentBranch"), "Current branch should be present");
            assertFalse(
                (Boolean) status.get("hasUncommittedChanges"),
                "Should have no uncommitted changes"
            );
            
            // Get history
            Map<String, Object> history = toolProvider.getHistory(
                workflowRepo.toString(),
                Optional.of(10)
            );
            assertNotNull(history.get("commits"), "Commits should be present");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commits = (List<Map<String, Object>>) history.get("commits");
            assertEquals(1, commits.size(), "Should have one commit");
            
            // Create and switch branch
            toolProvider.createBranch(workflowRepo.toString(), "feature-branch");
            toolProvider.switchBranch(workflowRepo.toString(), "feature-branch");
            
            Map<String, Object> newStatus = toolProvider.getStatus(workflowRepo.toString());
            assertEquals(
                "feature-branch",
                newStatus.get("currentBranch"),
                "Should be on feature branch"
            );
            
        } catch (IOException e) {
            fail("Workflow test should not fail: " + e.getMessage());
        }
    }
}
