package com.cobre.notifications.application.port.out;

import com.cobre.notifications.domain.model.DeliveryAttempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryAttemptRepositoryPort {
    DeliveryAttempt save(DeliveryAttempt attempt);
    Optional<DeliveryAttempt> findByEventId(String eventId);

    /** Atomically claims up to {@code limit} due attempts so concurrent workers never race on the same row. */
    List<DeliveryAttempt> findDue(Instant now, int limit);
}
