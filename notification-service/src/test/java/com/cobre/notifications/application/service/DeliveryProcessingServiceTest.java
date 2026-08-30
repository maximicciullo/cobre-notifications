package com.cobre.notifications.application.service;

import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.application.port.out.SubscriptionRepositoryPort;
import com.cobre.notifications.application.port.out.WebhookDeliveryResult;
import com.cobre.notifications.application.port.out.WebhookSenderPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.DeliveryStatus;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.cobre.notifications.domain.model.Subscription;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryProcessingServiceTest {

    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository = mock(DeliveryAttemptRepositoryPort.class);
    private final NotificationEventRepositoryPort notificationEventRepository = mock(NotificationEventRepositoryPort.class);
    private final SubscriptionRepositoryPort subscriptionRepository = mock(SubscriptionRepositoryPort.class);
    private final WebhookSenderPort webhookSender = mock(WebhookSenderPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC);

    private final NotificationEvent event = new NotificationEvent(
            "EVT001", "CLIENT001", "credit_card_payment", "content", Instant.parse("2026-08-29T10:00:00Z")
    );
    private final Subscription activeSubscription = new Subscription(
            "CLIENT001", "key", "https://example.com/hook", true, Instant.now(), Instant.now()
    );

    private DeliveryProcessingService service(long backoffBaseSeconds) {
        return new DeliveryProcessingService(
                deliveryAttemptRepository, notificationEventRepository, subscriptionRepository,
                webhookSender, clock, new SimpleMeterRegistry(), backoffBaseSeconds, 20
        );
    }

    @BeforeEach
    void setUp() {
        when(notificationEventRepository.findById("EVT001")).thenReturn(Optional.of(event));
        when(subscriptionRepository.findByClientId("CLIENT001")).thenReturn(Optional.of(activeSubscription));
    }

    @Test
    void noDueAttemptsMeansNoWork() {
        when(deliveryAttemptRepository.findDue(any(), eq(20))).thenReturn(List.of());

        int processed = service(30).processDueDeliveries();

        assertThat(processed).isZero();
        verify(webhookSender, never()).send(any(), any());
    }

    @Test
    void successfulDeliveryMarksAttemptCompleted() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, Instant.parse("2026-08-29T10:00:00Z"));
        when(deliveryAttemptRepository.findDue(any(), eq(20))).thenReturn(List.of(attempt));
        when(webhookSender.send(eq("https://example.com/hook"), eq(event))).thenReturn(WebhookDeliveryResult.success(200));

        int processed = service(30).processDueDeliveries();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<DeliveryAttempt> saved = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(DeliveryStatus.COMPLETED);
        assertThat(saved.getValue().lastHttpStatus()).isEqualTo(200);
    }

    @Test
    void failedDeliverySchedulesRetryWithBackoff() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, Instant.parse("2026-08-29T10:00:00Z"));
        when(deliveryAttemptRepository.findDue(any(), eq(20))).thenReturn(List.of(attempt));
        when(webhookSender.send(eq("https://example.com/hook"), eq(event)))
                .thenReturn(WebhookDeliveryResult.failure(504, "timeout"));

        service(30).processDueDeliveries();

        ArgumentCaptor<DeliveryAttempt> saved = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(saved.getValue().retryCount()).isEqualTo(1);
        assertThat(saved.getValue().nextRetryAt()).isEqualTo(Instant.parse("2026-08-29T12:00:30Z"));
    }

    @Test
    void missingEventSkipsWithoutCallingWebhook() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT999", 5, Instant.parse("2026-08-29T10:00:00Z"));
        when(deliveryAttemptRepository.findDue(any(), eq(20))).thenReturn(List.of(attempt));
        when(notificationEventRepository.findById("EVT999")).thenReturn(Optional.empty());

        service(30).processDueDeliveries();

        verify(webhookSender, never()).send(any(), any());
        verify(deliveryAttemptRepository, never()).save(any());
    }

    @Test
    void inactiveSubscriptionSkipsWithoutCallingWebhook() {
        DeliveryAttempt attempt = new DeliveryAttempt("EVT001", 5, Instant.parse("2026-08-29T10:00:00Z"));
        when(deliveryAttemptRepository.findDue(any(), eq(20))).thenReturn(List.of(attempt));
        when(subscriptionRepository.findByClientId("CLIENT001")).thenReturn(Optional.empty());

        service(30).processDueDeliveries();

        verify(webhookSender, never()).send(any(), any());
        verify(deliveryAttemptRepository, never()).save(any());
    }
}
