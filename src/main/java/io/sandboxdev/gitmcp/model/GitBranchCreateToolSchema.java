package io.sandboxdev.gitmcp.model;

/**
 * Git branch create tool schema.
 */
public record GitBranchCreateToolSchema(
    String repositoryPath,
    String branchName
) {}
