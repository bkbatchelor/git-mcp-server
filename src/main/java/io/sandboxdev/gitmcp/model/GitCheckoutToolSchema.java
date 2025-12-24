package io.sandboxdev.gitmcp.model;

/**
 * Git checkout tool schema.
 */
public record GitCheckoutToolSchema(
    String repositoryPath,
    String branchName
) {}
