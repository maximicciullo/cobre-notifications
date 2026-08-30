package com.cobre.notifications.application.port.in;

public interface GetNotificationEventUseCase {
    NotificationEventView getByIdForClient(String clientId, String eventId);
}
