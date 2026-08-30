package com.cobre.notifications.infrastructure.adapter.out.webhook;

import com.cobre.notifications.application.port.out.WebhookDeliveryResult;
import com.cobre.notifications.application.port.out.WebhookSenderPort;
import com.cobre.notifications.domain.exception.InvalidWebhookUrlException;
import com.cobre.notifications.domain.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class WebhookHttpAdapter implements WebhookSenderPort {

    private static final Logger log = LoggerFactory.getLogger(WebhookHttpAdapter.class);

    private final RestClient restClient;
    private final WebhookUrlValidator urlValidator;

    public WebhookHttpAdapter(RestClient webhookRestClient, WebhookUrlValidator urlValidator) {
        this.restClient = webhookRestClient;
        this.urlValidator = urlValidator;
    }

    @Override
    public WebhookDeliveryResult send(String webhookUrl, NotificationEvent event) {
        try {
            urlValidator.validate(webhookUrl);
        } catch (InvalidWebhookUrlException e) {
            log.error("Refusing to call invalid webhook URL for event {}: {}", event.eventId(), e.getMessage());
            return WebhookDeliveryResult.failure(null, "Invalid webhook URL: " + e.getMessage());
        }

        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "event_id", event.eventId(),
                            "event_type", event.eventType(),
                            "content", event.content(),
                            "created_at", event.createdAt().toString()
                    ))
                    .retrieve()
                    .toBodilessEntity();

            return WebhookDeliveryResult.success(response.getStatusCode().value());
        } catch (RestClientResponseException e) {
            return WebhookDeliveryResult.failure(
                    e.getStatusCode().value(), "HTTP " + e.getStatusCode().value() + ": " + e.getStatusText()
            );
        } catch (RestClientException e) {
            log.warn("Webhook call failed for event {}: {}", event.eventId(), e.getMessage());
            return WebhookDeliveryResult.failure(null, e.getMessage());
        }
    }
}
