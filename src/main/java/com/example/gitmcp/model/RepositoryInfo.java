package com.example.gitmcp.model;

import java.nio.file.Path;

/**
 * Information about a Git repository.
 */
public record RepositoryInfo(
    Path path,
    String defaultBranch,
    boolean isBare
) {}
