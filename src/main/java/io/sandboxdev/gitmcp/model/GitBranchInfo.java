package io.sandboxdev.gitmcp.model;

/**
 * Git branch information.
 */
public record GitBranchInfo(
    String name,
    String commitHash,
    boolean isCurrent,
    boolean isRemote
) {}
