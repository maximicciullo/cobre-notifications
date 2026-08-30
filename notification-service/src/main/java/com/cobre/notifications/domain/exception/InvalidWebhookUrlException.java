package com.cobre.notifications.domain.exception;

public class InvalidWebhookUrlException extends RuntimeException {
    public InvalidWebhookUrlException(String message) {
        super(message);
    }
}
