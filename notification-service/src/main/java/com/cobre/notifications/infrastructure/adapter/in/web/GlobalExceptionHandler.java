package com.cobre.notifications.infrastructure.adapter.in.web;

import com.cobre.notifications.domain.exception.NotificationEventNotFoundException;
import com.cobre.notifications.domain.exception.ReplayNotAllowedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationEventNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotificationEventNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.getMessage()));
    }

    @ExceptionHandler(ReplayNotAllowedException.class)
    public ResponseEntity<Object> handleReplayNotAllowed(ReplayNotAllowedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(e.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Object> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(errorBody("Invalid request: " + e.getMessage()));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of("timestamp", Instant.now().toString(), "message", message);
    }
}
