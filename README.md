# Git MCP Server

A Model Context Protocol (MCP) server for Git, enabling AI agents to interact with repositories safely and efficiently.

## Overview

The `git-mcp-server` bridges the gap between Large Language Models (LLMs) and your version control system. It allows AI agents to perform Git operations—starting with reading repository state and commit history—in a structured, controlled environment.

## Features

*   **MCP Infrastructure:** Full implementation of the Model Context Protocol over Standard I/O (Stdio).
*   **Branch Listing:** Retrieve a list of local branches.
*   **Commit History:** Fetch commit logs with structured metadata (hash, author, date, message).
*   **Safety:** Designed with read-only operations first, ensuring safe interaction for AI agents.

## Technology Stack

*   **Language:** Java 21
*   **Framework:** Spring Boot 4.0.1
*   **Build System:** Gradle (Kotlin DSL)
*   **Git Interaction:** Native Git CLI Wrapper

## Prerequisites

*   **Java 21** or later
*   **Git** installed and available on the system PATH

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd git-mcp-server
```

### 2. Build the Project

```bash
./gradlew build
```

### 3. Run the Server

```bash
java -jar build/libs/git-mcp-server-0.0.1-SNAPSHOT.jar
```

The server will start and listen for MCP JSON-RPC messages on Standard Input (stdin).

## Usage

This server is designed to be used by an MCP Client (like an IDE extension or an AI agent runner). However, you can test it manually via the command line.

### Example: List Branches

**Input (stdin):**
```json
{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "list_branches", "arguments": {}}, "id": 1}
```

**Output (stdout):**
```json
{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"main\nfeature/branch..."}]}}
```

### Example: Get Commit Log

**Input (stdin):**
```json
{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "get_log", "arguments": {"count": 5}}, "id": 2}
```

## Available Tools

| Tool Name | Description | Arguments |
| :--- | :--- | :--- |
| `list_branches` | Lists all local branches. | None |
| `get_log` | Retrieves commit history. | `count` (integer, default: 10) |

## Development

### Running Tests

```bash
./gradlew test
```

### Project Structure

*   `src/main/java/io/sandboxdev/gitmcpserver/mcp`: Core MCP infrastructure (JSON-RPC, Transport).
*   `src/main/java/io/sandboxdev/gitmcpserver/git`: Git service and tool definitions.

## License

[LICENSE](LICENSE)
