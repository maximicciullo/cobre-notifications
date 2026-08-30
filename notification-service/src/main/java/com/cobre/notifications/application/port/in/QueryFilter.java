package com.cobre.notifications.application.port.in;

import com.cobre.notifications.domain.model.DeliveryStatus;

import java.time.Instant;

public record QueryFilter(
        Instant createdFrom,
        Instant createdTo,
        DeliveryStatus deliveryStatus,
        int page,
        int size
) {
}
