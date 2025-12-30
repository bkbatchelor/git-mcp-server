package io.sandboxdev.gitmcp.stateless;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityConfigValidator.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigValidatorTest {

    private final SecurityConfigValidator validator = new SecurityConfigValidator();

    /**
     * Property 15: Spring Security Stateless Configuration (Req 14.4)
     * Validates that Spring Security uses STATELESS session creation policy
     */
    @Test
    void shouldReturnStatelessSessionPolicy() {
        SessionCreationPolicy policy = validator.getSessionCreationPolicy();
        
        assertThat(policy).isEqualTo(SessionCreationPolicy.STATELESS);
    }
}
