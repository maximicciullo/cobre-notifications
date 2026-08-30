package com.cobre.notifications.application.port.out;

import com.cobre.notifications.domain.model.DeliveryAttempt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryAttemptRepositoryPort {
    DeliveryAttempt save(DeliveryAttempt attempt);
    Optional<DeliveryAttempt> findByEventId(String eventId);

    /**
     * Atomically claims up to {@code limit} due attempts so multiple worker instances can poll
     * concurrently without double-delivering (see DESIGN.md §5 — "safe concurrent polling").
     */
    List<DeliveryAttempt> findDue(Instant now, int limit);
}
