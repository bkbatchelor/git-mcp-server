package com.example.gitmcp.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all Git MCP operations.
 */
public class GitMcpException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public GitMcpException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public GitMcpException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public GitMcpException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>(details);
    }

    public GitMcpException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public void addDetail(String key, Object value) {
        details.put(key, value);
    }
}
