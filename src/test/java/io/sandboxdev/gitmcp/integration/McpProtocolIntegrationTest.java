package io.sandboxdev.gitmcp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for end-to-end MCP protocol flows.
 * Tests the complete integration of all components following TDD approach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class McpProtocolIntegrationTest {

    @Autowired
    private McpIntegrationService integrationService;

    /**
     * Integration Test: End-to-End MCP Protocol Flow (All Requirements)
     * Tests complete MCP protocol flow from request to response
     */
    @Test
    void shouldProcessCompleteProtocolFlow() {
        // GREEN phase - use real integration service
        boolean protocolFlowWorks = integrationService.testCompleteProtocolFlow();
        
        assertThat(protocolFlowWorks)
            .as("Complete MCP protocol flow should work end-to-end")
            .isTrue();
    }

    /**
     * Integration Test: Git Operations with Real Repository
     * Tests Git operations integration
     */
    @Test
    void shouldPerformGitOperationsWithRealRepository() {
        // GREEN phase - use real integration service
        boolean gitOperationsWork = integrationService.testGitOperationsIntegration();
        
        assertThat(gitOperationsWork)
            .as("Git operations should work with real repository")
            .isTrue();
    }

    /**
     * Integration Test: Transport Layer Integration
     * Tests transport layer integration with proper Spring test slices
     */
    @Test
    void shouldIntegrateTransportLayer() {
        // GREEN phase - use real integration service
        boolean transportIntegrationWorks = integrationService.testTransportLayerIntegration();
        
        assertThat(transportIntegrationWorks)
            .as("Transport layer integration should work correctly")
            .isTrue();
    }

    /**
     * Integration Test: Security and Validation Integration
     * Tests security guardrails and input validation integration
     */
    @Test
    void shouldIntegrateSecurityAndValidation() {
        // GREEN phase - use real integration service
        boolean securityIntegrationWorks = integrationService.testSecurityAndValidationIntegration();
        
        assertThat(securityIntegrationWorks)
            .as("Security and validation integration should work correctly")
            .isTrue();
    }

    /**
     * Integration Test: Observability Integration
     * Tests metrics, tracing, and logging integration
     */
    @Test
    void shouldIntegrateObservability() {
        // GREEN phase - use real integration service
        boolean observabilityIntegrationWorks = integrationService.testObservabilityIntegration();
        
        assertThat(observabilityIntegrationWorks)
            .as("Observability integration should work correctly")
            .isTrue();
    }
}
