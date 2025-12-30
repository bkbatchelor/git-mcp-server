package io.sandboxdev.gitmcp.stateless;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConcurrentRequestHandler.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrentRequestHandlerTest {

    private final ConcurrentRequestHandler handler = new ConcurrentRequestHandler();

    /**
     * Property 15: Concurrent Request Independence (Req 14.5)
     * Validates that concurrent requests are handled independently
     */
    @Test
    void shouldValidateConcurrentIndependence() {
        List<String> requests = List.of("request1", "request2", "request3");
        
        boolean result = handler.validateConcurrentIndependence(requests);
        
        assertThat(result).isTrue();
    }

    /**
     * Property 15: No Shared Mutable State (Req 14.5)
     * Validates that no shared mutable state exists
     */
    @Test
    void shouldValidateNoSharedMutableState() {
        boolean result = handler.validateNoSharedMutableState();
        
        assertThat(result).isTrue();
    }

    /**
     * Property 15: Single Request Independence
     * Validates that single requests are always independent
     */
    @Test
    void shouldHandleSingleRequestIndependently() {
        List<String> singleRequest = List.of("request1");
        
        boolean result = handler.validateConcurrentIndependence(singleRequest);
        
        assertThat(result).isTrue();
    }
}
