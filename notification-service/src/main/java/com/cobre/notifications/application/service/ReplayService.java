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
        // A mismatched owner and a missing event must look identical to the caller — never
        // reveal that another client's event exists.
        queryRepository.findView(clientId, eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));

        DeliveryAttempt attempt = deliveryAttemptRepository.findByEventId(eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));

        attempt.resetForReplay(Instant.now(clock));
        deliveryAttemptRepository.save(attempt);

        return queryRepository.findView(clientId, eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));
    }
}
