package com.cobre.notifications.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;

/** Spring Data interface projection for the NotificationEvent + DeliveryAttempt join. */
public interface NotificationEventProjection {
    String getEventId();
    String getClientId();
    String getEventType();
    String getContent();
    Instant getCreatedAt();
    String getStatus();
    Integer getRetryCount();
    Instant getLastAttemptedAt();
    Integer getLastHttpStatus();
    String getLastError();
    Instant getCompletedAt();
}
