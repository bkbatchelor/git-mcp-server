# Git MCP Server

A production-grade Model Context Protocol (MCP) Server that enables Large Language Models (LLMs) to safely interact with Git repositories through a secure, intelligent bridge.

## Features

- **MCP Protocol Compliance**: Strict adherence to MCP JSON-RPC 2.0 specification
- **Dual Transport Support**: Stdio for local IDE integration and SSE for remote communication
- **Java 21 & Virtual Threads**: High-throughput I/O operations with minimal resource overhead
- **JGit Integration**: Pure Java Git operations without external dependencies
- **Comprehensive Security**: Input validation, path traversal prevention, and access control
- **Stateless Architecture**: Horizontal scalability and fault tolerance
- **Observability**: Distributed tracing, metrics, and structured logging
- **Headless Deployment**: Docker-ready with daemon mode support
- **Property-Based Testing**: Comprehensive test coverage with mutation testing

## Prerequisites

- Java 21 or higher
- Git repository access

## Quick Start

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Run with Stdio transport** (for IDE integration):
   ```bash
   ./gradlew bootRun --args="--spring.profiles.active=dev"
   ```

3. **Run with SSE transport** (for remote access):
   ```bash
   ./gradlew bootRun --args="--git.mcp.transport.sse-enabled=true --git.mcp.transport.stdio-enabled=false"
   ```

## Deployment

### Stdio Transport (IDE Integration)

For local IDE integration using standard input/output:

```bash
# Development mode with debug logging
./gradlew bootRun --args="--spring.profiles.active=dev"

# Production mode with optimized settings
java -jar build/libs/git-mcp-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

**Key Features:**
- JSON-RPC messages via System.out
- All logs redirected to System.err
- Virtual threads for high concurrency
- Stateless operation

### SSE Transport (Remote Access)

For HTTP-based Server-Sent Events transport:

```bash
# Enable SSE on port 8080
java -jar build/libs/git-mcp-server-1.0.0-SNAPSHOT.jar \
  --git.mcp.transport.sse-enabled=true \
  --git.mcp.transport.stdio-enabled=false \
  --git.mcp.transport.sse-port=8080
```

**Endpoints:**
- `GET /sse/{sessionId}` - Establish SSE connection
- `POST /mcp/request` - Send MCP requests
- `GET /actuator/health` - Health check

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
COPY build/libs/git-mcp-server-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build and run container
docker build -t git-mcp-server .
docker run -p 8080:8080 \
  -e GIT_MCP_ALLOWED_REPOSITORIES="/workspace" \
  -v /path/to/repos:/workspace \
  git-mcp-server --spring.profiles.active=prod
```

## Configuration

### Required Environment Variables

```bash
# Repository access (required)
export GIT_MCP_ALLOWED_REPOSITORIES="/path/to/repo1,/path/to/repo2"

# Domain-based access with wildcards
export GIT_MCP_ALLOWED_REPOSITORIES="/home/*/projects/*,/workspace/*,/var/git/*"

# Allow everything (NOT recommended for production)
export GIT_MCP_ALLOWED_REPOSITORIES="/*"

# Optional: API keys for AI integrations
export OPENAI_API_KEY="your-openai-key"
export ANTHROPIC_API_KEY="your-anthropic-key"
```

### Application Configuration

Create `application-local.yml` for custom settings:

```yaml
git:
  mcp:
    transport:
      stdio-enabled: true
      sse-enabled: false
      sse-port: 8080
    security:
      allowed-repositories:
        - /path/to/your/repos
        - /home/*/projects/*     # Domain wildcards
        - /workspace/*           # Any workspace repo
      rate-limiting-enabled: true
      max-requests-per-minute: 60
    repository:
      default-branch: main
      operation-timeout-seconds: 30
    headless:
      daemon-mode: false
      structured-logging: true
    observability:
      tracing-enabled: true
      metrics-enabled: true

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### Profile-Specific Configuration

**Development Profile** (`--spring.profiles.active=dev`):
- Debug logging enabled
- Rate limiting disabled
- All actuator endpoints exposed
- Detailed health information

**Production Profile** (`--spring.profiles.active=prod`):
- Daemon mode enabled
- Structured JSON logging
- Limited actuator endpoints
- Security-focused settings

## Accessing Actuator Endpoints

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Application Info
```bash
curl http://localhost:8080/actuator/info
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Logging Configuration

