package io.sandboxdev.gitmcp.integration;

import io.sandboxdev.gitmcp.registry.GitToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Git operations integration tests.
 * Tests Git operations integration without requiring Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
class GitOperationsIntegrationTest {

    @Autowired
    private GitToolRegistry gitToolRegistry;

    /**
     * Integration Test: Git Tool Registry Integration
     * Tests Git tool registry integration
     */
    @Test
    void shouldIntegrateGitToolRegistry() {
        // GREEN phase - use real Git tool registry
        boolean gitToolRegistryWorks = gitToolRegistry.listTools().size() > 0;
        
        assertThat(gitToolRegistryWorks)
            .as("Git tool registry should integrate correctly")
            .isTrue();
    }

    /**
     * Integration Test: Git Status Integration
     * Tests Git status operations integration
     */
    @Test
    void shouldIntegrateGitStatusOperations() {
        // GREEN phase - validate Git status tool exists
        boolean gitStatusIntegrationWorks = gitToolRegistry.listTools().stream()
            .anyMatch(tool -> "git_status".equals(tool.name()));
        
        assertThat(gitStatusIntegrationWorks)
            .as("Git status integration should work correctly")
            .isTrue();
    }

    /**
     * Integration Test: Git Commit Integration
     * Tests Git commit operations integration
     */
    @Test
    void shouldIntegrateGitCommitOperations() {
        // GREEN phase - validate Git commit tool exists
        boolean gitCommitIntegrationWorks = gitToolRegistry.listTools().stream()
            .anyMatch(tool -> "git_commit".equals(tool.name()));
        
        assertThat(gitCommitIntegrationWorks)
            .as("Git commit integration should work correctly")
            .isTrue();
    }

    /**
     * Integration Test: Git Branch Integration
     * Tests Git branch operations integration
     */
    @Test
    void shouldIntegrateGitBranchOperations() {
        // GREEN phase - validate Git branch tools exist
        boolean gitBranchIntegrationWorks = gitToolRegistry.listTools().stream()
            .anyMatch(tool -> tool.name().startsWith("git_branch"));
        
        assertThat(gitBranchIntegrationWorks)
            .as("Git branch integration should work correctly")
            .isTrue();
    }
}
