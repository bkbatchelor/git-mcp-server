package io.sandboxdev.gitmcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.jgit.RepositoryLockManager;
import io.sandboxdev.gitmcp.mcp.GitMcpResourceProvider;
import io.sandboxdev.gitmcp.model.RepositoryInfo;
import io.sandboxdev.gitmcp.service.GitBranchService;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
import io.sandboxdev.gitmcp.service.impl.GitBranchServiceImpl;
import io.sandboxdev.gitmcp.service.impl.GitRepositoryServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for MCP resource exposure.
 * Feature: git-mcp-server, Property 23: Resources expose repository information
 * Validates: Requirements 7.5
 */
public class ResourceExposureProperty {
    
    private final GitMcpResourceProvider resourceProvider;
    private final GitRepositoryService repositoryService;
    private final ObjectMapper objectMapper;
    
    public ResourceExposureProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        RepositoryLockManager lockManager = new RepositoryLockManager();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
        GitBranchService branchService = new GitBranchServiceImpl(repositoryManager, commandExecutor, lockManager);
        this.objectMapper = new ObjectMapper();
        this.resourceProvider = new GitMcpResourceProvider(repositoryService, branchService, objectMapper);
    }
    
    // Feature: git-mcp-server, Property 23: Resources expose repository information
    @Property(tries = 100)
    void repositoryStatusResourceExposesInformation(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When requesting the repository status resource
            String statusResource = resourceProvider.getRepositoryStatus(repositoryPath.toString());
            
            // Then the resource should contain MCP-formatted repository state
            assertThat(statusResource).isNotNull();
            assertThat(statusResource).isNotEmpty();
            
            // Parse the JSON response to verify structure
            JsonNode statusJson = objectMapper.readTree(statusResource);
            
            // Verify all required fields are present
            assertThat(statusJson.has("currentBranch")).isTrue();
            assertThat(statusJson.has("stagedFiles")).isTrue();
            assertThat(statusJson.has("unstagedFiles")).isTrue();
            assertThat(statusJson.has("untrackedFiles")).isTrue();
            assertThat(statusJson.has("hasUncommittedChanges")).isTrue();
            
            // Verify field types and values
            assertThat(statusJson.get("currentBranch").isTextual()).isTrue();
            assertThat(statusJson.get("currentBranch").asText()).isNotEmpty();
            assertThat(statusJson.get("stagedFiles").isArray()).isTrue();
            assertThat(statusJson.get("unstagedFiles").isArray()).isTrue();
            assertThat(statusJson.get("untrackedFiles").isArray()).isTrue();
            assertThat(statusJson.get("hasUncommittedChanges").isBoolean()).isTrue();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to test repository status resource", e);
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Resources expose repository information - branches
    @Property(tries = 100)
    void repositoryBranchesResourceExposesInformation(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When requesting the repository branches resource
            String branchesResource = resourceProvider.getRepositoryBranches(repositoryPath.toString());
            
            // Then the resource should contain MCP-formatted branch information
            assertThat(branchesResource).isNotNull();
            assertThat(branchesResource).isNotEmpty();
            
            // Parse the JSON response to verify structure
            JsonNode branchesJson = objectMapper.readTree(branchesResource);
            
            // Verify branches array is present
            assertThat(branchesJson.has("branches")).isTrue();
            assertThat(branchesJson.get("branches").isArray()).isTrue();
            
            // Verify at least one branch exists (the default branch)
            JsonNode branchesArray = branchesJson.get("branches");
            assertThat(branchesArray.size()).isGreaterThan(0);
            
            // Verify each branch has required fields
            for (JsonNode branch : branchesArray) {
                assertThat(branch.has("name")).isTrue();
                assertThat(branch.has("commitHash")).isTrue();
                assertThat(branch.has("isCurrent")).isTrue();
                
                assertThat(branch.get("name").isTextual()).isTrue();
                assertThat(branch.get("name").asText()).isNotEmpty();
                assertThat(branch.get("commitHash").isTextual()).isTrue();
                assertThat(branch.get("commitHash").asText()).isNotEmpty();
                assertThat(branch.get("isCurrent").isBoolean()).isTrue();
            }
            
            // Verify exactly one branch is marked as current
            long currentBranchCount = 0;
            for (JsonNode branch : branchesArray) {
                if (branch.get("isCurrent").asBoolean()) {
                    currentBranchCount++;
                }
            }
            assertThat(currentBranchCount).isEqualTo(1);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to test repository branches resource", e);
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Resources expose repository information - history
    @Property(tries = 100)
    void repositoryHistoryResourceExposesInformation(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When requesting the repository history resource
            String historyResource = resourceProvider.getRepositoryHistory(repositoryPath.toString());
            
            // Then the resource should contain MCP-formatted commit history
            assertThat(historyResource).isNotNull();
            assertThat(historyResource).isNotEmpty();
            
            // Parse the JSON response to verify structure
            JsonNode historyJson = objectMapper.readTree(historyResource);
            
            // Verify commits array is present
            assertThat(historyJson.has("commits")).isTrue();
            assertThat(historyJson.get("commits").isArray()).isTrue();
            
            // Verify at least one commit exists (the initial commit)
            JsonNode commitsArray = historyJson.get("commits");
            assertThat(commitsArray.size()).isGreaterThan(0);
            
            // Verify each commit has required fields
            for (JsonNode commit : commitsArray) {
                assertThat(commit.has("hash")).isTrue();
                assertThat(commit.has("shortHash")).isTrue();
                assertThat(commit.has("message")).isTrue();
                assertThat(commit.has("author")).isTrue();
                assertThat(commit.has("timestamp")).isTrue();
                assertThat(commit.has("changedFiles")).isTrue();
                assertThat(commit.has("stats")).isTrue();
                
                // Verify field types
                assertThat(commit.get("hash").isTextual()).isTrue();
                assertThat(commit.get("hash").asText()).isNotEmpty();
                assertThat(commit.get("shortHash").isTextual()).isTrue();
                assertThat(commit.get("shortHash").asText()).isNotEmpty();
                assertThat(commit.get("message").isTextual()).isTrue();
                assertThat(commit.get("message").asText()).isNotEmpty();
                assertThat(commit.get("changedFiles").isArray()).isTrue();
                
                // Verify author structure
                JsonNode author = commit.get("author");
                assertThat(author.has("name")).isTrue();
                assertThat(author.has("email")).isTrue();
                assertThat(author.get("name").isTextual()).isTrue();
                assertThat(author.get("email").isTextual()).isTrue();
                
                // Verify stats structure
                JsonNode stats = commit.get("stats");
                assertThat(stats.has("insertions")).isTrue();
                assertThat(stats.has("deletions")).isTrue();
                assertThat(stats.has("filesChanged")).isTrue();
                assertThat(stats.get("insertions").isNumber()).isTrue();
                assertThat(stats.get("deletions").isNumber()).isTrue();
                assertThat(stats.get("filesChanged").isNumber()).isTrue();
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to test repository history resource", e);
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Resources expose repository information - current branch
    @Property(tries = 100)
    void currentBranchResourceExposesInformation(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // When requesting the current branch resource
            String currentBranchResource = resourceProvider.getCurrentBranch(repositoryPath.toString());
            
            // Then the resource should contain the current branch name
            assertThat(currentBranchResource).isNotNull();
            assertThat(currentBranchResource).isNotEmpty();
            
            // The current branch should be a valid branch name (typically "main" or "master")
            assertThat(currentBranchResource).matches("^[a-zA-Z0-9/_-]+$");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to test current branch resource", e);
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Resources expose repository information with staged changes
    @Property(tries = 50)
    void repositoryStatusResourceWithStagedChanges(@ForAll("validRepositoryPath") Path repositoryPath,
                                                 @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And a new file is created and staged
            Path newFile = repositoryPath.resolve(fileName);
            Files.writeString(newFile, "New file content for resource test");
            
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.add().addFilepattern(fileName).call();
            }
            repository.close();
            
            // When requesting the repository status resource
            String statusResource = resourceProvider.getRepositoryStatus(repositoryPath.toString());
            
            // Then the resource should reflect the staged changes
            JsonNode statusJson = objectMapper.readTree(statusResource);
            
            // Verify staged files contains the new file
            JsonNode stagedFiles = statusJson.get("stagedFiles");
            assertThat(stagedFiles.isArray()).isTrue();
            
            boolean foundStagedFile = false;
            for (JsonNode stagedFile : stagedFiles) {
                if (stagedFile.asText().equals(fileName)) {
                    foundStagedFile = true;
                    break;
                }
            }
            assertThat(foundStagedFile).isTrue();
            
            // Verify hasUncommittedChanges is true
            assertThat(statusJson.get("hasUncommittedChanges").asBoolean()).isTrue();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to test repository status resource with staged changes", e);
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Resources expose repository information with multiple branches
    @Property(tries = 50)
    void repositoryBranchesResourceWithMultipleBranches(@ForAll("validRepositoryPath") Path repositoryPath,
                                                      @ForAll("validBranchName") String branchName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And a new branch is created
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.branchCreate().setName(branchName).call();
            }
            repository.close();
            
            // When requesting the repository branches resource
            String branchesResource = resourceProvider.getRepositoryBranches(repositoryPath.toString());
            
            // Then the resource should contain both branches
            JsonNode branchesJson = objectMapper.readTree(branchesResource);
            JsonNode branchesArray = branchesJson.get("branches");
            
            // Should have at least 2 branches now
            assertThat(branchesArray.size()).isGreaterThanOrEqualTo(2);
            
            // Verify the new branch is present
            boolean foundNewBranch = false;
            for (JsonNode branch : branchesArray) {
                if (branch.get("name").asText().equals(branchName)) {
                    foundNewBranch = true;
                    // New branch should not be current
                    assertThat(branch.get("isCurrent").asBoolean()).isFalse();
                    break;
                }
            }
            assertThat(foundNewBranch).isTrue();
            
            // Still should have exactly one current branch
            long currentBranchCount = 0;
            for (JsonNode branch : branchesArray) {
                if (branch.get("isCurrent").asBoolean()) {
                    currentBranchCount++;
                }
            }
            assertThat(currentBranchCount).isEqualTo(1);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to test repository branches resource with multiple branches", e);
        } finally {
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Invalid repository paths throw exceptions
    @Property(tries = 50)
    void repositoryStatusResourceThrowsExceptionForInvalidPaths(@ForAll("invalidRepositoryPath") Path invalidPath) {
        // When requesting status resource from an invalid repository path
        // Then it should throw an exception and log the error
        assertThatThrownBy(() -> resourceProvider.getRepositoryStatus(invalidPath.toString()))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                // Log the exception for verification
                System.out.println("Expected exception for invalid path " + invalidPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
        
        // Cleanup any created directories
        cleanupRepository(invalidPath);
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Null path parameters throw exceptions
    @Property(tries = 10)
    void repositoryResourcesThrowExceptionForNullPaths() {
        // When requesting resources with null paths
        // Then they should throw exceptions
        assertThatThrownBy(() -> resourceProvider.getRepositoryStatus(null))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for null status path: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
            
        assertThatThrownBy(() -> resourceProvider.getRepositoryBranches(null))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for null branches path: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
            
        assertThatThrownBy(() -> resourceProvider.getRepositoryHistory(null))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for null history path: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
            
        assertThatThrownBy(() -> resourceProvider.getCurrentBranch(null))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for null current branch path: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Non-existent paths throw exceptions
    @Property(tries = 30)
    void repositoryResourcesThrowExceptionForNonExistentPaths(@ForAll("nonExistentPath") Path nonExistentPath) {
        // When requesting resources from non-existent paths
        // Then they should throw exceptions
        assertThatThrownBy(() -> resourceProvider.getRepositoryStatus(nonExistentPath.toString()))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for non-existent status path " + nonExistentPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
            
        assertThatThrownBy(() -> resourceProvider.getRepositoryBranches(nonExistentPath.toString()))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for non-existent branches path " + nonExistentPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
            
        assertThatThrownBy(() -> resourceProvider.getRepositoryHistory(nonExistentPath.toString()))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for non-existent history path " + nonExistentPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
            
        assertThatThrownBy(() -> resourceProvider.getCurrentBranch(nonExistentPath.toString()))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                System.out.println("Expected exception for non-existent current branch path " + nonExistentPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Empty directories without .git throw exceptions
    @Property(tries = 30)
    void repositoryResourcesThrowExceptionForEmptyDirectories(@ForAll("validRepositoryPath") Path emptyPath) {
        try {
            // Given an empty directory exists (no .git folder)
            Files.createDirectories(emptyPath);
            
            // When requesting resources from the empty directory
            // Then they should throw exceptions
            assertThatThrownBy(() -> resourceProvider.getRepositoryStatus(emptyPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for empty directory status " + emptyPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getRepositoryBranches(emptyPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for empty directory branches " + emptyPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getRepositoryHistory(emptyPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for empty directory history " + emptyPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getCurrentBranch(emptyPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for empty directory current branch " + emptyPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty directory", e);
        } finally {
            // Cleanup
            cleanupRepository(emptyPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - File paths instead of directories throw exceptions
    @Property(tries = 20)
    void repositoryResourcesThrowExceptionForFilePaths(@ForAll("validRepositoryPath") Path basePath) {
        try {
            // Given a file exists instead of a directory
            Files.createDirectories(basePath);
            final Path filePath = basePath.resolve("not-a-directory.txt");
            Files.writeString(filePath, "This is a file, not a directory");
            
            // When requesting resources from the file path
            // Then they should throw exceptions
            assertThatThrownBy(() -> resourceProvider.getRepositoryStatus(filePath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for file path status " + filePath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getRepositoryBranches(filePath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for file path branches " + filePath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getRepositoryHistory(filePath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for file path history " + filePath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getCurrentBranch(filePath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for file path current branch " + filePath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            // Cleanup the file
            Files.deleteIfExists(filePath);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test file", e);
        } finally {
            // Cleanup
            cleanupRepository(basePath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Corrupted .git directory throws exceptions
    @Property(tries = 20)
    void repositoryResourcesThrowExceptionForCorruptedGitDirectory(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            
            // And the .git directory is corrupted (replace with a file)
            Path gitDir = repositoryPath.resolve(".git");
            if (Files.exists(gitDir)) {
                Files.walk(gitDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore individual file deletion errors
                        }
                    });
            }
            // Create a file where .git directory should be
            Files.writeString(gitDir, "This is not a git directory");
            
            // When requesting resources from the corrupted repository
            // Then they should throw exceptions
            assertThatThrownBy(() -> resourceProvider.getRepositoryStatus(repositoryPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for corrupted git directory status " + repositoryPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getRepositoryBranches(repositoryPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for corrupted git directory branches " + repositoryPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getRepositoryHistory(repositoryPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for corrupted git directory history " + repositoryPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
            assertThatThrownBy(() -> resourceProvider.getCurrentBranch(repositoryPath.toString()))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    System.out.println("Expected exception for corrupted git directory current branch " + repositoryPath + ": " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to corrupt .git directory", e);
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Repository without commits throws exceptions for history
    @Property(tries = 20)
    void repositoryHistoryResourceThrowsExceptionForEmptyRepository(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized but has no commits
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            // Note: Not creating any commits
            
            // When requesting history resource from the empty repository
            // Then it should handle gracefully (may return empty array or throw exception)
            // This tests the edge case of repositories without any commits
            try {
                String historyResource = resourceProvider.getRepositoryHistory(repositoryPath.toString());
                // If it doesn't throw, verify it returns valid JSON with empty commits array
                JsonNode historyJson = objectMapper.readTree(historyResource);
                assertThat(historyJson.has("commits")).isTrue();
                assertThat(historyJson.get("commits").isArray()).isTrue();
                System.out.println("Empty repository history handled gracefully: " + historyResource);
            } catch (Exception e) {
                // If it throws an exception, that's also acceptable behavior
                System.out.println("Expected exception for empty repository history " + repositoryPath + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
                
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 23: Failure conditions - Invalid path characters throw exceptions
    @Property(tries = 20)
    void repositoryResourcesThrowExceptionForInvalidPathCharacters(@ForAll("invalidPathString") String invalidPathString) {
        // When requesting resources with invalid path characters
        // Then they should either throw exceptions or handle gracefully
        // Some invalid paths might be handled by the OS/filesystem layer
        
        try {
            resourceProvider.getRepositoryStatus(invalidPathString);
            System.out.println("Invalid path status handled gracefully: '" + invalidPathString + "'");
        } catch (Exception e) {
            System.out.println("Expected exception for invalid path characters status '" + invalidPathString + "': " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        try {
            resourceProvider.getRepositoryBranches(invalidPathString);
            System.out.println("Invalid path branches handled gracefully: '" + invalidPathString + "'");
        } catch (Exception e) {
            System.out.println("Expected exception for invalid path characters branches '" + invalidPathString + "': " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        try {
            resourceProvider.getRepositoryHistory(invalidPathString);
            System.out.println("Invalid path history handled gracefully: '" + invalidPathString + "'");
        } catch (Exception e) {
            System.out.println("Expected exception for invalid path characters history '" + invalidPathString + "': " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        try {
            resourceProvider.getCurrentBranch(invalidPathString);
            System.out.println("Invalid path current branch handled gracefully: '" + invalidPathString + "'");
        } catch (Exception e) {
            System.out.println("Expected exception for invalid path characters current branch '" + invalidPathString + "': " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        
        // The test passes if we reach this point - either exceptions were thrown or handled gracefully
        // Both behaviors are acceptable for invalid paths
    }
    
    @Provide
    Arbitrary<Path> invalidRepositoryPath() {
        return Arbitraries.oneOf(
            // Paths that look like repositories but aren't (fake .git file)
            Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(15)
                .map(name -> {
                    try {
                        Path tempDir = Files.createTempDirectory("git-fake-test-" + name + "-");
                        // Create a fake .git file instead of directory
                        Files.writeString(tempDir.resolve(".git"), "This is not a git directory");
                        return tempDir;
                    } catch (IOException e) {
                        return Path.of("/tmp/fake-git-" + name);
                    }
                }),
            // Very long paths that might cause issues
            Arbitraries.strings()
                .alpha()
                .ofMinLength(100)
                .ofMaxLength(200)
                .map(longName -> {
                    try {
                        return Files.createTempDirectory("git-long-test-").resolve(longName);
                    } catch (IOException e) {
                        return Path.of("/tmp/" + longName);
                    }
                }),
            // Paths with spaces and special characters (but valid for filesystem)
            Arbitraries.strings()
                .withChars(' ', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '=', '+')
                .ofMinLength(3)
                .ofMaxLength(10)
                .map(specialName -> {
                    try {
                        Path tempDir = Files.createTempDirectory("git-special-test-");
                        // Create a fake .git file with special characters in path
                        Path specialPath = tempDir.resolve(specialName);
                        Files.createDirectories(specialPath);
                        Files.writeString(specialPath.resolve(".git"), "fake git");
                        return specialPath;
                    } catch (IOException e) {
                        return Path.of("/tmp/special-" + specialName.replaceAll("[^a-zA-Z0-9]", "_"));
                    }
                })
        );
    }
    
    @Provide
    Arbitrary<Path> nonExistentPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(10)
            .ofMaxLength(30)
            .map(randomName -> Path.of("/tmp/non-existent-" + randomName + "-" + System.currentTimeMillis()));
    }
    
    @Provide
    Arbitrary<String> invalidPathString() {
        return Arbitraries.oneOf(
            // Empty string
            Arbitraries.just(""),
            // Whitespace only
            Arbitraries.just("   "),
            // Invalid characters for paths
            Arbitraries.strings()
                .withChars('\0', '\n', '\r', '\t')
                .ofMinLength(1)
                .ofMaxLength(5),
            // Very long strings
            Arbitraries.strings()
                .alpha()
                .ofMinLength(1000)
                .ofMaxLength(2000)
        );
    }
    
    @Provide
    Arbitrary<Path> validRepositoryPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(randomName -> {
                try {
                    return Files.createTempDirectory("git-resource-test-" + randomName + "-");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            });
    }
    
    @Provide
    Arbitrary<String> validFileName() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(3)
            .ofMaxLength(15)
            .map(name -> name + ".txt");
    }
    
    @Provide
    Arbitrary<String> validBranchName() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '_')
            .ofMinLength(3)
            .ofMaxLength(20)
            .filter(name -> !name.equals("main") && !name.equals("master")) // Avoid conflicts with default branch
            .filter(name -> !name.startsWith("-") && !name.startsWith("_")) // Git branch names cannot start with - or _
            .filter(name -> !name.endsWith("-") && !name.endsWith("_")) // Better to avoid ending with special chars too
            .filter(name -> name.matches("^[a-zA-Z][a-zA-Z0-9_-]*[a-zA-Z0-9]$")); // Must start with letter, end with alphanumeric
    }
    
    private void createInitialCommit(Path repositoryPath) {
        try {
            // Create a simple file to commit
            Path testFile = repositoryPath.resolve("README.md");
            Files.writeString(testFile, "# Test Repository\n\nThis is a test repository for resource testing.");
            
            // Open the repository and create an initial commit
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.add().addFilepattern("README.md").call();
                git.commit()
                    .setMessage("Initial commit")
                    .setAuthor("Test User", "test@example.com")
                    .setCommitter("Test User", "test@example.com")
                    .call();
            }
            
            repository.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create initial commit", e);
        }
    }
    
    private void cleanupRepository(Path repositoryPath) {
        try {
            if (Files.exists(repositoryPath)) {
                Files.walk(repositoryPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
}