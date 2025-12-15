package io.sandboxdev.gitmcp.debug.model;

import java.util.Map;

/**
 * Represents a notification channel configuration.
 */
public record NotificationChannel(
    String name,
    NotificationChannelType type,
    Map<String, String> configuration
) {}