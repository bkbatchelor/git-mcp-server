package com.example.gitmcp.model;

/**
 * Information about a Git remote.
 */
public record RemoteInfo(
    String name,
    String url,
    RemoteType type
) {}
