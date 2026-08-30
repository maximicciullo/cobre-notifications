package com.cobre.notifications.domain.model;

import java.time.Instant;

/**
 * Immutable — a business event as reported by the Cobre platform. Never mutated after
 * ingestion; delivery state lives separately in {@link DeliveryAttempt}.
 */
public record NotificationEvent(
        String eventId,
        String clientId,
        String eventType,
        String content,
        Instant createdAt
) {
}
