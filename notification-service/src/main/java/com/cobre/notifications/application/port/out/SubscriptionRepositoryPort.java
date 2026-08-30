package com.cobre.notifications.application.port.out;

import com.cobre.notifications.domain.model.Subscription;

import java.util.Optional;

public interface SubscriptionRepositoryPort {
    Optional<Subscription> findByClientId(String clientId);
    Optional<Subscription> findByApiKey(String apiKey);
}
