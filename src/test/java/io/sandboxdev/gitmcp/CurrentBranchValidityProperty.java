package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitCommandExecutor;
import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.RepositoryInfo;
import io.sandboxdev.gitmcp.service.GitRepositoryService;
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
 * Property-based tests for current branch validity.
 * Feature: git-mcp-server, Property 7: Current branch is always defined
 * Validates: Requirements 2.5
 */
public class CurrentBranchValidityProperty {
    
    private final GitRepositoryService repositoryService;
    
    public CurrentBranchValidityProperty() {
        JGitRepositoryManager repositoryManager = new JGitRepositoryManager();
        JGitCommandExecutor commandExecutor = new JGitCommandExecutor();
        this.repositoryService = new GitRepositoryServiceImpl(repositoryManager, commandExecutor);
    }
    
    // Feature: git-mcp-server, Property 7: Current branch is always defined
    @Property(tries = 100)
    void currentBranchIsAlwaysDefined(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository exists
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            
            // Create an initial commit to establish a proper repository state
            createInitialCommit(repositoryPath);
            
            // When getting the current branch
            String currentBranch = repositoryService.getCurrentBranch(repositoryPath);
            
            // Then the current branch should be defined (non-null and non-empty)
            assertThat(currentBranch).isNotNull();
            assertThat(currentBranch).isNotEmpty();
            assertThat(currentBranch).isNotBlank();
            
            // And it should match an existing branch (typically "main" or "master")
            assertThat(currentBranch).matches("^[a-zA-Z0-9/_-]+$");
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Current branch is always defined after branch creation
    @Property(tries = 50)
    void currentBranchIsAlwaysDefinedAfterBranchCreation(@ForAll("validRepositoryPath") Path repositoryPath,
                                                        @ForAll("validBranchName") String branchName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And a new branch is created and checked out
            createAndCheckoutBranch(repositoryPath, branchName);
            
            // When getting the current branch
            String currentBranch = repositoryService.getCurrentBranch(repositoryPath);
            
            // Then the current branch should be defined and match the new branch
            assertThat(currentBranch).isNotNull();
            assertThat(currentBranch).isNotEmpty();
            assertThat(currentBranch).isNotBlank();
            assertThat(currentBranch).isEqualTo(branchName);
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Current branch is always defined after switching branches
    @Property(tries = 30)
    void currentBranchIsAlwaysDefinedAfterSwitchingBranches(@ForAll("validRepositoryPath") Path repositoryPath,
                                                           @ForAll("validBranchName") String firstBranch,
                                                           @ForAll("validBranchName") String secondBranch) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And two different branches are created
            if (!firstBranch.equals(secondBranch)) {
                createBranch(repositoryPath, firstBranch);
                createBranch(repositoryPath, secondBranch);
                
                // Switch to first branch
                checkoutBranch(repositoryPath, firstBranch);
                String currentAfterFirst = repositoryService.getCurrentBranch(repositoryPath);
                assertThat(currentAfterFirst).isEqualTo(firstBranch);
                
                // Switch to second branch
                checkoutBranch(repositoryPath, secondBranch);
                String currentAfterSecond = repositoryService.getCurrentBranch(repositoryPath);
                
                // Then the current branch should always be defined
                assertThat(currentAfterSecond).isNotNull();
                assertThat(currentAfterSecond).isNotEmpty();
                assertThat(currentAfterSecond).isNotBlank();
                assertThat(currentAfterSecond).isEqualTo(secondBranch);
            }
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Current branch is always defined in cloned repositories
    @Property(tries = 20)
    void currentBranchIsAlwaysDefinedInClonedRepositories(@ForAll("validRepositoryPath") Path sourcePath,
                                                         @ForAll("validRepositoryPath") Path targetPath) {
        try {
            // Given a source repository exists with commits
            RepositoryInfo sourceRepo = repositoryService.initRepository(sourcePath);
            assertThat(sourceRepo).isNotNull();
            createInitialCommit(sourcePath);
            
            // When cloning the repository
            RepositoryInfo clonedRepo = repositoryService.cloneRepository(
                sourcePath.toUri().toString(), 
                targetPath, 
                null
            );
            assertThat(clonedRepo).isNotNull();
            
            // Then the current branch should be defined in the cloned repository
            String currentBranch = repositoryService.getCurrentBranch(targetPath);
            
            assertThat(currentBranch).isNotNull();
            assertThat(currentBranch).isNotEmpty();
            assertThat(currentBranch).isNotBlank();
            
            // And it should match the default branch from the cloned repository info
            assertThat(currentBranch).isEqualTo(clonedRepo.defaultBranch());
            
        } finally {
            // Cleanup both repositories
            cleanupRepository(sourcePath);
            cleanupRepository(targetPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Current branch remains defined after commits
    @Property(tries = 30)
    void currentBranchRemainsDefinedAfterCommits(@ForAll("validRepositoryPath") Path repositoryPath,
                                                @ForAll("validFileName") String fileName) {
        try {
            // Given a repository exists with an initial commit
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            String branchBeforeCommit = repositoryService.getCurrentBranch(repositoryPath);
            assertThat(branchBeforeCommit).isNotNull();
            
            // When creating additional commits
            createFileAndCommit(repositoryPath, fileName, "Additional commit content");
            
            // Then the current branch should still be defined and unchanged
            String branchAfterCommit = repositoryService.getCurrentBranch(repositoryPath);
            
            assertThat(branchAfterCommit).isNotNull();
            assertThat(branchAfterCommit).isNotEmpty();
            assertThat(branchAfterCommit).isNotBlank();
            assertThat(branchAfterCommit).isEqualTo(branchBeforeCommit);
            
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Behavior verification - Repository with no commits still has default branch
    @Property(tries = 30)
    void getCurrentBranchWithNoCommitsReturnsDefaultBranch(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized but has no commits
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            // Note: No initial commit created
            
            // When requesting current branch from the repository with no commits
            String currentBranch = repositoryService.getCurrentBranch(repositoryPath);
            
            // Then it should still return a valid default branch (typically "main" or "master")
            assertThat(currentBranch).isNotNull();
            assertThat(currentBranch).isNotEmpty();
            assertThat(currentBranch).isNotBlank();
            
            // Log the behavior for verification
            System.out.println("BEHAVIOR_LOG: getCurrentBranch(no_commits: " + repositoryPath + ") returned: " + currentBranch);
                
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Failure conditions - Null path throws exception
    @Property(tries = 50)
    void getCurrentBranchWithNullPathThrowsException() {
        // When requesting current branch with null path
        // Then it should throw an exception (likely NPE or GitMcpException)
        assertThatThrownBy(() -> repositoryService.getCurrentBranch(null))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                // Log the exception for verification
                System.out.println("FAILURE_LOG: getCurrentBranch(null) threw: " + 
                    exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 7: Failure conditions - Non-existent path throws exception
    @Property(tries = 50)
    void getCurrentBranchWithNonExistentPathThrowsException(@ForAll("nonExistentPath") Path nonExistentPath) {
        // When requesting current branch from a non-existent path
        // Then it should throw an exception
        assertThatThrownBy(() -> repositoryService.getCurrentBranch(nonExistentPath))
            .isInstanceOf(Exception.class)
            .satisfies(exception -> {
                // Log the exception for verification
                System.out.println("FAILURE_LOG: getCurrentBranch(" + nonExistentPath + ") threw: " + 
                    exception.getClass().getSimpleName() + " - " + exception.getMessage());
            });
    }
    
    // Feature: git-mcp-server, Property 7: Failure conditions - Empty directory throws exception
    @Property(tries = 30)
    void getCurrentBranchWithEmptyDirectoryThrowsException(@ForAll("validRepositoryPath") Path emptyPath) {
        try {
            // Given an empty directory exists (no .git folder)
            Files.createDirectories(emptyPath);
            
            // When requesting current branch from the empty directory
            // Then it should throw an exception
            assertThatThrownBy(() -> repositoryService.getCurrentBranch(emptyPath))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    // Log the exception for verification
                    System.out.println("FAILURE_LOG: getCurrentBranch(empty_dir: " + emptyPath + ") threw: " + 
                        exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create empty directory", e);
        } finally {
            // Cleanup
            cleanupRepository(emptyPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Failure conditions - File instead of directory throws exception
    @Property(tries = 30)
    void getCurrentBranchWithFilePathThrowsException(@ForAll("validRepositoryPath") Path basePath) {
        try {
            // Given a file exists instead of a directory
            final Path filePath = basePath.resolve("not-a-directory.txt");
            Files.createDirectories(basePath);
            Files.writeString(filePath, "This is a file, not a directory");
            
            // When requesting current branch from the file path
            // Then it should throw an exception
            assertThatThrownBy(() -> repositoryService.getCurrentBranch(filePath))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    // Log the exception for verification
                    System.out.println("FAILURE_LOG: getCurrentBranch(file_path: " + filePath + ") threw: " + 
                        exception.getClass().getSimpleName() + " - " + exception.getMessage());
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
    
    // Feature: git-mcp-server, Property 7: Failure conditions - Corrupted .git directory throws exception
    @Property(tries = 20)
    void getCurrentBranchWithCorruptedGitDirectoryThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a repository is initialized
            RepositoryInfo repoInfo = repositoryService.initRepository(repositoryPath);
            assertThat(repoInfo).isNotNull();
            createInitialCommit(repositoryPath);
            
            // And the .git directory is corrupted by removing critical files
            Path gitDir = repositoryPath.resolve(".git");
            Path headFile = gitDir.resolve("HEAD");
            if (Files.exists(headFile)) {
                Files.delete(headFile);
            }
            
            // When requesting current branch from the corrupted repository
            // Then it should throw an exception
            assertThatThrownBy(() -> repositoryService.getCurrentBranch(repositoryPath))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    // Log the exception for verification
                    System.out.println("FAILURE_LOG: getCurrentBranch(corrupted_git: " + repositoryPath + ") threw: " + 
                        exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to corrupt .git directory", e);
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
    }
    
    // Feature: git-mcp-server, Property 7: Failure conditions - Fake .git file throws exception
    @Property(tries = 20)
    void getCurrentBranchWithFakeGitFileThrowsException(@ForAll("validRepositoryPath") Path repositoryPath) {
        try {
            // Given a directory with a fake .git file instead of directory
            Files.createDirectories(repositoryPath);
            Path fakeGitFile = repositoryPath.resolve(".git");
            Files.writeString(fakeGitFile, "This is not a git directory");
            
            // When requesting current branch from the fake repository
            // Then it should throw an exception
            assertThatThrownBy(() -> repositoryService.getCurrentBranch(repositoryPath))
                .isInstanceOf(Exception.class)
                .satisfies(exception -> {
                    // Log the exception for verification
                    System.out.println("FAILURE_LOG: getCurrentBranch(fake_git_file: " + repositoryPath + ") threw: " + 
                        exception.getClass().getSimpleName() + " - " + exception.getMessage());
                });
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to create fake .git file", e);
        } finally {
            // Cleanup
            cleanupRepository(repositoryPath);
        }
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
                    return Files.createTempDirectory("git-branch-test-" + randomName + "-");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp directory", e);
                }
            });
    }
    
    @Provide
    Arbitrary<String> validBranchName() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '_')
            .ofMinLength(3)
            .ofMaxLength(15)
            .filter(name -> !name.startsWith("-") && !name.endsWith("-"))
            .filter(name -> !name.contains(".."))
            .map(name -> "branch-" + name);
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
    Arbitrary<Path> nonExistentPath() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(10)
            .ofMaxLength(30)
            .map(randomName -> Path.of("/tmp/non-existent-branch-test-" + randomName + "-" + System.currentTimeMillis()));
    }
    
    private void createInitialCommit(Path repositoryPath) {
        try {
            // Create a simple file to commit
            Path testFile = repositoryPath.resolve("README.md");
            Files.writeString(testFile, "# Test Repository\n\nThis is a test repository for branch testing.");
            
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
    
    private void createBranch(Path repositoryPath, String branchName) {
        try {
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.branchCreate()
                    .setName(branchName)
                    .call();
            }
            
            repository.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create branch: " + branchName, e);
        }
    }
    
    private void checkoutBranch(Path repositoryPath, String branchName) {
        try {
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.checkout()
                    .setName(branchName)
                    .call();
            }
            
            repository.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to checkout branch: " + branchName, e);
        }
    }
    
    private void createAndCheckoutBranch(Path repositoryPath, String branchName) {
        try {
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .call();
            }
            
            repository.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create and checkout branch: " + branchName, e);
        }
    }
    
    private void createFileAndCommit(Path repositoryPath, String fileName, String content) {
        try {
            // Create a file
            Path newFile = repositoryPath.resolve(fileName);
            Files.writeString(newFile, content);
            
            // Open the repository and commit the file
            Repository repository = new FileRepositoryBuilder()
                .setGitDir(repositoryPath.resolve(".git").toFile())
                .build();
            
            try (Git git = new Git(repository)) {
                git.add().addFilepattern(fileName).call();
                git.commit()
                    .setMessage("Add " + fileName)
                    .setAuthor("Test User", "test@example.com")
                    .setCommitter("Test User", "test@example.com")
                    .call();
            }
            
            repository.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create file and commit: " + fileName, e);
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