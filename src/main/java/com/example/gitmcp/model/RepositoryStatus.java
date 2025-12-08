package com.example.gitmcp.model;

import java.util.List;

/**
 * Current status of a Git repository.
 */
public record RepositoryStatus(
    String currentBranch,
    List<String> stagedFiles,
    List<String> unstagedFiles,
    List<String> untrackedFiles,
    boolean hasUncommittedChanges
) {}
