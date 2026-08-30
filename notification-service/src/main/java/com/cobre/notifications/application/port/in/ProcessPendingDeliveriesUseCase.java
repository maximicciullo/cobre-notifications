package com.cobre.notifications.application.port.in;

public interface ProcessPendingDeliveriesUseCase {
    /** @return number of delivery attempts processed in this cycle */
    int processDueDeliveries();
}
