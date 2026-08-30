package com.cobre.notifications.domain.model;

import com.cobre.notifications.domain.exception.ReplayNotAllowedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryAttemptTest {

    private final Instant now = Instant.parse("2026-08-29T12:00:00Z");

    @Test
    void newAttemptIsPendingAndDue() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, now);

        assertThat(attempt.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(attempt.isDue(now)).isTrue();
    }

    @Test
    void recordSuccessMarksCompleted() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, now);

        attempt.recordSuccess(200, now);

        assertThat(attempt.status()).isEqualTo(DeliveryStatus.COMPLETED);
        assertThat(attempt.lastHttpStatus()).isEqualTo(200);
        assertThat(attempt.completedAt()).isEqualTo(now);
        assertThat(attempt.isDue(now)).isFalse();
    }

    @Test
    void recordFailureSchedulesRetryUntilMaxRetriesReached() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 3, now);

        attempt.recordFailure(504, "timeout", now, 30);
        assertThat(attempt.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(attempt.retryCount()).isEqualTo(1);
        assertThat(attempt.nextRetryAt()).isEqualTo(now.plusSeconds(30));

        attempt.recordFailure(504, "timeout", now, 30);
        assertThat(attempt.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(attempt.retryCount()).isEqualTo(2);

        attempt.recordFailure(504, "timeout", now, 30);
        assertThat(attempt.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(attempt.retryCount()).isEqualTo(3);
        assertThat(attempt.nextRetryAt()).isNull();
        assertThat(attempt.isDue(now.plusSeconds(999))).isFalse();
    }

    @Test
    void resetForReplayOnlyAllowedFromFailed() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 1, now);
        attempt.recordFailure(504, "timeout", now, 30);
        assertThat(attempt.status()).isEqualTo(DeliveryStatus.FAILED);

        attempt.resetForReplay(now.plusSeconds(100));

        assertThat(attempt.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(attempt.retryCount()).isZero();
        assertThat(attempt.nextRetryAt()).isEqualTo(now.plusSeconds(100));
    }

    @Test
    void resetForReplayRejectsNonFailedAttempt() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, now);

        assertThatThrownBy(() -> attempt.resetForReplay(now))
                .isInstanceOf(ReplayNotAllowedException.class)
                .hasMessageContaining("PENDING");
    }
}
