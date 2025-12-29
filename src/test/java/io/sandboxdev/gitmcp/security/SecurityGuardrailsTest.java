package io.sandboxdev.gitmcp.security;

import io.sandboxdev.gitmcp.config.GitMcpProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityGuardrailsTest {

    private GitMcpProperties createProperties(List<String> allowedRepos, int maxConcurrent) {
        return new GitMcpProperties(
                new GitMcpProperties.TransportConfig(true, false, 8080, Duration.ofSeconds(30)),
                new GitMcpProperties.SecurityConfig(allowedRepos, true, maxConcurrent),
                new GitMcpProperties.RepositoryConfig("main", "10MB"),
                new GitMcpProperties.ObservabilityConfig(true, true, "INFO")
        );
    }

    @Test
    @DisplayName("Property 10: Security Guardrails - Repository allowlist enforcement (Req 9.5)")
    void enforcesRepositoryAllowlist() {
        var properties = createProperties(List.of("/var/git/allowed", "/home/user/projects"), 10);
        var guardrails = new SecurityGuardrails(properties);

        assertThat(guardrails.isRepositoryAllowed("/var/git/allowed")).isTrue();
        assertThat(guardrails.isRepositoryAllowed("/var/git/allowed/subdir")).isTrue();
        assertThat(guardrails.isRepositoryAllowed("/home/user/projects")).isTrue();
        assertThat(guardrails.isRepositoryAllowed("/var/git/forbidden")).isFalse();
        assertThat(guardrails.isRepositoryAllowed("/etc/passwd")).isFalse();
    }

    @Test
    @DisplayName("Property 10: Security Guardrails - Rate limiting for resource-intensive operations (Req 10.1)")
    void enforcesRateLimiting() {
        var properties = createProperties(List.of("/var/git"), 2);
        var guardrails = new SecurityGuardrails(properties);

        // First two operations should succeed
        assertThat(guardrails.acquireOperationPermit()).isTrue();
        assertThat(guardrails.acquireOperationPermit()).isTrue();
        
        // Third operation should be rate limited
        assertThat(guardrails.acquireOperationPermit()).isFalse();
        
        // After releasing, should work again
        guardrails.releaseOperationPermit();
        assertThat(guardrails.acquireOperationPermit()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Normal commit message",
        "Fix: resolve issue #123",
        "feat: add new feature\n\nDetailed description here"
    })
    @DisplayName("Property 10: Security Guardrails - Output sanitization for safe content (Req 10.3)")
    void sanitizesOutputForSafeContent(String input) {
        var properties = createProperties(List.of("/var/git"), 10);
        var guardrails = new SecurityGuardrails(properties);
        
        String sanitized = guardrails.sanitizeOutput(input);
        assertThat(sanitized).isEqualTo(input); // Safe content should pass through unchanged
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "javascript:alert(1)",
        "<img src=x onerror=alert(1)>",
        "data:text/html,<script>alert(1)</script>"
    })
    @DisplayName("Property 10: Security Guardrails - Output sanitization removes dangerous content (Req 10.3)")
    void sanitizesOutputRemovesDangerousContent(String input) {
        var properties = createProperties(List.of("/var/git"), 10);
        var guardrails = new SecurityGuardrails(properties);
        
        String sanitized = guardrails.sanitizeOutput(input);
        assertThat(sanitized).doesNotContain("<script>");
        assertThat(sanitized).doesNotContain("javascript:");
        assertThat(sanitized).doesNotContain("onerror=");
        assertThat(sanitized).doesNotContain("data:text/html");
    }

    @Test
    @DisplayName("Property 10: Security Guardrails - Rejects empty allowlist (Req 9.5)")
    void rejectsEmptyAllowlist() {
        var properties = createProperties(List.of(), 10);
        
        assertThatThrownBy(() -> new SecurityGuardrails(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Repository allowlist cannot be empty");
    }

    @Test
    @DisplayName("Property 10: Security Guardrails - Handles null output sanitization (Req 10.3)")
    void handlesNullOutputSanitization() {
        var properties = createProperties(List.of("/var/git"), 10);
        var guardrails = new SecurityGuardrails(properties);
        
        String sanitized = guardrails.sanitizeOutput(null);
        assertThat(sanitized).isEmpty();
    }
}
