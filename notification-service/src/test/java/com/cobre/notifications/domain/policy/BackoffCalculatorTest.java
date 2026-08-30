package com.cobre.notifications.domain.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackoffCalculatorTest {

    @Test
    void firstRetryUsesBaseDelay() {
        assertThat(BackoffCalculator.nextDelay(1, 30)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void delayDoublesEachRetry() {
        assertThat(BackoffCalculator.nextDelay(1, 30)).isEqualTo(Duration.ofSeconds(30));
        assertThat(BackoffCalculator.nextDelay(2, 30)).isEqualTo(Duration.ofSeconds(60));
        assertThat(BackoffCalculator.nextDelay(3, 30)).isEqualTo(Duration.ofSeconds(120));
        assertThat(BackoffCalculator.nextDelay(4, 30)).isEqualTo(Duration.ofSeconds(240));
    }

    @Test
    void rejectsRetryCountBelowOne() {
        assertThatThrownBy(() -> BackoffCalculator.nextDelay(0, 30))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void guardsAgainstOverflowOnPathologicalRetryCounts() {
        assertThat(BackoffCalculator.nextDelay(1000, 30)).isPositive();
    }
}
