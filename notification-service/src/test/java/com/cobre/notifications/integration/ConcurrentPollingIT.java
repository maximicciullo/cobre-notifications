package com.cobre.notifications.integration;

import com.cobre.notifications.application.port.out.DeliveryAttemptRepositoryPort;
import com.cobre.notifications.application.port.out.NotificationEventRepositoryPort;
import com.cobre.notifications.domain.model.DeliveryAttempt;
import com.cobre.notifications.domain.model.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the row-level locking in DeliveryAttemptJpaRepository#findDueForUpdate actually
 * prevents two concurrent worker instances from claiming the same due attempt — something no
 * mock-based unit test can verify, since it depends on real Postgres row locks.
 */
class ConcurrentPollingIT extends PostgresIntegrationTest {

    private static final String CLIENT_ID = "CLIENT001";
    private static final int TOTAL_ATTEMPTS = 20;
    private static final int WORKER_THREADS = 6;
    private static final int BATCH_SIZE = 3;

    @Autowired
    private NotificationEventRepositoryPort notificationEventRepository;
    @Autowired
    private DeliveryAttemptRepositoryPort deliveryAttemptRepository;

    @Test
    void concurrentWorkersNeverClaimTheSameAttemptTwice() throws InterruptedException {
        Instant createdAt = Instant.now().minusSeconds(60);
        Set<String> expectedIds = new HashSet<>();
        for (int i = 0; i < TOTAL_ATTEMPTS; i++) {
            String eventId = "EVT-CONCURRENT-" + i;
            notificationEventRepository.save(new NotificationEvent(eventId, CLIENT_ID, "credit_card_payment", "content", createdAt));
            deliveryAttemptRepository.save(new DeliveryAttempt(eventId, 5, createdAt));
            expectedIds.add(eventId);
        }

        List<String> claimedIds = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);
        CountDownLatch startLine = new CountDownLatch(1);

        List<Runnable> workers = IntStream.range(0, WORKER_THREADS)
                .<Runnable>mapToObj(i -> () -> {
                    try {
                        startLine.await();
                        List<DeliveryAttempt> claimed;
                        do {
                            claimed = deliveryAttemptRepository.findDue(Instant.now(), BATCH_SIZE);
                            claimed.forEach(a -> claimedIds.add(a.eventId()));
                        } while (!claimed.isEmpty());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                })
                .collect(Collectors.toList());

        workers.forEach(executor::execute);
        startLine.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();

        assertThat(claimedIds).hasSize(TOTAL_ATTEMPTS);
        assertThat(new HashSet<>(claimedIds)).isEqualTo(expectedIds);
    }
}
