package com.cobre.notifications.application.service;

import com.cobre.notifications.application.port.in.GetNotificationEventUseCase;
import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.in.QueryFilter;
import com.cobre.notifications.application.port.in.QueryNotificationEventsUseCase;
import com.cobre.notifications.application.port.out.NotificationQueryRepositoryPort;
import com.cobre.notifications.domain.exception.NotificationEventNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventQueryService implements QueryNotificationEventsUseCase, GetNotificationEventUseCase {

    private final NotificationQueryRepositoryPort queryRepository;

    public NotificationEventQueryService(NotificationQueryRepositoryPort queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Override
    public Page<NotificationEventView> query(String clientId, QueryFilter filter) {
        return queryRepository.search(clientId, filter);
    }

    @Override
    public NotificationEventView getByIdForClient(String clientId, String eventId) {
        // Ownership is enforced inside the query itself (WHERE client_id = ...), never checked
        // after the fact — a mismatch looks identical to "not found".
        return queryRepository.findView(clientId, eventId)
                .orElseThrow(() -> new NotificationEventNotFoundException(eventId));
    }
}
