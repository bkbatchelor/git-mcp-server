package io.sandboxdev.gitmcp.model;

/**
 * Git commit tool schema.
 */
public record GitCommitToolSchema(
    String repositoryPath,
    String message
) {}
