# Specification: Remove ProcessBuilder and replace with JGit

## Overview
Currently, the `git-mcp-server` uses a `ProcessBuilder` wrapper to execute native Git CLI commands. This track aims to replace this implementation with [JGit](https://www.eclipse.org/jgit/), a lightweight, pure Java library implementing the Git version control system. This transition will improve portability, error handling, and performance. Additionally, a robust file-based logging system will be implemented to support debugging without interfering with the MCP JSON-RPC protocol.

## Functional Requirements
- **Complete Migration:** Replace all Git operations currently implemented via `ProcessBuilder` in `GitService.java` with JGit-based implementations.
- **Support Existing Tools:** Ensure all existing MCP tools (`list_branches`, `checkout_branch`, `commit`, etc.) continue to function as expected.
- **Implicit Authentication:** JGit should be configured to use the existing system environment (SSH agents, `~/.ssh/config`, credential helpers).
- **Improved Error Handling:** Utilize JGit's exceptions for specific error messages.
- **File-Based Logging:** Implement persistent logging to a file (e.g., `git-mcp-server.log`). This logging must capture server events and JGit operations.

## Non-Functional Requirements
- **Portability:** The server should no longer require a `git` executable.
- **Reliability:** Maintain or improve the reliability of Git operations.
- **Protocol Safety:** **CRITICAL:** Logging must never output to `stdout`, as this is reserved for the JSON-RPC transport. All logs must go to the configured file or `stderr`.
- **Maintainability:** Use idiomatic JGit patterns.

## Acceptance Criteria
- [ ] All unit tests pass using the JGit implementation.
- [ ] Manual verification confirms tools work correctly on a local repository.
- [ ] Logs are successfully written to the specified log file during operation.
- [ ] No log output appears on `stdout` (verified by ensuring the MCP client connection remains stable).
- [ ] `ProcessBuilder` logic is removed.

## Out of Scope
- New Git features not currently supported.
- UI for credential management.
