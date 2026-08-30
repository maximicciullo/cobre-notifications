package com.cobre.notifications.application.service;

import com.cobre.notifications.application.port.in.ProcessPendingDeliveriesUseCase;
import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.application.port.out.SubscriptionRepositoryPort;
import com.cobre.notifications.application.port.out.WebhookDeliveryResult;
import com.cobre.notifications.application.port.out.WebhookSenderPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.cobre.notifications.domain.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryProcessingService implements ProcessPendingDeliveriesUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliveryProcessingService.class);

    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    private final NotificationEventRepositoryPort notificationEventRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final WebhookSenderPort webhookSender;
    private final Clock clock;
    private final long backoffBaseSeconds;
    private final int batchSize;

    public DeliveryProcessingService(
            DeliveryAttemptRepositoryPort deliveryAttemptRepository,
            NotificationEventRepositoryPort notificationEventRepository,
            SubscriptionRepositoryPort subscriptionRepository,
            WebhookSenderPort webhookSender,
            Clock clock,
            @Value("${notification.delivery.backoff-base-seconds:30}") long backoffBaseSeconds,
            @Value("${notification.delivery.batch-size:20}") int batchSize
    ) {
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.notificationEventRepository = notificationEventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.webhookSender = webhookSender;
        this.clock = clock;
        this.backoffBaseSeconds = backoffBaseSeconds;
        this.batchSize = batchSize;
    }

    @Override
    public int processDueDeliveries() {
        Instant now = Instant.now(clock);
        List<DeliveryAttempt> due = deliveryAttemptRepository.findDue(now, batchSize);
        for (DeliveryAttempt attempt : due) {
            processOne(attempt, now);
        }
        return due.size();
    }

    private void processOne(DeliveryAttempt attempt, Instant now) {
        Optional<NotificationEvent> eventOpt = notificationEventRepository.findById(attempt.eventId());
        if (eventOpt.isEmpty()) {
            log.warn("DeliveryAttempt {} has no matching NotificationEvent, skipping", attempt.eventId());
            return;
        }
        NotificationEvent event = eventOpt.get();

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByClientId(event.clientId());
        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().active()) {
            log.warn("No active subscription for client {}, skipping event {}", event.clientId(), event.eventId());
            return;
        }

        String webhookUrl = subscriptionOpt.get().webhookUrl();
        WebhookDeliveryResult result = webhookSender.send(webhookUrl, event);

        if (result.success()) {
            attempt.recordSuccess(result.httpStatus(), now);
            log.info("Delivered event {} to client {} (http {})", event.eventId(), event.clientId(), result.httpStatus());
        } else {
            attempt.recordFailure(result.httpStatus(), result.errorMessage(), now, backoffBaseSeconds);
            log.warn("Delivery failed for event {} (attempt {}/{}): {}",
                    event.eventId(), attempt.retryCount(), attempt.maxRetries(), result.errorMessage());
        }

        deliveryAttemptRepository.save(attempt);
    }
}
