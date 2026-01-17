# Track Specification: Create Project README

## Overview
This track focuses on creating a comprehensive `README.md` file for the `git-mcp-server` project. The README will serve as the primary entry point for users and developers, documenting the project's purpose, features, installation, and usage.

## Objectives
- Create a professional `README.md` file.
- Document the project's core value proposition (MCP for Git).
- Provide clear instructions for building and running the server.
- specific examples of how to interact with the server via Standard I/O (MCP protocol).
- List the available tools (`list_branches`, `get_log`).

## Content Requirements
The README must include:
1.  **Project Title & Description:** Clear and concise.
2.  **Features:** Bulleted list of capabilities.
3.  **Technology Stack:** Java 21, Spring Boot, Native Git CLI.
4.  **Prerequisites:** Java 21, Git.
5.  **Getting Started:**
    -   Clone instructions.
    -   Build instructions (`./gradlew build`).
    -   Run instructions (`java -jar ...`).
6.  **Usage Guide:**
    -   Explanation of Stdio Transport.
    -   JSON-RPC examples for `tools/list` and `tools/call`.
7.  **Development:**
    -   Running tests.
    -   Project structure overview.
8.  **License:** Reference the LICENSE file.

## Success Criteria
- `README.md` exists in the project root.
- All sections are populated with accurate information.
- Code snippets are copy-pasteable and functional.
