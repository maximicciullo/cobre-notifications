package com.cobre.notifications.infrastructure.adapter.out.persistence.repository;

import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, String> {
    Optional<SubscriptionEntity> findByApiKey(String apiKey);
}
