package io.sandboxdev.gitmcp.model;

/**
 * Credentials for Git remote operations.
 */
public record Credentials(
    String username,
    String password
) {
    public boolean isEmpty() {
        return username == null || username.isBlank();
    }
}
