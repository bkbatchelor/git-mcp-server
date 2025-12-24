package io.sandboxdev.gitmcp.model;

import java.util.List;

/**
 * Git repository status information.
 */
public record GitStatus(
    List<String> modifiedFiles,
    List<String> stagedFiles,
    List<String> untrackedFiles,
    boolean isClean
) {}
