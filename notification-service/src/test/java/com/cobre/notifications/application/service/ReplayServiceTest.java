package com.cobre.notifications.application.service;

import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationQueryRepositoryPort;
import com.cobre.notifications.domain.exception.NotificationEventNotFoundException;
import com.cobre.notifications.domain.exception.ReplayNotAllowedException;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class ReplayServiceTest {

    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository = mock(DeliveryAttemptRepositoryPort.class);
    private final NotificationQueryRepositoryPort queryRepository = mock(NotificationQueryRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC);
    private ReplayService replayService;

    @BeforeEach
    void setUp() {
        replayService = new ReplayService(deliveryAttemptRepository, queryRepository, clock);
    }

    @Test
    void replayingAnotherClientsEventLooksLikeNotFound() {
        // The query is scoped by clientId, so a mismatched owner simply finds nothing —
        // this is the whole point of the mitigation: never a 403 that confirms the event exists.
        when(queryRepository.findView(eq("CLIENT_ATTACKER"), eq("EVT003"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replayService.replayForClient("CLIENT_ATTACKER", "EVT003"))
                .isInstanceOf(NotificationEventNotFoundException.class);

        verify(deliveryAttemptRepository, never()).findByEventId(any());
        verify(deliveryAttemptRepository, never()).save(any());
    }

    @Test
    void replayingAnUnknownEventIsNotFound() {
        when(queryRepository.findView(eq("CLIENT001"), eq("EVT999"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replayService.replayForClient("CLIENT001", "EVT999"))
                .isInstanceOf(NotificationEventNotFoundException.class);
    }

    @Test
    void replayingANonFailedEventIsRejected() {
        NotificationEventView view = viewWith(DeliveryStatus.PENDING);
        when(queryRepository.findView("CLIENT001", "EVT001")).thenReturn(Optional.of(view));

        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, Instant.parse("2026-08-29T10:00:00Z"));
        when(deliveryAttemptRepository.findByEventId("EVT001")).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> replayService.replayForClient("CLIENT001", "EVT001"))
                .isInstanceOf(ReplayNotAllowedException.class);
    }

    private NotificationEventView viewWith(DeliveryStatus status) {
        return new NotificationEventView(
                "EVT001", "CLIENT001", "credit_card_payment", "content",
                Instant.parse("2026-08-29T10:00:00Z"), status, 0, null, null, null, null
        );
    }
}
