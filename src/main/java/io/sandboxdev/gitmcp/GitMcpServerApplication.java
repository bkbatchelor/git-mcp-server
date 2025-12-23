package io.sandboxdev.gitmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Git MCP Server.
 * 
 * This application implements the Model Context Protocol (MCP) JSON-RPC 2.0 specification
 * to provide secure Git operations to Large Language Models (LLMs).
 * 
 * Key features:
 * - Java 21 with Virtual Threads for high-throughput I/O
 * - Dual transport support: Stdio and Server-Sent Events (SSE)
 * - JGit pure Java implementation for Git operations
 * - Comprehensive input validation and security guardrails
 * - Distributed tracing and metrics via Micrometer
 */
@SpringBootApplication
public class GitMcpServerApplication {

    public static void main(String[] args) {
        // Enable Virtual Threads for Spring Boot
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(GitMcpServerApplication.class, args);
    }
}