package io.sandboxdev.gitmcp.model;

import java.time.Instant;

/**
 * Git commit information.
 */
public record GitCommitInfo(
    String hash,
    String shortHash,
    String author,
    String email,
    Instant timestamp,
    String message
) {}
