package io.sandboxdev.gitmcp.service;

import io.sandboxdev.gitmcp.model.GitBranchInfo;
import io.sandboxdev.gitmcp.model.GitCommitInfo;
import io.sandboxdev.gitmcp.model.GitStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for JGitRepositoryManager using Testcontainers for real Git repositories.
 */
@Testcontainers
class JGitRepositoryManagerTest {

    private JGitRepositoryManager repositoryManager;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        repositoryManager = new JGitRepositoryManager();
    }

    @Test
    void getRepository_withValidPath_returnsRepository() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        
        // Act
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Assert
        assertThat(repository).isNotNull();
        assertThat(repository.getObjectDatabase()).isNotNull();
    }

    @Test
    void getRepository_withNullPath_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> repositoryManager.getRepository(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Repository path cannot be null");
    }

    @Test
    void getRepository_withInvalidPath_throwsRuntimeException() {
        // Arrange
        Path invalidPath = tempDir.resolve("not-a-repo");
        
        // Act & Assert
        assertThatThrownBy(() -> repositoryManager.getRepository(invalidPath))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to open repository");
    }

    @Test
    void getStatus_withCleanRepository_returnsCleanStatus() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Act
        GitStatus status = repositoryManager.getStatus(repository);
        
        // Assert
        assertThat(status.isClean()).isTrue();
        assertThat(status.modifiedFiles()).isEmpty();
        assertThat(status.stagedFiles()).isEmpty();
        assertThat(status.untrackedFiles()).isEmpty();
    }

    @Test
    void getStatus_withUntrackedFile_returnsUntrackedFiles() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        Files.writeString(repoPath.resolve("untracked.txt"), "content");
        
        // Act
        GitStatus status = repositoryManager.getStatus(repository);
        
        // Assert
        assertThat(status.isClean()).isFalse();
        assertThat(status.untrackedFiles()).contains("untracked.txt");
    }

    @Test
    void commit_withStagedChanges_returnsCommitId() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Create and stage a file
        Files.writeString(repoPath.resolve("test.txt"), "content");
        try (Git git = new Git(repository)) {
            git.add().addFilepattern("test.txt").call();
        }
        
        // Act
        ObjectId commitId = repositoryManager.commit(repository, "Test commit");
        
        // Assert
        assertThat(commitId).isNotNull();
        assertThat(commitId.getName()).hasSize(40); // SHA-1 hash length
    }

    @Test
    void getCommitInfo_withValidCommit_returnsCommitInfo() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Create and commit a file
        Files.writeString(repoPath.resolve("test.txt"), "content");
        try (Git git = new Git(repository)) {
            git.add().addFilepattern("test.txt").call();
        }
        ObjectId commitId = repositoryManager.commit(repository, "Test commit");
        
        // Act
        GitCommitInfo commitInfo = repositoryManager.getCommitInfo(repository, commitId);
        
        // Assert
        assertThat(commitInfo.hash()).isEqualTo(commitId.getName());
        assertThat(commitInfo.shortHash()).hasSize(7);
        assertThat(commitInfo.message()).isEqualTo("Test commit");
        assertThat(commitInfo.author()).isNotBlank();
        assertThat(commitInfo.timestamp()).isNotNull();
    }

    @Test
    void listBranches_withDefaultBranch_returnsMainBranch() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Act
        List<GitBranchInfo> branches = repositoryManager.listBranches(repository);
        
        // Assert
        assertThat(branches).hasSize(1);
        GitBranchInfo mainBranch = branches.get(0);
        assertThat(mainBranch.name()).isEqualTo("main");
        assertThat(mainBranch.isCurrent()).isTrue();
        assertThat(mainBranch.isRemote()).isFalse();
    }

    @Test
    void getLog_withLimit_returnsLimitedCommits() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Create multiple commits
        for (int i = 1; i <= 3; i++) {
            Files.writeString(repoPath.resolve("file" + i + ".txt"), "content " + i);
            try (Git git = new Git(repository)) {
                git.add().addFilepattern("file" + i + ".txt").call();
            }
            repositoryManager.commit(repository, "Commit " + i);
        }
        
        // Act
        List<GitCommitInfo> commits = repositoryManager.getLog(repository, 2);
        
        // Assert
        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).message()).isEqualTo("Commit 3"); // Most recent first
        assertThat(commits.get(1).message()).isEqualTo("Commit 2");
    }

    @Test
    void hasUncommittedChanges_withCleanRepo_returnsFalse() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Act
        boolean hasChanges = repositoryManager.hasUncommittedChanges(repository);
        
        // Assert
        assertThat(hasChanges).isFalse();
    }

    @Test
    void hasUncommittedChanges_withUntrackedFile_returnsTrue() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        Files.writeString(repoPath.resolve("untracked.txt"), "content");
        
        // Act
        boolean hasChanges = repositoryManager.hasUncommittedChanges(repository);
        
        // Assert
        assertThat(hasChanges).isTrue();
    }

    @Test
    void isValidRepository_withValidRepo_returnsTrue() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        
        // Act
        boolean isValid = repositoryManager.isValidRepository(repoPath);
        
        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void isValidRepository_withInvalidPath_returnsFalse() {
        // Arrange
        Path invalidPath = tempDir.resolve("not-a-repo");
        
        // Act
        boolean isValid = repositoryManager.isValidRepository(invalidPath);
        
        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void closeRepository_removesFromCache() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        assertThat(repository).isNotNull();
        
        // Act
        repositoryManager.closeRepository(repoPath);
        
        // Assert - Getting repository again should create new instance
        Repository newRepository = repositoryManager.getRepository(repoPath);
        assertThat(newRepository).isNotSameAs(repository);
    }

    @Test
    void closeAll_clearsAllRepositories() throws IOException, GitAPIException {
        // Arrange
        Path repoPath1 = initializeGitRepository(tempDir.resolve("repo1"));
        Path repoPath2 = initializeGitRepository(tempDir.resolve("repo2"));
        
        repositoryManager.getRepository(repoPath1);
        repositoryManager.getRepository(repoPath2);
        
        // Act
        repositoryManager.closeAll();
        
        // Assert - Should be able to get repositories again (new instances)
        assertThat(repositoryManager.getRepository(repoPath1)).isNotNull();
        assertThat(repositoryManager.getRepository(repoPath2)).isNotNull();
    }

    @Test
    void getLogWithFilePath_returnsCommitsForSpecificFile() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Create commits for different files
        Files.writeString(repoPath.resolve("file1.txt"), "content 1");
        Files.writeString(repoPath.resolve("file2.txt"), "content 2");
        
        try (Git git = new Git(repository)) {
            git.add().addFilepattern("file1.txt").call();
            git.add().addFilepattern("file2.txt").call();
        }
        repositoryManager.commit(repository, "Add both files");
        
        // Modify only file1
        Files.writeString(repoPath.resolve("file1.txt"), "modified content 1");
        try (Git git = new Git(repository)) {
            git.add().addFilepattern("file1.txt").call();
        }
        repositoryManager.commit(repository, "Modify file1");
        
        // Act
        List<GitCommitInfo> allCommits = repositoryManager.getLog(repository, 10);
        List<GitCommitInfo> file1Commits = repositoryManager.getLog(repository, 10, "file1.txt");
        
        // Assert
        assertThat(allCommits).hasSize(3); // Initial + Add both + Modify file1
        assertThat(file1Commits).hasSize(2); // Add both + Modify file1 (file1 was involved)
    }

    @Test
    void getDiff_withTwoRefs_returnsDiffOutput() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Get initial commit hash
        String initialCommit = repository.resolve("HEAD").getName();
        
        // Create a new file and commit
        Files.writeString(repoPath.resolve("newfile.txt"), "new content");
        try (Git git = new Git(repository)) {
            git.add().addFilepattern("newfile.txt").call();
        }
        repositoryManager.commit(repository, "Add new file");
        String secondCommit = repository.resolve("HEAD").getName();
        
        // Act
        String diff = repositoryManager.getDiff(repository, initialCommit, secondCommit);
        
        // Assert
        assertThat(diff).isNotEmpty();
        assertThat(diff).contains("diff --git a//dev/null b/newfile.txt"); // JGit shows /dev/null for new files
        assertThat(diff).contains("index 0000000000000000000000000000000000000000");
    }

    @Test
    void getUnstagedDiff_withModifiedFiles_returnsDiff() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Modify existing file without staging
        Files.writeString(repoPath.resolve("README.md"), "# Modified Repository");
        
        // Act
        String diff = repositoryManager.getUnstagedDiff(repository);
        
        // Assert
        assertThat(diff).isNotEmpty();
        assertThat(diff).contains("diff --git a/README.md b/README.md");
    }

    @Test
    void createBranch_createsNewBranch() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        // Act
        repositoryManager.createBranch(repository, "feature-branch");
        
        // Assert
        List<GitBranchInfo> branches = repositoryManager.listBranches(repository);
        assertThat(branches).hasSize(2);
        assertThat(branches).extracting(GitBranchInfo::name)
            .contains("main", "feature-branch");
    }

    @Test
    void checkout_switchesToBranch() throws IOException, GitAPIException {
        // Arrange
        Path repoPath = initializeGitRepository(tempDir);
        Repository repository = repositoryManager.getRepository(repoPath);
        
        repositoryManager.createBranch(repository, "feature-branch");
        
        // Act
        repositoryManager.checkout(repository, "feature-branch");
        
        // Assert
        List<GitBranchInfo> branches = repositoryManager.listBranches(repository);
        GitBranchInfo currentBranch = branches.stream()
            .filter(GitBranchInfo::isCurrent)
            .findFirst()
            .orElseThrow();
        
        assertThat(currentBranch.name()).isEqualTo("feature-branch");
    }

    /**
     * Helper method to initialize a Git repository with an initial commit.
     */
    private Path initializeGitRepository(Path path) throws IOException, GitAPIException {
        Files.createDirectories(path);
        
        try (Git git = Git.init().setDirectory(path.toFile()).setInitialBranch("main").call()) {
            // Configure user for commits
            git.getRepository().getConfig().setString("user", null, "name", "Test User");
            git.getRepository().getConfig().setString("user", null, "email", "test@example.com");
            git.getRepository().getConfig().save();
            
            // Create initial commit
            Files.writeString(path.resolve("README.md"), "# Test Repository");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Initial commit").call();
        }
        
        return path;
    }
}
