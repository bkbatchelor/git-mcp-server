package io.sandboxdev.gitmcp.stateless;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StatelessValidator.
 */
@SpringBootTest
@ActiveProfiles("test")
class StatelessValidatorTest {

    private final StatelessValidator validator = new StatelessValidator();

    /**
     * Property 15: Stateless Operation - No Session State (Req 14.1)
     * Validates that no session state is stored in memory between requests
     */
    @Test
    void shouldValidateNoSessionState() {
        List<String> requests = List.of("request1", "request2", "request3");
        
        boolean result = validator.validateNoSessionState(requests);
        
        assertThat(result).isTrue();
    }

    /**
     * Property 15: Stateless Operation - MCP Primitives (Req 14.2)
     * Validates that MCP protocol primitives are used for context
     */
    @Test
    void shouldValidateMcpPrimitiveUsage() {
        List<String> requests = List.of("request1", "request2");
        
        boolean result = validator.validateMcpPrimitiveUsage(requests);
        
        assertThat(result).isTrue();
    }

    /**
     * Property 15: Stateless Operation - Restart Capability (Req 14.3)
     * Validates that server can restart without state recovery
     */
    @Test
    void shouldValidateRestartCapability() {
        boolean result = validator.validateRestartCapability();
        
        assertThat(result).isTrue();
    }
}
