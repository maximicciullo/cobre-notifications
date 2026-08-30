package com.cobre.notifications.integration;

import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventApiIT extends PostgresIntegrationTest {

    private static final String CLIENT001_KEY = "demo-api-key-client001";
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private NotificationEventRepositoryPort notificationEventRepository;
    @Autowired
    private DeliveryAttemptRepositoryPort deliveryAttemptRepository;

    private HttpEntity<Void> withApiKey(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        return new HttpEntity<>(headers);
    }

    private void seedEvent(String eventId, String clientId, boolean failed) {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        notificationEventRepository.save(new NotificationEvent(eventId, clientId, "credit_card_payment", "content", createdAt));
        DeliveryAttempt attempt = new DeliveryAttempt(eventId, 3, createdAt);
        if (failed) {
            attempt.recordFailure(504, "timeout", createdAt, 1);
            attempt.recordFailure(504, "timeout", createdAt, 1);
            attempt.recordFailure(504, "timeout", createdAt, 1);
        } else {
            attempt.recordSuccess(200, createdAt);
        }
        deliveryAttemptRepository.save(attempt);
    }

    @Test
    void listReturnsOnlyTheCallingClientsEvents() {
        seedEvent("EVT-LIST-1", "CLIENT001", false);
        seedEvent("EVT-LIST-2", "CLIENT002", false);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events", HttpMethod.GET, withApiKey(CLIENT001_KEY), JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).extracting(e -> e.get("client_id")).containsOnly("CLIENT001");
    }

    @Test
    void getOneReturnsTheEventForItsOwner() {
        seedEvent("EVT-GET-OWNED", "CLIENT001", false);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events/EVT-GET-OWNED", HttpMethod.GET, withApiKey(CLIENT001_KEY), JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("delivery_status", "completed");
    }

    @Test
    void getOneAnotherClientsEventReturns404() {
        seedEvent("EVT-GET-OTHER", "CLIENT002", false);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events/EVT-GET-OTHER", HttpMethod.GET, withApiKey(CLIENT001_KEY), JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requestWithoutApiKeyIsUnauthorized() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events", HttpMethod.GET, HttpEntity.EMPTY, JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void replaySuccessfullyResetsAFailedAttempt() {
        seedEvent("EVT-REPLAY-OK", "CLIENT001", true);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events/EVT-REPLAY-OK/replay", HttpMethod.POST, withApiKey(CLIENT001_KEY), JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("delivery_status", "pending");
    }

    @Test
    void replayingANonFailedEventReturns409() {
        seedEvent("EVT-REPLAY-CONFLICT", "CLIENT001", false);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events/EVT-REPLAY-CONFLICT/replay", HttpMethod.POST, withApiKey(CLIENT001_KEY), JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void replayingAnotherClientsEventReturns404() {
        seedEvent("EVT-REPLAY-OTHER", "CLIENT002", true);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/notification_events/EVT-REPLAY-OTHER/replay", HttpMethod.POST, withApiKey(CLIENT001_KEY), JSON_OBJECT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
