package com.cobre.notifications.domain.exception;

public class ReplayNotAllowedException extends RuntimeException {
    public ReplayNotAllowedException(String message) {
        super(message);
    }
}
