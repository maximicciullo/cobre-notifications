package com.cobre.notifications.domain.exception;

/**
 * Also thrown when an event exists but belongs to a different client (A01 mitigation —
 * ownership mismatch and "doesn't exist" must look identical to the caller, see SECURITY.md).
 */
public class NotificationEventNotFoundException extends RuntimeException {
    public NotificationEventNotFoundException(String eventId) {
        super("Notification event not found: " + eventId);
    }
}
