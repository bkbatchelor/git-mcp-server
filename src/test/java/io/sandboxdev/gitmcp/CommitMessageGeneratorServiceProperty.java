package io.sandboxdev.gitmcp;

import io.sandboxdev.gitmcp.service.CommitMessageGeneratorService;
import io.sandboxdev.gitmcp.service.impl.CommitMessageGeneratorServiceImpl;
import net.jqwik.api.*;


import java.util.Optional;

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
}
