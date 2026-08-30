package com.cobre.notifications.infrastructure.adapter.out.persistence;

import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.DeliveryAttemptEntity;
import com.cobre.notifications.infrastructure.adapter.out.persistence.repository.DeliveryAttemptJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DeliveryAttemptRepositoryAdapter implements DeliveryAttemptRepositoryPort {

    // Bumped onto next_retry_at when a row is claimed, so a crashed worker's pick gets retried
    // by the next poll instead of stuck forever — without holding the row lock across the HTTP call.
    private static final Duration CLAIM_LEASE = Duration.ofSeconds(60);

    private final DeliveryAttemptJpaRepository jpaRepository;

    public DeliveryAttemptRepositoryAdapter(DeliveryAttemptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DeliveryAttempt save(DeliveryAttempt attempt) {
        DeliveryAttemptEntity entity = toEntity(attempt);
        DeliveryAttemptEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DeliveryAttempt> findByEventId(String eventId) {
        return jpaRepository.findById(eventId).map(this::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DeliveryAttempt> findDue(Instant now, int limit) {
        List<DeliveryAttemptEntity> claimed = jpaRepository.findDueForUpdate(now, limit);
        claimed.forEach(e -> e.setNextRetryAt(now.plus(CLAIM_LEASE)));
        jpaRepository.saveAll(claimed);
        return claimed.stream().map(this::toDomain).toList();
    }

    private DeliveryAttemptEntity toEntity(DeliveryAttempt attempt) {
        DeliveryAttemptEntity entity = new DeliveryAttemptEntity();
        entity.setEventId(attempt.eventId());
        entity.setStatus(attempt.status());
        entity.setRetryCount(attempt.retryCount());
        entity.setMaxRetries(attempt.maxRetries());
        entity.setNextRetryAt(attempt.nextRetryAt());
        entity.setLastAttemptedAt(attempt.lastAttemptedAt());
        entity.setLastHttpStatus(attempt.lastHttpStatus());
        entity.setLastError(attempt.lastError());
        entity.setCompletedAt(attempt.completedAt());
        return entity;
    }

    private DeliveryAttempt toDomain(DeliveryAttemptEntity entity) {
        return DeliveryAttempt.restore(
                entity.getEventId(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getMaxRetries(),
                entity.getNextRetryAt(),
                entity.getLastAttemptedAt(),
                entity.getLastHttpStatus(),
                entity.getLastError(),
                entity.getCompletedAt()
        );
    }
}
