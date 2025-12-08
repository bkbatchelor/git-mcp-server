package com.example.gitmcp.model;

import java.time.Instant;
import java.util.List;

/**
 * Information about a Git commit.
 */
public record CommitInfo(
    String hash,
    String shortHash,
    String message,
    AuthorInfo author,
    Instant timestamp,
    List<String> changedFiles,
    DiffStats stats
) {}
