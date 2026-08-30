package com.cobre.notifications.infrastructure.adapter.out.persistence.repository;

import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Read-only, composed view spanning notification_event + delivery_attempt. Extends the bare
 * marker {@link Repository} (not JpaRepository) since it only ever reads this joined shape,
 * never the plain entity CRUD surface.
 */
public interface NotificationQueryJpaRepository extends Repository<NotificationEventEntity, String> {

    @Query(value = """
            SELECT n.event_id AS eventId, n.client_id AS clientId, n.event_type AS eventType,
                   n.content AS content, n.created_at AS createdAt,
                   d.status AS status, d.retry_count AS retryCount,
                   d.last_attempted_at AS lastAttemptedAt, d.last_http_status AS lastHttpStatus,
                   d.last_error AS lastError, d.completed_at AS completedAt
            FROM notification_event n
            JOIN delivery_attempt d ON d.event_id = n.event_id
            WHERE n.client_id = :clientId
              AND (CAST(:createdFrom AS timestamptz) IS NULL OR n.created_at >= CAST(:createdFrom AS timestamptz))
              AND (CAST(:createdTo AS timestamptz) IS NULL OR n.created_at <= CAST(:createdTo AS timestamptz))
              AND (CAST(:status AS varchar) IS NULL OR d.status = CAST(:status AS varchar))
            ORDER BY n.created_at DESC
            """,
            countQuery = """
            SELECT count(*)
            FROM notification_event n
            JOIN delivery_attempt d ON d.event_id = n.event_id
            WHERE n.client_id = :clientId
              AND (CAST(:createdFrom AS timestamptz) IS NULL OR n.created_at >= CAST(:createdFrom AS timestamptz))
              AND (CAST(:createdTo AS timestamptz) IS NULL OR n.created_at <= CAST(:createdTo AS timestamptz))
              AND (CAST(:status AS varchar) IS NULL OR d.status = CAST(:status AS varchar))
            """,
            nativeQuery = true)
    Page<NotificationEventProjection> search(
            @Param("clientId") String clientId,
            @Param("createdFrom") Instant createdFrom,
            @Param("createdTo") Instant createdTo,
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = """
            SELECT n.event_id AS eventId, n.client_id AS clientId, n.event_type AS eventType,
                   n.content AS content, n.created_at AS createdAt,
                   d.status AS status, d.retry_count AS retryCount,
                   d.last_attempted_at AS lastAttemptedAt, d.last_http_status AS lastHttpStatus,
                   d.last_error AS lastError, d.completed_at AS completedAt
            FROM notification_event n
            JOIN delivery_attempt d ON d.event_id = n.event_id
            WHERE n.client_id = :clientId AND n.event_id = :eventId
            """, nativeQuery = true)
    Optional<NotificationEventProjection> findView(@Param("clientId") String clientId, @Param("eventId") String eventId);
}
