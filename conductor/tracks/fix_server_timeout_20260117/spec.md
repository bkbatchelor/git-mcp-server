# Fix Server Timeout

## Context
The user reports "Request timed out" when using the `git-mcp-server` with Gemini CLI.
The server startup time (Spring Boot) seems to be around 1.7s, which might be close to the timeout limit or there might be other startup latency issues.

## Goals
1. Reduce server startup time.
2. Ensure no output on stdout during startup.
3. Verify connection stability.

## References
- `src/main/resources/application.yml`
- `build.gradle.kts`
