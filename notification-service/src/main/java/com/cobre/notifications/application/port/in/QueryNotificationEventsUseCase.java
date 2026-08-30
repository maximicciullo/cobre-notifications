package com.cobre.notifications.application.port.in;

import org.springframework.data.domain.Page;

public interface QueryNotificationEventsUseCase {
    Page<NotificationEventView> query(String clientId, QueryFilter filter);
}
