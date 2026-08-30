package com.cobre.notifications.infrastructure.adapter.out.persistence;

import com.cobre.notifications.application.port.in.NotificationEventView;
import com.cobre.notifications.application.port.in.QueryFilter;
import com.cobre.notifications.application.port.out.NotificationQueryRepositoryPort;
import com.cobre.notifications.domain.model.DeliveryStatus;
import com.cobre.notifications.infrastructure.adapter.out.persistence.repository.NotificationEventProjection;
import com.cobre.notifications.infrastructure.adapter.out.persistence.repository.NotificationQueryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class NotificationQueryRepositoryAdapter implements NotificationQueryRepositoryPort {

    private final NotificationQueryJpaRepository jpaRepository;

    public NotificationQueryRepositoryAdapter(NotificationQueryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Page<NotificationEventView> search(String clientId, QueryFilter filter) {
        var pageable = PageRequest.of(filter.page(), filter.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        String status = filter.deliveryStatus() == null ? null : filter.deliveryStatus().name();

        Page<NotificationEventProjection> page = jpaRepository.search(
                clientId, filter.createdFrom(), filter.createdTo(), status, pageable
        );
        return page.map(this::toView);
    }

    @Override
    public Optional<NotificationEventView> findView(String clientId, String eventId) {
        return jpaRepository.findView(clientId, eventId).map(this::toView);
    }

    private NotificationEventView toView(NotificationEventProjection p) {
        return new NotificationEventView(
                p.getEventId(), p.getClientId(), p.getEventType(), p.getContent(), p.getCreatedAt(),
                DeliveryStatus.valueOf(p.getStatus()), p.getRetryCount(), p.getLastAttemptedAt(),
                p.getLastHttpStatus(), p.getLastError(), p.getCompletedAt()
        );
    }
}
