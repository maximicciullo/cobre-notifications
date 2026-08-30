package com.cobre.notifications.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeliveryStatus {
    PENDING,
    COMPLETED,
    FAILED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static DeliveryStatus fromJson(String value) {
        return DeliveryStatus.valueOf(value.trim().toUpperCase());
    }
}
