package com.cobre.notifications.domain.model;

import com.cobre.notifications.domain.exception.ReplayNotAllowedException;
import com.cobre.notifications.domain.policy.BackoffCalculator;

import java.time.Instant;

/**
 * Mutable — tracks the current delivery state for one {@link NotificationEvent}, 1:1.
 * Owns every state transition (success, failure/backoff, dead-letter, replay) so the rules
 * live in one place instead of being scattered across services (see DESIGN.md §4).
 */
public class DeliveryAttempt {

    private final String eventId;
    private DeliveryStatus status;
    private int retryCount;
    private final int maxRetries;
    private Instant nextRetryAt;
    private Instant lastAttemptedAt;
    private Integer lastHttpStatus;
    private String lastError;
    private Instant completedAt;

    /** New attempt for a freshly-ingested event. */
    public DeliveryAttempt(String eventId, int maxRetries, Instant createdAt) {
        this.eventId = eventId;
        this.maxRetries = maxRetries;
        this.status = DeliveryStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = createdAt;
    }

    /** Rehydration from storage — used by persistence adapters, not by business code. */
    public static DeliveryAttempt restore(
            String eventId,
            DeliveryStatus status,
            int retryCount,
            int maxRetries,
            Instant nextRetryAt,
            Instant lastAttemptedAt,
            Integer lastHttpStatus,
            String lastError,
            Instant completedAt
    ) {
        DeliveryAttempt attempt = new DeliveryAttempt(eventId, maxRetries, nextRetryAt);
        attempt.status = status;
        attempt.retryCount = retryCount;
        attempt.nextRetryAt = nextRetryAt;
        attempt.lastAttemptedAt = lastAttemptedAt;
        attempt.lastHttpStatus = lastHttpStatus;
        attempt.lastError = lastError;
        attempt.completedAt = completedAt;
        return attempt;
    }

    public boolean isDue(Instant now) {
        return status == DeliveryStatus.PENDING
                && (nextRetryAt == null || !nextRetryAt.isAfter(now));
    }

    public void recordSuccess(int httpStatus, Instant now) {
        this.status = DeliveryStatus.COMPLETED;
        this.lastHttpStatus = httpStatus;
        this.lastAttemptedAt = now;
        this.completedAt = now;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void recordFailure(Integer httpStatus, String error, Instant now, long backoffBaseSeconds) {
        this.retryCount++;
        this.lastAttemptedAt = now;
        this.lastHttpStatus = httpStatus;
        this.lastError = error;
        if (this.retryCount >= this.maxRetries) {
            this.status = DeliveryStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            this.nextRetryAt = now.plus(BackoffCalculator.nextDelay(this.retryCount, backoffBaseSeconds));
        }
    }

    /** Replay is only allowed from a definitively FAILED (dead-lettered) attempt. */
    public void resetForReplay(Instant now) {
        if (this.status != DeliveryStatus.FAILED) {
            throw new ReplayNotAllowedException(
                    "Only a FAILED delivery can be replayed (current status: " + this.status + ")"
            );
        }
        this.status = DeliveryStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = now;
        this.completedAt = null;
    }

    public String eventId() {
        return eventId;
    }

    public DeliveryStatus status() {
        return status;
    }

    public int retryCount() {
        return retryCount;
    }

    public int maxRetries() {
        return maxRetries;
    }

    public Instant nextRetryAt() {
        return nextRetryAt;
    }

    public Instant lastAttemptedAt() {
        return lastAttemptedAt;
    }

    public Integer lastHttpStatus() {
        return lastHttpStatus;
    }

    public String lastError() {
        return lastError;
    }

    public Instant completedAt() {
        return completedAt;
    }
}
