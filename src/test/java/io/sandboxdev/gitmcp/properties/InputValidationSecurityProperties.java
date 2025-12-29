package io.sandboxdev.gitmcp.properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sandboxdev.gitmcp.config.GitMcpProperties;
import io.sandboxdev.gitmcp.security.GitInputValidator;
import io.sandboxdev.gitmcp.security.SecurityGuardrails;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for input validation and security guardrails.
 * Tests Property 10: Input Validation and Security (Requirements 9.1, 9.2, 9.3, 9.4, 9.5)
 */
class InputValidationSecurityProperties {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private GitMcpProperties createProperties() {
        return new GitMcpProperties(
                new GitMcpProperties.TransportConfig(true, false, 8080, Duration.ofSeconds(30)),
                new GitMcpProperties.SecurityConfig(List.of("/var/git", "/home/user/repos"), true, 10),
                new GitMcpProperties.RepositoryConfig("main", "10MB"),
                new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
    }

    private final GitInputValidator validator = new GitInputValidator(new SecurityGuardrails(createProperties()));

    /**
     * Property 10: Input Validation and Security (Req 9.1, 9.2)
     * Path traversal sequences are always rejected
     */
    @Property
    void pathTraversalSequencesAlwaysRejected(@ForAll("pathTraversalStrings") String traversalPath) {
        assertThatThrownBy(() -> validator.validateRepositoryPath("/var/git/" + traversalPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal detected");
                
        assertThatThrownBy(() -> validator.validateFilePath(traversalPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal detected");
    }

    /**
     * Property 10: Input Validation and Security (Req 9.1)
     * Safe paths are always accepted
     */
    @Property
    void safePathsAlwaysAccepted(@ForAll("safePaths") String safePath) {
        // Should not throw for safe paths within allowed repositories
        validator.validateRepositoryPath("/var/git/" + safePath);
        validator.validateFilePath(safePath);
    }

    /**
     * Property 10: Input Validation and Security (Req 9.3, 9.4)
     * Shell injection characters in branch names are rejected
     */
    @Property
    void shellInjectionCharactersInBranchNamesRejected(@ForAll("shellInjectionStrings") String maliciousBranch) {
        assertThatThrownBy(() -> validator.validateBranchName(maliciousBranch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid branch name");
    }

    /**
     * Property 10: Input Validation and Security (Req 9.3)
     * Valid branch names are always accepted
     */
    @Property
    void validBranchNamesAlwaysAccepted(@ForAll("validBranchNames") String validBranch) {
        // Should not throw for valid branch names
        validator.validateBranchName(validBranch);
    }

    /**
     * Property 10: Input Validation and Security (Req 9.1)
     * Tool parameter validation enforces schemas
     */
    @Property
    void toolParameterValidationEnforcesSchemas(@ForAll("toolInvocations") ToolInvocation invocation) {
        // This test should fail initially - we need to implement schema validation
        boolean isValid = validator.validateToolParameters(invocation.toolName(), invocation.parameters());
        
        if (invocation.isValidSchema()) {
            assertThat(isValid).isTrue();
        } else {
            assertThat(isValid).isFalse();
        }
    }

    /**
     * Property 10: Input Validation and Security (Req 9.5)
     * Repository allowlist is enforced
     */
    @Property
    void repositoryAllowlistEnforced(@ForAll("repositoryPaths") String repoPath) {
        boolean isAllowed = repoPath.startsWith("/var/git") || repoPath.startsWith("/home/user/repos");
        
        if (isAllowed) {
            // Should not throw for allowed paths (assuming no path traversal)
            if (!repoPath.contains("..")) {
                validator.validateRepositoryPath(repoPath);
            }
        } else {
            // Should throw for disallowed paths
            assertThatThrownBy(() -> validator.validateRepositoryPath(repoPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Repository path not in allowlist");
        }
    }

    // Generators for property-based testing

    @Provide
    Arbitrary<String> pathTraversalStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just("../"),
                Arbitraries.just("..\\"),
                Arbitraries.just("../../"),
                Arbitraries.just("../../../"),
                Arbitraries.just("..\\..\\"),
                Arbitraries.just("./.."),
                Arbitraries.just(".\\.."),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "/../" + s),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "/../../" + s)
        );
    }

    @Provide
    Arbitrary<String> safePaths() {
        return Arbitraries.oneOf(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "/" + s),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "/subdir/" + s + ".txt")
        );
    }

    @Provide
    Arbitrary<String> shellInjectionStrings() {
        return Arbitraries.oneOf(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + ";rm -rf"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "&whoami"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "|cat"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "`cmd`"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "$var"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + " space"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "~1"),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "^2")
        );
    }

    @Provide
    Arbitrary<String> validBranchNames() {
        return Arbitraries.oneOf(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> "feature/" + s),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(10)
                        .map(s -> "bugfix_" + s),
                Arbitraries.strings().withCharRange('0', '9').ofMinLength(1).ofMaxLength(5)
                        .map(s -> "release/v1." + s + ".0"),
                Arbitraries.just("main"),
                Arbitraries.just("develop")
        );
    }

    @Provide
    Arbitrary<String> repositoryPaths() {
        return Arbitraries.oneOf(
                // Allowed paths
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                        .map(s -> "/var/git/" + s),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                        .map(s -> "/home/user/repos/" + s),
                // Disallowed paths
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                        .map(s -> "/tmp/" + s),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                        .map(s -> "/etc/" + s),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20)
                        .map(s -> "/root/" + s)
        );
    }

    @Provide
    Arbitrary<ToolInvocation> toolInvocations() {
        return Arbitraries.oneOf(
                // Valid tool invocations
                Arbitraries.just(new ToolInvocation("git_status", createValidJsonNode("{}"), true)),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(50)
                        .map(msg -> new ToolInvocation("git_commit", 
                                createValidJsonNode("{\"message\":\"" + msg + "\"}"), true)),
                // Invalid tool invocations
                Arbitraries.just(new ToolInvocation("git_commit", createValidJsonNode("{}"), false)),
                Arbitraries.just(new ToolInvocation("unknown_tool", createValidJsonNode("{}"), false))
        );
    }

    private JsonNode createValidJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record ToolInvocation(String toolName, JsonNode parameters, boolean isValidSchema) {}
}
