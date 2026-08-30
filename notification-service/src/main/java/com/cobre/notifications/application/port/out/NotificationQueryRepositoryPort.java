package com.cobre.notifications.application.port.out;

import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.in.QueryFilter;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface NotificationQueryRepositoryPort {
    /** clientId is always mandatory — never an optional filter. */
    Page<NotificationEventView> search(String clientId, QueryFilter filter);

    Optional<NotificationEventView> findView(String clientId, String eventId);
}
