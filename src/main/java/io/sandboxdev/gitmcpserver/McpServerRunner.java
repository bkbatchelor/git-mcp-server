package io.sandboxdev.gitmcpserver;

import io.sandboxdev.gitmcpserver.mcp.StdioTransport;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class McpServerRunner implements CommandLineRunner {

    private final StdioTransport stdioTransport;

    public McpServerRunner(StdioTransport stdioTransport) {
        this.stdioTransport = stdioTransport;
    }

    @Override
    public void run(String... args) throws Exception {
        stdioTransport.run();
    }
}
