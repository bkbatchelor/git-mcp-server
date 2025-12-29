package io.sandboxdev.gitmcp.properties;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import io.sandboxdev.gitmcp.security.GitInputValidator;
import io.sandboxdev.gitmcp.security.SecurityGuardrails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputValidationPropertiesTest {

    private GitMcpProperties createProperties() {
        return new GitMcpProperties(
                new GitMcpProperties.TransportConfig(true, false, 8080, Duration.ofSeconds(30)),
                new GitMcpProperties.SecurityConfig(List.of("/var/git"), true, 10),
                new GitMcpProperties.RepositoryConfig("main", "10MB"),
                new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
    }

    private final GitInputValidator validator = new GitInputValidator(new SecurityGuardrails(createProperties()));

    @ParameterizedTest
    @ValueSource(strings = { "../etc/passwd", "foo/../bar", "test/../../secret", "..\\windows" })
    void rejectsPathTraversalInRepositoryPath(String path) {
        assertThatThrownBy(() -> validator.validateRepositoryPath("/var/git/" + path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal detected");
    }

    @ParameterizedTest
    @ValueSource(strings = { "../etc/passwd", "foo/../bar", "test/../../secret" })
    void rejectsPathTraversalInFilePath(String path) {
        assertThatThrownBy(() -> validator.validateFilePath(path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal detected");
    }

    @ParameterizedTest
    @ValueSource(strings = { "feature/my-branch", "bugfix_123", "main", "develop", "release/v1.0.0" })
    void acceptsSafeBranchNames(String branchName) {
        validator.validateBranchName(branchName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "branch with spaces",
            "branch;rm -rf",
            "branch&whoami",
            "branch|cat",
            "branch>file",
            "branch<input",
            "branch`cmd`",
            "branch$var",
            "branch(test)",
            "-invalid",
            "branch..name",
            "branch~1",
            "branch^2",
            "branch:colon",
            "branch?question",
            "branch*star",
            "branch[bracket"
    })
    void rejectsUnsafeBranchNames(String branchName) {
        assertThatThrownBy(() -> validator.validateBranchName(branchName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid branch name");
    }

    @Test
    void rejectsEmptyRepositoryPath() {
        assertThatThrownBy(() -> validator.validateRepositoryPath(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository path cannot be empty");
    }

    @Test
    void rejectsNullRepositoryPath() {
        assertThatThrownBy(() -> validator.validateRepositoryPath(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository path cannot be empty");
    }
}
