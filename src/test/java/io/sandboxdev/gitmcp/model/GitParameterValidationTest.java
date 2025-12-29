package io.sandboxdev.gitmcp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/**
     Unit tests for Git parameter validation (TDD RED phase).
 */
class GitParameterValidationTest {

    @Test
    void shouldValidateValidRepositoryPath() {
        String validPath = "/home/user/projects/my-repo";
        
        boolean isValid = GitParameterValidator.isValidRepositoryPath(validPath);
        
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../../etc/passwd",
        "/home/user/../../../etc/passwd",
        "repo/../../../sensitive"
    })
    void shouldRejectPathTraversalAttempts(String maliciousPath) {
        boolean isValid = GitParameterValidator.isValidRepositoryPath(maliciousPath);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldValidateValidBranchName() {
        String validBranch = "feature/new-api-v2";
        
        boolean isValid = GitParameterValidator.isValidBranchName(validBranch);
        
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "branch with spaces",
        "branch\nwith\nnewlines",
        "branch..with..dots",
        ".branch-starting-with-dot"
    })
    void shouldRejectInvalidBranchNames(String invalidBranch) {
        boolean isValid = GitParameterValidator.isValidBranchName(invalidBranch);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldValidateValidCommitMessage() {
        String validMessage = "Add new feature implementation";
        
        boolean isValid = GitParameterValidator.isValidCommitMessage(validMessage);
        
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "message\u0000with\u0000null"
    })
    void shouldRejectInvalidCommitMessages(String invalidMessage) {
        boolean isValid = GitParameterValidator.isValidCommitMessage(invalidMessage);
        
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldValidateValidGitReference() {
        String validRef = "HEAD~1";
        
        boolean isValid = GitParameterValidator.isValidGitReference(validRef);
        
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ref with spaces",
        "ref..with..dots",
        ".ref-starting-with-dot"
    })
    void shouldRejectInvalidGitReferences(String invalidRef) {
        boolean isValid = GitParameterValidator.isValidGitReference(invalidRef);
        
        assertThat(isValid).isFalse();
    }
}
