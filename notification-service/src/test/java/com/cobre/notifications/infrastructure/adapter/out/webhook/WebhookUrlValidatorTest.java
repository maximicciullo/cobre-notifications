package com.cobre.notifications.infrastructure.adapter.out.webhook;

import com.cobre.notifications.domain.exception.InvalidWebhookUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A10 (SSRF) mitigation tests — see SECURITY.md. Uses IP literals (not hostnames) so the test
 * never depends on real DNS resolution / network access.
 */
class WebhookUrlValidatorTest {

    private final WebhookUrlValidator validator = new WebhookUrlValidator();

    @Test
    void acceptsAPublicHttpsIpLiteral() {
        assertThatCode(() -> validator.validate("https://8.8.8.8/webhook"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPlainHttp() {
        assertThatThrownBy(() -> validator.validate("http://8.8.8.8/webhook"))
                .isInstanceOf(InvalidWebhookUrlException.class)
                .hasMessageContaining("https");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://127.0.0.1/webhook",          // loopback
            "https://169.254.169.254/latest/meta-data/", // cloud metadata service
            "https://10.0.0.1/webhook",            // RFC1918 private
            "https://172.16.0.5/webhook",          // RFC1918 private
            "https://192.168.1.1/webhook",         // RFC1918 private
            "https://0.0.0.0/webhook"               // any-local
    })
    void rejectsNonPublicAddresses(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(InvalidWebhookUrlException.class);
    }

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> validator.validate("not a url"))
                .isInstanceOf(InvalidWebhookUrlException.class);
    }

    @Test
    void rejectsUnresolvableHost() {
        assertThatThrownBy(() -> validator.validate("https://this-host-does-not-exist.invalid/webhook"))
                .isInstanceOf(InvalidWebhookUrlException.class);
    }
}
