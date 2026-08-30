package com.cobre.notifications.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "subscription")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity {

    @Id
    @Column(name = "client_id", nullable = false, updatable = false)
    private String clientId;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    // TODO: this is where the real destination URL goes once it's provided on presentation
    // day — see V2__seed_subscriptions.sql and README.md "How to point it at the real endpoint".
    @Column(name = "webhook_url", nullable = false, length = 2048)
    private String webhookUrl;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
