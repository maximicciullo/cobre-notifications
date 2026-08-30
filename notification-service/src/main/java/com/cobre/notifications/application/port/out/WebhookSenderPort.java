package com.cobre.notifications.application.port.out;

import com.cobre.notifications.domain.model.NotificationEvent;

public interface WebhookSenderPort {
    WebhookDeliveryResult send(String webhookUrl, NotificationEvent event);
}
