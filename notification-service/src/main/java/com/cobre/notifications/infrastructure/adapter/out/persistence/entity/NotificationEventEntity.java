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
@Table(name = "notification_event")
@Getter
@Setter
@NoArgsConstructor
public class NotificationEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private String clientId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public NotificationEventEntity(String eventId, String clientId, String eventType, String content, Instant createdAt) {
        this.eventId = eventId;
        this.clientId = clientId;
        this.eventType = eventType;
        this.content = content;
        this.createdAt = createdAt;
    }
}
