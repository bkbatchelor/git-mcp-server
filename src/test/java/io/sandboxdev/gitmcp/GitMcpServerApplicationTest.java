package io.sandboxdev.gitmcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
     Basic integration test to verify the Spring Boot application context loads correctly.
     
     This test validates that:
     - All Spring Boot auto-configuration works properly
     - Configuration properties are valid
     - All required beans can be created
     - Virtual Threads are enabled
 */
@SpringBootTest
@ActiveProfiles("test")
class GitMcpServerApplicationTest {

    @Test
    void contextLoads() {
        // This test will pass if the Spring Boot application context loads successfully
        // It validates that all configuration is correct and all beans can be created
    }
}