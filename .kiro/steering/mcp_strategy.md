#### Protocol Architecture

* **Standard Compliance:** All MCP servers must strictly adhere to the **Model Context Protocol (MCP) JSON-RPC 2.0** specification.
* **Transport Layer:**
  * **Local Integrations (IDE):** Use **Stdio** transport. Rely on **Java 21 Virtual Threads** to manage blocking I/O on `System.in`/`System.out` without stalling the application loop.
  * **Remote Agents:** Use **SSE (Server-Sent Events)** for HTTP-based transport, leveraging Spring Boot’s reactive stack or async servlets.
* **Statelessness:** MCP Servers must be stateless. Context must be passed via the protocol's `sampling` or `resources` primitives, not stored in server memory.

#### Logging & Diagnostics

* **Output Stream Hygiene:**
  * **Stdio Constraint:** `System.out` is strictly reserved for JSON-RPC protocol messages.
  * **Redirection:** All application logs (Spring Boot startup logs, SLF4J output) must be configured to write to `System.err` (Standard Error) to prevent protocol corruption.

* **MCP Protocol Logging:**
  * **Bridge:** Implement a custom SLF4J Appender that forwards log events to the MCP Host (the IDE) using the MCP `notifications/message` capability. This allows the IDE to display agent logs in its UI.
  * **Levels:** Map SLF4J levels (INFO, WARN, ERROR) directly to MCP logging levels.
* **Dynamic Level Management:**
  * **Startup:** Support standard Spring Boot configuration for initial levels (e.g., passing `--logging.level.root=DEBUG` as a CLI argument).
  * **Runtime:** Expose a system tool (e.g., `set_log_level`) that allows the User or the LLM to dynamically adjust the logging verbosity without restarting the server.

#### Resource & Tool Implementation

* **Tool Definitions:**
  * **Schema:** Define Tool input schemas using **Java Records** exclusively to ensure immutability and precise serialization.
  * **Documentation:** Every Tool must have a clear `@Description` (or equivalent metadata) explaining its purpose and parameter constraints to the LLM.
* **Resource Exposure:**
  * **URI Schemes:** Use distinct custom URI schemes (e.g., `kiro-resource://`) to identify managed resources.
  * **Read-Only:** Resources are read-only by default. Mutations must occur strictly through **Tools**.

#### Security & Sandboxing

* **Input Validation:**
  * **Strict Typing:** Validate all incoming JSON-RPC arguments against the Java Record schema before execution.
  * **Path Traversal:** If tools access the file system, strictly enforce an **allowlist** of directories. Reject any paths containing `..` or absolute paths outside the sandbox.
* **Confirmation Loops:**
  * **High-Stakes Actions:** Tools that modify code, delete files, or execute system commands must implement the MCP `confirmation` capability (if supported) or require explicit user consent via the IDE UI.

#### Error Handling

* **JSON-RPC Errors:** Exceptions must be caught and translated into standard JSON-RPC error codes (e.g., `-32603` for Internal Error). Do not leak Java stack traces in the `error.message` field.
* **Graceful Degradation:** If a tool fails, return a human-readable error string as the tool result, allowing the AI model to self-correct rather than crashing the session.
