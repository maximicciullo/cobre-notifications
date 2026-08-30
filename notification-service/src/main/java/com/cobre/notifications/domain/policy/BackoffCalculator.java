package com.cobre.notifications.domain.policy;

import java.time.Duration;

/**
 * Pure exponential backoff: baseSeconds * 2^(retryCount - 1).
 * With base=30s: 1st retry -> 30s, 2nd -> 1min, 3rd -> 2min, 4th -> 4min, ...
 * No Spring/framework dependency on purpose — kept trivial to unit test in isolation.
 */
public final class BackoffCalculator {

    private BackoffCalculator() {
    }

    public static Duration nextDelay(int retryCount, long baseSeconds) {
        if (retryCount < 1) {
            throw new IllegalArgumentException("retryCount must be >= 1, was: " + retryCount);
        }
        int exponent = Math.min(retryCount - 1, 20); // guard against overflow on pathological inputs
        long delaySeconds = baseSeconds * (1L << exponent);
        return Duration.ofSeconds(delaySeconds);
    }
}
