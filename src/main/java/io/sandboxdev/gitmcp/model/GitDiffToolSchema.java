package io.sandboxdev.gitmcp.model;

import java.util.Optional;

/**
 * Git diff tool schema.
 */
public record GitDiffToolSchema(
        String repositoryPath,
        Optional<String> fromRef,
        Optional<String> toRef,
        Optional<String> filePath) {
}
