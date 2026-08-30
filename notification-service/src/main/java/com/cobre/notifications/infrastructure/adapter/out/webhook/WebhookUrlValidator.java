package com.cobre.notifications.infrastructure.adapter.out.webhook;

import com.cobre.notifications.domain.exception.InvalidWebhookUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Runs immediately before every outbound webhook call, not only when a subscription's URL is
 * first saved, to prevent DNS-rebinding (a hostname that resolves to a public IP at
 * registration time but a private one later).
 */
@Component
public class WebhookUrlValidator {

    public void validate(String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new InvalidWebhookUrlException("Malformed webhook URL: " + rawUrl);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new InvalidWebhookUrlException("Webhook URL must use https: " + rawUrl);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidWebhookUrlException("Webhook URL is missing a host: " + rawUrl);
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new InvalidWebhookUrlException("Cannot resolve webhook host: " + host);
        }

        if (isDisallowed(address)) {
            throw new InvalidWebhookUrlException(
                    "Webhook URL resolves to a non-public address (" + address.getHostAddress() + "): " + rawUrl
            );
        }
    }

    private boolean isDisallowed(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isLinkLocalAddress()   // covers 169.254.0.0/16 — the cloud metadata service
                || address.isSiteLocalAddress()   // covers RFC1918 private ranges (10/8, 172.16/12, 192.168/16)
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();
    }
}
