package com.example.gitmcp.model;

/**
 * Statistics about changes in a diff.
 */
public record DiffStats(
    int filesChanged,
    int insertions,
    int deletions
) {}
