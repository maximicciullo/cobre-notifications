package com.cobre.notifications.application.port.out;

import com.cobre.notifications.domain.model.NotificationEvent;

import java.util.Optional;

public interface NotificationEventRepositoryPort {
    NotificationEvent save(NotificationEvent event);
    Optional<NotificationEvent> findById(String eventId);
}
