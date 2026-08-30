package com.cobre.notifications.infrastructure.config;

import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Loads challenge/notification_events.json (copied to src/main/resources/seed/) on startup.
 * Plays the role of the Event Consumer from DESIGN.md §2 for demo purposes: real ingestion
 * would come from the platform's event bus, which isn't part of the challenge timebox.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int MAX_RETRIES = 5;
    private static final long BACKOFF_BASE_SECONDS = 30;

    private final NotificationEventRepositoryPort notificationEventRepository;
    private final DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    private final ObjectMapper objectMapper;

    public DataSeeder(
            NotificationEventRepositoryPort notificationEventRepository,
            DeliveryAttemptRepositoryPort deliveryAttemptRepository,
            ObjectMapper objectMapper
    ) {
        this.notificationEventRepository = notificationEventRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (notificationEventRepository.findById("EVT001").isPresent()) {
            log.info("Seed data already present, skipping.");
            return;
        }

        var resource = new ClassPathResource("seed/notification_events.json");
        SeedFile seedFile = objectMapper.readValue(resource.getInputStream(), SeedFile.class);

        for (SeedEvent seed : seedFile.events()) {
            Instant createdAt = OffsetDateTime.parse(seed.delivery_date()).toInstant();

            notificationEventRepository.save(new NotificationEvent(
                    seed.event_id(), seed.client_id(), seed.event_type(), seed.content(), createdAt
            ));

            DeliveryAttempt attempt = new DeliveryAttempt(seed.event_id(), MAX_RETRIES, createdAt);
            if ("completed".equalsIgnoreCase(seed.delivery_status())) {
                attempt.recordSuccess(200, createdAt);
            } else if ("failed".equalsIgnoreCase(seed.delivery_status())) {
                for (int i = 0; i < MAX_RETRIES; i++) {
                    attempt.recordFailure(504, "Simulated timeout (seed data)", createdAt, BACKOFF_BASE_SECONDS);
                }
            }
            deliveryAttemptRepository.save(attempt);
        }

        log.info("Seeded {} notification event(s) from notification_events.json", seedFile.events().size());
    }

    private record SeedFile(List<SeedEvent> events) {
    }

    private record SeedEvent(
            String event_id,
            String event_type,
            String content,
            String delivery_date,
            String delivery_status,
            String client_id
    ) {
    }
}
