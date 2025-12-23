package io.sandboxdev.gitmcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for GitMcpProperties configuration validation.
 * 
 * This test validates that:
 * - Configuration properties are correctly loaded from application-test.yml
 * - All required properties have valid values
 * - Type-safe configuration works as expected
 */
@SpringBootTest(classes = GitMcpPropertiesTest.TestConfiguration.class)
@ActiveProfiles("test")
class GitMcpPropertiesTest {

    @Autowired
    private GitMcpProperties properties;

    @Test
    void shouldLoadConfigurationProperties() {
        assertThat(properties).isNotNull();
        
        // Validate transport configuration
        assertThat(properties.transport().stdioEnabled()).isTrue();
        assertThat(properties.transport().sseEnabled()).isFalse();
        assertThat(properties.transport().ssePort()).isEqualTo(8080);
        
        // Validate security configuration
        assertThat(properties.security().allowedRepositories())
            .containsExactly("/tmp/test-repos");
        assertThat(properties.security().enableInputSanitization()).isTrue();
        assertThat(properties.security().maxConcurrentOperations()).isEqualTo(5);
        
        // Validate repository configuration
        assertThat(properties.repository().defaultBranch()).isEqualTo("main");
        assertThat(properties.repository().maxFileSize()).isEqualTo("1MB");
        
        // Validate observability configuration
        assertThat(properties.observability().tracingEnabled()).isFalse();
        assertThat(properties.observability().metricsEnabled()).isFalse();
        assertThat(properties.observability().logLevel()).isEqualTo("DEBUG");
    }

    @EnableConfigurationProperties(GitMcpProperties.class)
    static class TestConfiguration {
        // Test configuration class
    }
}