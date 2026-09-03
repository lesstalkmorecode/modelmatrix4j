package com.modelmatrix4j.core.execution;

import java.time.Duration;
import java.util.Objects;

/** Package-private immutable execution tuning. */
record ExecutionSettings(int repetitions, Duration timeout, int maxConcurrentInvocations) {
    ExecutionSettings {
        if (repetitions < 1) {
            throw new IllegalArgumentException("repetitions must be positive");
        }
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (maxConcurrentInvocations < 1) {
            throw new IllegalArgumentException("maxConcurrentInvocations must be positive");
        }
    }

    static ExecutionSettings defaults() {
        return new ExecutionSettings(1, Duration.ofSeconds(30), 8);
    }
}

