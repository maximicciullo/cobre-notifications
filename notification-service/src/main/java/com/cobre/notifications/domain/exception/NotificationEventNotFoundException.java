package com.cobre.notifications.domain.exception;

/** Also thrown when an event exists but belongs to a different client — the two cases must look identical to the caller. */
public class NotificationEventNotFoundException extends RuntimeException {
    public NotificationEventNotFoundException(String eventId) {
        super("Notification event not found: " + eventId);
    }
}
