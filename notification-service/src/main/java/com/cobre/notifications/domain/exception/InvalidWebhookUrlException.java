package com.cobre.notifications.domain.exception;

/** Raised by the SSRF guard (A10 mitigation, see SECURITY.md) before any outbound call. */
public class InvalidWebhookUrlException extends RuntimeException {
    public InvalidWebhookUrlException(String message) {
        super(message);
    }
}
