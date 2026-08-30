package com.cobre.notifications.infrastructure.adapter.in.web.dto;

import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.domain.model.DeliveryStatus;

import java.time.Instant;

public record NotificationEventResponse(
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
    public static NotificationEventResponse from(NotificationEventView view) {
        return new NotificationEventResponse(
                view.eventId(), view.clientId(), view.eventType(), view.content(), view.createdAt(),
                view.deliveryStatus(), view.retryCount(), view.lastAttemptedAt(), view.lastHttpStatus(),
                view.lastError(), view.completedAt()
        );
    }
}
