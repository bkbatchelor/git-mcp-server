package io.sandboxdev.gitmcp.model;

/**
 * Information about a commit author.
 */
public record AuthorInfo(
    String name,
    String email
) {}
