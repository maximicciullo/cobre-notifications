package com.cobre.notifications.infrastructure.adapter.out.webhook;

import com.cobre.notifications.application.port.out.WebhookDeliveryResult;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class WebhookHttpAdapterTest {

    private WireMockServer wireMockServer;
    private WebhookHttpAdapter adapter;
    private final NotificationEvent event = new NotificationEvent(
            "EVT001", "CLIENT001", "credit_card_payment", "Payment received", Instant.parse("2026-08-29T12:00:00Z")
    );

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        // WebhookUrlValidator (SSRF guard) is unit-tested on its own — stubbed here so this
        // test can focus on how the adapter maps HTTP outcomes to WebhookDeliveryResult.
        WebhookUrlValidator validator = mock(WebhookUrlValidator.class);
        doNothing().when(validator).validate(anyString());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(500);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        adapter = new WebhookHttpAdapter(restClient, validator);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void returnsSuccessOn2xxResponse() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse().withStatus(200)));

        WebhookDeliveryResult result = adapter.send(wireMockServer.baseUrl() + "/webhook", event);

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    void returnsFailureOn500Response() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse().withStatus(500)));

        WebhookDeliveryResult result = adapter.send(wireMockServer.baseUrl() + "/webhook", event);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(500);
    }

    @Test
    void returnsFailureOn4xxResponse() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse().withStatus(404)));

        WebhookDeliveryResult result = adapter.send(wireMockServer.baseUrl() + "/webhook", event);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(404);
    }

    @Test
    void returnsFailureOnTimeout() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

        WebhookDeliveryResult result = adapter.send(wireMockServer.baseUrl() + "/webhook", event);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isNull();
        assertThat(result.errorMessage()).isNotBlank();
    }
}
