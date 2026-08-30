package com.cobre.notifications.application.service;

import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.in.ReplayNotificationEventUseCase;
import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationQueryRepositoryPort;
import com.cobre.notifications.domain.exception.NotificationEventNotFoundException;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ReplayService implements ReplayNotificationEventUseCase {

    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    private final NotificationQueryRepositoryPort queryRepository;
    private final Clock clock;

    public ReplayService(
            DeliveryAttemptRepositoryPort deliveryAttemptRepository,
            NotificationQueryRepositoryPort queryRepository,
            Clock clock
    ) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.queryRepository = queryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public NotificationEventView replayForClient(String clientId, String eventId) {
        // Ownership check first (A01) — mismatch and "doesn't exist" both surface as 404,
        // never a 403 that would confirm another client's event exists (see SECURITY.md).
        queryRepository.findView(clientId, eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));

        DeliveryAttempt attempt = deliveryAttemptRepository.findByEventId(eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));

        // Throws ReplayNotAllowedException (409) unless the attempt is currently FAILED.
        attempt.resetForReplay(Instant.now(clock));
        deliveryAttemptRepository.save(attempt);

        return queryRepository.findView(clientId, eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));
    }
}
