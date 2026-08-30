package com.cobre.notifications.infrastructure.adapter.out.persistence;

import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.domain.model.NotificationEvent;
import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import com.cobre.notifications.infrastructure.adapter.out.persistence.repository.NotificationEventJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class NotificationEventRepositoryAdapter implements NotificationEventRepositoryPort {

    private final NotificationEventJpaRepository jpaRepository;

    public NotificationEventRepositoryAdapter(NotificationEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificationEvent save(NotificationEvent event) {
        NotificationEventEntity entity = new NotificationEventEntity(
                event.eventId(), event.clientId(), event.eventType(), event.content(), event.createdAt()
        );
        NotificationEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<NotificationEvent> findById(String eventId) {
        return jpaRepository.findById(eventId).map(this::toDomain);
    }

    private NotificationEvent toDomain(NotificationEventEntity entity) {
        return new NotificationEvent(
                entity.getEventId(), entity.getClientId(), entity.getEventType(),
                entity.getContent(), entity.getCreatedAt()
        );
    }
}