### Development Logging
```yaml
logging:
  level:
    io.sandboxdev.gitmcp: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId},%X{spanId}] %logger{36} - %msg%n"
```

### Production Logging (Structured JSON)
```yaml
logging:
  level:
    io.sandboxdev.gitmcp: INFO
    org.springframework.security: WARN
  pattern:
    console: '{"timestamp":"%d{yyyy-MM-dd HH:mm:ss}","level":"%-5level","traceId":"%X{traceId}","spanId":"%X{spanId}","logger":"%logger{36}","message":"%msg"}%n'
```

### Log Aggregation
For production deployments with log aggregation systems:

```bash
# Forward logs to external system
java -jar app.jar --spring.profiles.active=prod 2>&1 | your-log-forwarder
```

## Git Operations

The server exposes the following MCP tools:

- `git_status` - Get repository status
- `git_commit` - Create commits
- `git_diff` - Show differences
- `git_branch_list` - List branches
- `git_branch_create` - Create branches
- `git_checkout` - Switch branches
- `git_log` - Show commit history

### Example MCP Request
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "tools/call",
  "params": {
    "name": "git_status",
    "arguments": {
      "repositoryPath": "/path/to/repo"
    }
  }
}
```

## Security

### Repository Access Control
```yaml
git:
  mcp:
    security:
      allowed-repositories:
        # Specific repositories
        - /safe/repo/path
        - /another/safe/path
        
        # Domain-based wildcards
        - /home/*/projects/*     # Any user's projects
        - /workspace/*           # Any workspace repo
        - /opt/repos/*          # Company repos
        - /tmp/git-*            # Temporary Git repos
        
      enable-input-sanitization: true
      max-requests-per-minute: 60
```

**Security Levels:**
- **SECURE**: Specific paths (`/home/user/project`)
- **MODERATE**: Domain wildcards (`/workspace/*`)  
- **INSECURE**: Global access (`/*`) - avoid in production

### Input Validation
- Path traversal prevention (`../` blocked)
- Branch name validation (no shell injection)
- Commit message sanitization
- Repository allowlist enforcement

### Rate Limiting
- Configurable requests per minute
- Per-client rate limiting
- Resource-intensive operation throttling

## Monitoring & Observability

### Distributed Tracing
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### Metrics Collection
- Git operation latency and success rates
- JVM metrics (memory, GC, threads)
- HTTP request metrics
- Custom business metrics

### Health Checks
- Repository accessibility
- Git command availability
- Memory and disk space
- External service connectivity

## Troubleshooting

### Common Issues

**1. Repository Access Denied**
```bash
# Check repository permissions
ls -la /path/to/repo
# Verify allowlist configuration
curl http://localhost:8080/actuator/configprops | grep allowed-repositories
```

**2. High Memory Usage**
```bash
# Check JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
# Adjust heap size
java -Xmx2g -jar app.jar
```

**3. Stdio Transport Issues**
```bash
# Verify System.out is reserved for JSON-RPC
# Check System.err for application logs
java -jar app.jar 2>app.log
```

### Debug Mode
```bash
# Enable debug logging
java -jar app.jar --logging.level.io.sandboxdev.gitmcp=DEBUG
```

## Development

### Running Tests
```bash
# All tests
./gradlew test

# Integration tests only
./gradlew test --tests "*Integration*"

# Property-based tests
./gradlew test --tests "*Properties*"

# Mutation testing
./gradlew pitest
```

### Project Structure
```
src/
├── main/java/io/sandboxdev/gitmcp/
│   ├── config/          # Configuration classes
│   ├── tools/           # MCP Tool implementations
│   ├── resources/       # MCP Resource providers
│   ├── model/           # Java Records (DTOs, Schemas)
│   ├── security/        # Security and validation
│   ├── transport/       # Stdio and SSE transports
│   ├── protocol/        # JSON-RPC dispatcher
│   ├── registry/        # Tool and resource registries
│   ├── service/         # JGit repository manager
│   ├── stateless/       # Stateless operation components
│   ├── headless/        # Headless deployment support
│   └── integration/     # Integration services
└── test/java/           # Test classes
```

## License

This project is licensed under the Apache License 2.0.