package com.cobre.notifications.infrastructure.config;

import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class DeliveryBacklogMetrics {

    public DeliveryBacklogMetrics(DeliveryAttemptRepositoryPort deliveryAttemptRepository, MeterRegistry meterRegistry) {
        Gauge.builder("cobre.delivery.backlog", deliveryAttemptRepository, DeliveryAttemptRepositoryPort::countPending)
                .description("Notification deliveries currently PENDING")
                .register(meterRegistry);
    }
}
