package io.sandboxdev.gitmcp.model;

import java.util.Optional;

/**
 * Git log tool schema.
 */
public record GitLogToolSchema(
    String repositoryPath,
    Optional<Integer> limit,
    Optional<String> filePath
) {}
