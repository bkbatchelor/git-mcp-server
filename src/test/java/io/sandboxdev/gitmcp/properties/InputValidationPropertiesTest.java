package io.sandboxdev.gitmcp.properties;

import io.sandboxdev.gitmcp.security.GitInputValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputValidationPropertiesTest {

    private final GitInputValidator validator = new GitInputValidator();

    @ParameterizedTest
    @ValueSource(strings = { "../etc/passwd", "foo/../bar", "test/../../secret", "..\\windows" })
    @DisplayName("Property 10: Input Validation - Rejects path traversal in repository paths (Req 9.1, 9.2)")
    void rejectsPathTraversalInRepositoryPath(String path) {
        assertThatThrownBy(() -> validator.validateRepositoryPath("/var/git/" + path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal detected");
    }

    @ParameterizedTest
    @ValueSource(strings = { "../etc/passwd", "foo/../bar", "test/../../secret" })
    @DisplayName("Property 10: Input Validation - Rejects path traversal in file paths (Req 9.1, 9.2)")
    void rejectsPathTraversalInFilePath(String path) {
        assertThatThrownBy(() -> validator.validateFilePath(path))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path traversal detected");
    }

    @ParameterizedTest
    @ValueSource(strings = { "feature/my-branch", "bugfix_123", "main", "develop", "release/v1.0.0" })
    @DisplayName("Property 10: Input Validation - Accepts safe branch names (Req 9.3)")
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
    @DisplayName("Property 10: Input Validation - Rejects unsafe branch names with shell injection chars (Req 9.3, 9.4)")
    void rejectsUnsafeBranchNames(String branchName) {
        assertThatThrownBy(() -> validator.validateBranchName(branchName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid branch name");
    }

    @Test
    @DisplayName("Property 10: Input Validation - Rejects empty repository path (Req 9.5)")
    void rejectsEmptyRepositoryPath() {
        assertThatThrownBy(() -> validator.validateRepositoryPath(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository path cannot be empty");
    }

    @Test
    @DisplayName("Property 10: Input Validation - Rejects null repository path (Req 9.5)")
    void rejectsNullRepositoryPath() {
        assertThatThrownBy(() -> validator.validateRepositoryPath(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository path cannot be empty");
    }
}
