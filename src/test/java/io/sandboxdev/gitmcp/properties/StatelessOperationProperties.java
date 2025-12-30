package io.sandboxdev.gitmcp.properties;

import io.sandboxdev.gitmcp.stateless.ConcurrentRequestHandler;
import io.sandboxdev.gitmcp.stateless.SecurityConfigValidator;
import io.sandboxdev.gitmcp.stateless.StatelessValidator;
import net.jqwik.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for stateless operation capabilities.
 * Tests validate that the MCP server operates without session state
 * and handles concurrent requests independently.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(StatelessOperationProperties.TestConfig.class)
class StatelessOperationProperties {

    private final StatelessValidator statelessValidator = new StatelessValidator();
    private final SecurityConfigValidator securityConfigValidator = new SecurityConfigValidator();
    private final ConcurrentRequestHandler concurrentRequestHandler = new ConcurrentRequestHandler();

    /**
     * Property 15: Stateless Operation (Req 14.1, 14.2, 14.3, 14.4, 14.5)
     * Validates that the MCP server operates without session state and handles concurrent requests independently
     */
    @Property
    void shouldOperateWithoutSessionState(@ForAll("requestSequences") java.util.List<String> requests) {
        // Requirement 14.1: SHALL NOT store session state in memory between requests
        boolean hasNoSessionState = statelessValidator.validateNoSessionState(requests);
        assertThat(hasNoSessionState).isTrue();

        // Requirement 14.2: SHALL rely on MCP protocol primitives for context
        boolean usesMcpPrimitives = statelessValidator.validateMcpPrimitiveUsage(requests);
        assertThat(usesMcpPrimitives).isTrue();

        // Requirement 14.3: SHALL resume operation without state recovery after restart
        boolean canResumeWithoutStateRecovery = statelessValidator.validateRestartCapability();
        assertThat(canResumeWithoutStateRecovery).isTrue();
    }

    /**
     * Property 15: Spring Security Stateless Configuration (Req 14.4)
     * Validates that Spring Security uses STATELESS session creation policy
     */
    @Property
    void shouldUseStatelessSecurityPolicy() {
        // Requirement 14.4: SHALL use SessionCreationPolicy.STATELESS for Spring Security
        SessionCreationPolicy sessionPolicy = securityConfigValidator.getSessionCreationPolicy();
        assertThat(sessionPolicy).isEqualTo(SessionCreationPolicy.STATELESS);
    }

    /**
     * Property 15: Concurrent Request Independence (Req 14.5)
     * Validates that concurrent requests are handled independently without shared mutable state
     */
    @Property(tries = 10)
    void shouldHandleConcurrentRequestsIndependently(@ForAll("concurrentRequests") java.util.List<String> requests) {
        // Requirement 14.5: SHALL handle concurrent requests independently without shared mutable state
        boolean handlesIndependently = concurrentRequestHandler.validateConcurrentIndependence(requests);
        assertThat(handlesIndependently)
            .as("Concurrent requests should be handled independently for requests: %s", requests)
            .isTrue();

        // Validate no shared mutable state exists
        boolean hasNoSharedMutableState = concurrentRequestHandler.validateNoSharedMutableState();
        assertThat(hasNoSharedMutableState).isTrue();
    }

    @Provide
    Arbitrary<java.util.List<String>> requestSequences() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50)
                .list()
                .ofMinSize(1)
                .ofMaxSize(10);
    }

    @Provide
    Arbitrary<java.util.List<String>> concurrentRequests() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20)
                .list()
                .ofMinSize(2)
                .ofMaxSize(20);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public StatelessValidator statelessValidator() {
            return new StatelessValidator();
        }

        @Bean
        public SecurityConfigValidator securityConfigValidator() {
            return new SecurityConfigValidator();
        }

        @Bean
        public ConcurrentRequestHandler concurrentRequestHandler() {
            return new ConcurrentRequestHandler();
        }
    }
}
