package com.cobre.notifications.infrastructure.adapter.out.webhook;

import com.cobre.notifications.application.port.out.WebhookDeliveryResult;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class WebhookHttpAdapterTest {

    private HttpServer server;
    private final NotificationEvent event = new NotificationEvent(
            "EVT001", "CLIENT001", "credit_card_payment", "Payment received", Instant.parse("2026-08-29T12:00:00Z")
    );

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private WebhookHttpAdapter adapterWithReadTimeout(int readTimeoutMillis) {
        WebhookUrlValidator validator = mock(WebhookUrlValidator.class);
        doNothing().when(validator).validate(anyString());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(readTimeoutMillis);
        RestClient restClient = RestClient.builder().requestFactory(factory).build();

        return new WebhookHttpAdapter(restClient, validator);
    }

    private String startServerReturning(int statusCode, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/webhook", exchange -> {
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort() + "/webhook";
    }

    @Test
    void returnsSuccessOn2xxResponse() throws IOException {
        String url = startServerReturning(200, 0);

        WebhookDeliveryResult result = adapterWithReadTimeout(2000).send(url, event);

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    void returnsFailureOn500Response() throws IOException {
        String url = startServerReturning(500, 0);

        WebhookDeliveryResult result = adapterWithReadTimeout(2000).send(url, event);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(500);
    }

    @Test
    void returnsFailureOn4xxResponse() throws IOException {
        String url = startServerReturning(404, 0);

        WebhookDeliveryResult result = adapterWithReadTimeout(2000).send(url, event);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(404);
    }

    @Test
    void returnsFailureOnTimeout() throws IOException {
        String url = startServerReturning(200, 2000);

        WebhookDeliveryResult result = adapterWithReadTimeout(300).send(url, event);

        assertThat(result.success()).isFalse();
        assertThat(result.httpStatus()).isNull();
        assertThat(result.errorMessage()).isNotBlank();
    }
}
