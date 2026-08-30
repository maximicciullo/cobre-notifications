package com.cobre.notifications.application.port.out;

public record WebhookDeliveryResult(
        boolean success,
        Integer httpStatus,
        String errorMessage
) {
    public static WebhookDeliveryResult success(int httpStatus) {
        return new WebhookDeliveryResult(true, httpStatus, null);
    }

    public static WebhookDeliveryResult failure(Integer httpStatus, String errorMessage) {
        return new WebhookDeliveryResult(false, httpStatus, errorMessage);
    }
}
