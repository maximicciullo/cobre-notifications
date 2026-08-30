package com.cobre.notifications.infrastructure.adapter.out.persistence.repository;

import com.cobre.notifications.infrastructure.adapter.out.persistence.entity.DeliveryAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DeliveryAttemptJpaRepository extends JpaRepository<DeliveryAttemptEntity, String> {

    /**
     * Row-level lock + skip-locked so multiple worker instances can poll concurrently without
     * racing on the same rows (A03-safe: bound parameters, no string concatenation — see
     * SECURITY.md). Kept as a short, standalone transaction by the adapter — see
     * DeliveryAttemptRepositoryAdapter for why the lock isn't held across the HTTP call.
     */
    @Query(value = """
            SELECT * FROM delivery_attempt
            WHERE status = 'PENDING'
              AND (next_retry_at IS NULL OR next_retry_at <= :now)
            ORDER BY next_retry_at ASC NULLS FIRST
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<DeliveryAttemptEntity> findDueForUpdate(@Param("now") Instant now, @Param("limit") int limit);
}
