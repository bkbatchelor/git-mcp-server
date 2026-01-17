package io.sandboxdev.gitmcpserver.mcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public StdioTransport stdioTransport(McpMessageHandler handler) {
        return new StdioTransport(System.in, System.out, handler);
    }
}
