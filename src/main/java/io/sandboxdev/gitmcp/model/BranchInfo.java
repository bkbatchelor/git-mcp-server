package io.sandboxdev.gitmcp.model;

/**
 * Information about a Git branch.
 */
public record BranchInfo(
    String name,
    String commitHash,
    boolean isCurrent
) {}
