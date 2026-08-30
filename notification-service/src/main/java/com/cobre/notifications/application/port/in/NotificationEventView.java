package com.cobre.notifications.application.port.in;

import com.cobre.notifications.domain.model.DeliveryStatus;

import java.time.Instant;

/** Read model combining NotificationEvent + its current DeliveryAttempt — what the API returns. */
public record NotificationEventView(
        String eventId,
        String clientId,
        String eventType,
        String content,
        Instant createdAt,
        DeliveryStatus deliveryStatus,
        int retryCount,
        Instant lastAttemptedAt,
        Integer lastHttpStatus,
        String lastError,
        Instant completedAt
) {
}
