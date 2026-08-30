package com.cobre.notifications.infrastructure.adapter.in.scheduler;

import com.cobre.notifications.application.port.in.ProcessPendingDeliveriesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryWorkerScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorkerScheduler.class);

    private final ProcessPendingDeliveriesUseCase processPendingDeliveries;

    public DeliveryWorkerScheduler(ProcessPendingDeliveriesUseCase processPendingDeliveries) {
        this.processPendingDeliveries = processPendingDeliveries;
    }

    @Scheduled(fixedDelayString = "${notification.delivery.poll-interval-ms:5000}")
    public void pollAndDeliver() {
        int processed = processPendingDeliveries.processDueDeliveries();
        if (processed > 0) {
            log.info("Processed {} due delivery attempt(s)", processed);
        }
    }
}
