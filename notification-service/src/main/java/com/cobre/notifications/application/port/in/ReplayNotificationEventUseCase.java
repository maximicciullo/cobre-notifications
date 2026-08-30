package com.cobre.notifications.application.port.in;

public interface ReplayNotificationEventUseCase {
    NotificationEventView replayForClient(String clientId, String eventId);
}
