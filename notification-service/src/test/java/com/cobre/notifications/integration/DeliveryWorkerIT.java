package com.cobre.notifications.integration;

import com.cobre.notifications.application.port.in.ReplayNotificationEventUseCase;
import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.DeliveryStatus;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.cobre.notifications.infrastructure.adapter.out.persistence.repository.SubscriptionJpaRepository;
import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.SubscriptionEntity;
import com.cobre.notifications.infrastructure.adapter.out.webhook.WebhookUrlValidator;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Exercises the real Delivery Worker (scheduler + service + Postgres + outbound HTTP) against
 * WireMock standing in for the client's webhook endpoint. WireMock necessarily binds to
 * localhost, which the production WebhookUrlValidator correctly rejects (see
 * WebhookUrlValidatorTest) — so only that one component is swapped for a permissive test
 * double here; everything else in the delivery path is exercised for real.
 */
@Import(DeliveryWorkerIT.PermissiveWebhookValidatorConfig.class)
class DeliveryWorkerIT extends PostgresIntegrationTest {

    private static final String CLIENT_ID = "CLIENT003";

    @Autowired
    private NotificationEventRepositoryPort notificationEventRepository;
    @Autowired
    private DeliveryAttemptRepositoryPort deliveryAttemptRepository;
    @Autowired
    private SubscriptionJpaRepository subscriptionJpaRepository;
    @Autowired
    private ReplayNotificationEventUseCase replayUseCase;

    private WireMockServer wireMockServer;

    @BeforeEach
    void startWireMockAndPointSubscriptionAtIt() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        SubscriptionEntity subscription = subscriptionJpaRepository.findById(CLIENT_ID).orElseThrow();
        subscription.setWebhookUrl(wireMockServer.baseUrl() + "/webhook");
        subscriptionJpaRepository.save(subscription);
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    private String seedPendingEvent(String eventId, int maxRetries) {
        Instant createdAt = Instant.now().minusSeconds(60);
        notificationEventRepository.save(new NotificationEvent(eventId, CLIENT_ID, "credit_card_payment", "content", createdAt));
        deliveryAttemptRepository.save(new DeliveryAttempt(eventId, maxRetries, createdAt));
        return eventId;
    }

    @Test
    void successfulResponseMarksTheAttemptCompleted() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)));
        String eventId = seedPendingEvent("EVT-WORKER-OK", 5);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            DeliveryAttempt attempt = deliveryAttemptRepository.findByEventId(eventId).orElseThrow();
            assertThat(attempt.status()).isEqualTo(DeliveryStatus.COMPLETED);
            assertThat(attempt.lastHttpStatus()).isEqualTo(200);
        });
    }

    @Test
    void repeatedFailuresEventuallyDeadLetterTheAttempt() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(500)));
        String eventId = seedPendingEvent("EVT-WORKER-DEADLETTER", 2);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            DeliveryAttempt attempt = deliveryAttemptRepository.findByEventId(eventId).orElseThrow();
            assertThat(attempt.status()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(attempt.retryCount()).isEqualTo(2);
        });
    }

    @Test
    void replayReEntersTheSameDeliveryPathAndSucceeds() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(500)));
        String eventId = seedPendingEvent("EVT-WORKER-REPLAY", 1);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(deliveryAttemptRepository.findByEventId(eventId).orElseThrow().status())
                        .isEqualTo(DeliveryStatus.FAILED));

        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)));
        replayUseCase.replayForClient(CLIENT_ID, eventId);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(deliveryAttemptRepository.findByEventId(eventId).orElseThrow().status())
                        .isEqualTo(DeliveryStatus.COMPLETED));
    }

    @TestConfiguration
    static class PermissiveWebhookValidatorConfig {
        @Bean
        @Primary
        WebhookUrlValidator permissiveWebhookUrlValidator() {
            return new WebhookUrlValidator() {
                @Override
                public void validate(String rawUrl) {
                    // Allows the WireMock localhost URL used in this test. Production SSRF
                    // behavior is covered separately by WebhookUrlValidatorTest.
                }
            };
        }
    }
}
