package com.cobre.notifications.domain.model;

import java.time.Instant;

public record Subscription(
        String clientId,
        String apiKey,
        String webhookUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
