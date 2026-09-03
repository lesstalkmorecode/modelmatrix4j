package com.modelmatrix4j.junit;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;

/**
 * Supplies the scenario, model matrix, and execution settings used by {@link ModelMatrixTest}.
 * Implementations should return immutable/stable configuration for the duration of one test
 * invocation.
 */
public interface ModelMatrixSource {
    Scenario scenario();

    /** Model declaration order is preserved by matrix execution. */
    List<ModelUnderTest> models();

    /** Positive repetition count; defaults to {@code 1}. */
    default int repetitions() {
        return 1;
    }

    /** Positive per-repetition budget, including concurrency-admission wait; defaults to 30 seconds. */
    default Duration timeout() {
        return Duration.ofSeconds(30);
    }

    /** Positive maximum number of physically active/admitted invocations; defaults to {@code 8}. */
    default int maxConcurrentInvocations() {
        return 8;
    }
}
