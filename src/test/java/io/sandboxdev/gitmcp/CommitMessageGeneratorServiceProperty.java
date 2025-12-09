package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.jgit.JGitRepositoryManager;
import io.sandboxdev.gitmcp.model.CommitType;
import io.sandboxdev.gitmcp.service.CommitMessageGeneratorService;
import io.sandboxdev.gitmcp.service.impl.CommitMessageGeneratorServiceImpl;
import net.jqwik.api.*;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for CommitMessageGeneratorService.
 */
public class CommitMessageGeneratorServiceProperty {
    
    private final CommitMessageGeneratorService service = new CommitMessageGeneratorServiceImpl(null);
    
    // Feature: git-mcp-server, Property 27: Project ID extraction from branch names
    @Property(tries = 100)
    void projectIdExtractionFromBranchNames(
        @ForAll("branchPrefix") String prefix,
        @ForAll("projectId") String projectId,
        @ForAll("issueNumber") int issueNumber
    ) {
        // Given a branch name with pattern <prefix>/<PROJECT-ID>-<number>
        String branchName = prefix + "/" + projectId + "-" + issueNumber;
        String expectedProjectId = (projectId + "-" + issueNumber).toUpperCase();
        
        // When extracting the project ID
        Optional<String> result = service.extractProjectId(branchName);
        
        // Then it should return the project ID in uppercase
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedProjectId);
    }
    
    @Property(tries = 100)
    void projectIdExtractionReturnsEmptyForInvalidBranchNames(
        @ForAll("invalidBranchName") String branchName
    ) {
        // Given an invalid branch name (no project ID pattern)
        // When extracting the project ID
        Optional<String> result = service.extractProjectId(branchName);
        
        // Then it should return empty
        assertThat(result).isEmpty();
    }
    
    @Provide
    Arbitrary<String> branchPrefix() {
        return Arbitraries.of("feature", "bugfix", "hotfix", "chore", "refactor", "docs");
    }
    
    @Provide
    Arbitrary<String> projectId() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(2)
            .ofMaxLength(10)
            .map(String::toUpperCase);
    }
    
    @Provide
    Arbitrary<Integer> issueNumber() {
        return Arbitraries.integers().between(1, 9999);
    }
    
    @Provide
    Arbitrary<String> invalidBranchName() {
        return Arbitraries.oneOf(
            // Branch names without slashes
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            // Branch names with slash but no project ID pattern
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                .map(s -> "feature/" + s),
            // Empty or null-like
            Arbitraries.of("", "main", "master", "develop"),
            // Branch names with numbers but no hyphen
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                .map(s -> "feature/" + s + "123")
        );
    }
    
    // Feature: git-mcp-server, Property 28: Commit type is valid
    @Property(tries = 100)
    void commitTypeIsValid(@ForAll("repositoryWithStagedChanges") Path repositoryPath) {
        // Given a repository with staged changes
        CommitMessageGeneratorService serviceWithManager = createServiceWithManager();
        
        // When determining the commit type
        CommitType commitType = serviceWithManager.determineCommitType(repositoryPath);
        
        // Then the commit type should be one of the valid enum values
        Set<CommitType> validTypes = Arrays.stream(CommitType.values()).collect(Collectors.toSet());
        assertThat(commitType).isIn(validTypes);
        assertThat(commitType).isNotNull();
        
        // Cleanup
        cleanupRepository(repositoryPath);
    }
    
    @Provide
    Arbitrary<Path> repositoryWithStagedChanges() {
        return Arbitraries.of(
            createRepoWithTestFiles(),
            createRepoWithDocFiles(),
            createRepoWithSourceFiles(),
            createRepoWithConfigFiles(),
            createRepoWithMixedFiles()
        );
    }
    
    private Path createRepoWithTestFiles() {
        try {
            Path tempDir = Files.createTempDirectory("git-test-");
            Git git = Git.init().setDirectory(tempDir.toFile()).call();
            
            // Create and stage test files
            Path testFile = tempDir.resolve("MyTest.java");
            Files.writeString(testFile, "public class MyTest {}");
            git.add().addFilepattern("MyTest.java").call();
            
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test repository", e);
        }
    }
    
    private Path createRepoWithDocFiles() {
        try {
            Path tempDir = Files.createTempDirectory("git-test-");
            Git git = Git.init().setDirectory(tempDir.toFile()).call();
            
            // Create and stage doc files
            Path docFile = tempDir.resolve("README.md");
            Files.writeString(docFile, "# Documentation");
            git.add().addFilepattern("README.md").call();
            
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test repository", e);
        }
    }
    
    private Path createRepoWithSourceFiles() {
        try {
            Path tempDir = Files.createTempDirectory("git-test-");
            Git git = Git.init().setDirectory(tempDir.toFile()).call();
            
            // Create and stage source files
            Path sourceFile = tempDir.resolve("Main.java");
            Files.writeString(sourceFile, "public class Main {}");
            git.add().addFilepattern("Main.java").call();
            
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test repository", e);
        }
    }
    
    private Path createRepoWithConfigFiles() {
        try {
            Path tempDir = Files.createTempDirectory("git-test-");
            Git git = Git.init().setDirectory(tempDir.toFile()).call();
            
            // Create and stage config files
            Path configFile = tempDir.resolve("config.yml");
            Files.writeString(configFile, "key: value");
            git.add().addFilepattern("config.yml").call();
            
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test repository", e);
        }
    }
    
    private Path createRepoWithMixedFiles() {
        try {
            Path tempDir = Files.createTempDirectory("git-test-");
            Git git = Git.init().setDirectory(tempDir.toFile()).call();
            
            // Create and stage mixed files
            Path sourceFile = tempDir.resolve("Service.java");
            Files.writeString(sourceFile, "public class Service {}");
            Path testFile = tempDir.resolve("ServiceTest.java");
            Files.writeString(testFile, "public class ServiceTest {}");
            
            git.add().addFilepattern("Service.java").call();
            git.add().addFilepattern("ServiceTest.java").call();
            
            return tempDir;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test repository", e);
        }
    }
    
    private CommitMessageGeneratorService createServiceWithManager() {
        JGitRepositoryManager manager = new JGitRepositoryManager();
        return new CommitMessageGeneratorServiceImpl(manager);
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
    
    // Feature: git-mcp-server, Property 29: Generated commit message follows template
    @Property(tries = 100)
    void generatedCommitMessageFollowsTemplate(
        @ForAll("repositoryWithBranch") RepositoryWithBranch repoWithBranch,
        @ForAll("commitSummary") String summary,
        @ForAll("commitDescription") String description
    ) {
        // Given a repository with a branch and staged changes
        Path repositoryPath = repoWithBranch.path;
        String branchName = repoWithBranch.branchName;
        
        CommitMessageGeneratorService serviceWithManager = createServiceWithManager();
        
        // When generating a commit message
        String commitMessage = serviceWithManager.generateCommitMessage(repositoryPath, summary, description);
        
        // Then the message should follow the template format
        assertThat(commitMessage).isNotNull();
        assertThat(commitMessage).isNotEmpty();
        
        String[] lines = commitMessage.split("\n", 2);
        String titleLine = lines[0];
        
        // Verify title line format
        if (branchName.matches("^[^/]+/[A-Za-z]+-\\d+.*")) {
            // Branch has project ID - format should be [PROJECT-ID]:TYPE Summary
            assertThat(titleLine).matches("^\\[[A-Z]+-\\d+\\]:[A-Z]+ .+$");
            
            // Extract and verify project ID is present
            assertThat(titleLine).startsWith("[");
            assertThat(titleLine).contains("]:");
            
            // Extract commit type
            String afterBracket = titleLine.substring(titleLine.indexOf("]:") + 2);
            String commitType = afterBracket.split(" ")[0];
            assertThat(commitType).isIn("FEAT", "FIX", "CHORE", "REFACTOR", "DOCS", "STYLE", "TEST");
        } else {
            // No project ID - format should be TYPE Summary
            assertThat(titleLine).matches("^[A-Z]+ .+$");
            
            // Extract commit type
            String commitType = titleLine.split(" ")[0];
            assertThat(commitType).isIn("FEAT", "FIX", "CHORE", "REFACTOR", "DOCS", "STYLE", "TEST");
        }
        
        // Verify summary is present in title line (trim to handle edge cases)
        assertThat(titleLine).contains(summary.trim());
        
        // If description is provided, verify body format
        if (description != null && !description.isBlank()) {
            assertThat(lines.length).isEqualTo(2);
            String body = lines[1];
            
            // Body should be separated by blank line (which means it starts with \n)
            assertThat(commitMessage).contains("\n\n");
            assertThat(body).startsWith("\n");
            assertThat(body.trim()).isEqualTo(description.trim());
        }
        
        // Cleanup
        cleanupRepository(repositoryPath);
    }
    
    @Provide
    Arbitrary<RepositoryWithBranch> repositoryWithBranch() {
        return Arbitraries.oneOf(
            createRepoWithProjectIdBranch(),
            createRepoWithoutProjectIdBranch()
        );
    }
    
    private Arbitrary<RepositoryWithBranch> createRepoWithProjectIdBranch() {
        return Combinators.combine(
            branchPrefix(),
            projectId(),
            issueNumber()
        ).as((prefix, projId, issueNum) -> {
            try {
                Path tempDir = Files.createTempDirectory("git-test-");
                Git git = Git.init().setDirectory(tempDir.toFile()).call();
                
                // Create initial commit on master/main branch
                Path initialFile = tempDir.resolve("initial.txt");
                Files.writeString(initialFile, "initial");
                git.add().addFilepattern("initial.txt").call();
                git.commit().setMessage("Initial commit").call();
                
                // Now create and checkout the branch
                String branchName = prefix + "/" + projId + "-" + issueNum;
                git.checkout().setCreateBranch(true).setName(branchName).call();
                
                // Create and stage a file
                Path file = tempDir.resolve("file.txt");
                Files.writeString(file, "content");
                git.add().addFilepattern("file.txt").call();
                
                return new RepositoryWithBranch(tempDir, branchName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository", e);
            }
        });
    }
    
    private Arbitrary<RepositoryWithBranch> createRepoWithoutProjectIdBranch() {
        return Arbitraries.of("main", "master", "develop", "mybranch").map(branchName -> {
            try {
                Path tempDir = Files.createTempDirectory("git-test-");
                Git git = Git.init().setDirectory(tempDir.toFile()).call();
                
                // Create initial commit
                Path initialFile = tempDir.resolve("initial.txt");
                Files.writeString(initialFile, "initial");
                git.add().addFilepattern("initial.txt").call();
                git.commit().setMessage("Initial commit").call();
                
                // Create branch if not master
                if (!branchName.equals("master")) {
                    git.checkout().setCreateBranch(true).setName(branchName).call();
                }
                
                // Create and stage a file
                Path file = tempDir.resolve("file.txt");
                Files.writeString(file, "content");
                git.add().addFilepattern("file.txt").call();
                
                return new RepositoryWithBranch(tempDir, branchName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create test repository", e);
            }
        });
    }
    
    @Provide
    Arbitrary<String> commitSummary() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars(' ')
            .ofMinLength(5)
            .ofMaxLength(50)
            .filter(s -> !s.isBlank() && s.trim().length() >= 3);
    }
    
    @Provide
    Arbitrary<String> commitDescription() {
        return Arbitraries.oneOf(
            // Empty description
            Arbitraries.just(""),
            // Null description
            Arbitraries.just(null),
            // Valid description
            Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '\n')
                .ofMinLength(10)
                .ofMaxLength(200)
        );
    }
    
    private static class RepositoryWithBranch {
        final Path path;
        final String branchName;
        
        RepositoryWithBranch(Path path, String branchName) {
            this.path = path;
            this.branchName = branchName;
        }
    }
}
