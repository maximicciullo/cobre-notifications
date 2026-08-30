package com.cobre.notifications.infrastructure.adapter.out.persistence;

import com.cobre.notifications.application.port.out.SubscriptionRepositoryPort;
import com.cobre.notifications.domain.model.Subscription;
import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.SubscriptionEntity;
import com.cobre.notifications.infrastructure.adapter.out.persistence.repository.SubscriptionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

    private final SubscriptionJpaRepository jpaRepository;

    public SubscriptionRepositoryAdapter(SubscriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Subscription> findByClientId(String clientId) {
        return jpaRepository.findById(clientId).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findByApiKey(String apiKey) {
        return jpaRepository.findByApiKey(apiKey).map(this::toDomain);
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        return new Subscription(
                entity.getClientId(), entity.getApiKey(), entity.getWebhookUrl(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
