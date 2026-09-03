package com.modelmatrix4j.core.result;

import java.util.List;
import java.util.Objects;

/**
 * Immutable result of evaluating one scenario across a model matrix.
 *
 * @param status overall matrix compatibility classification
 * @param runs terminal run results in deterministic declaration/repetition order
 */
public record CompatibilityResult(CompatibilityStatus status, List<RunResult> runs) {
    /** @throws IllegalArgumentException if {@code runs} is empty */
    public CompatibilityResult {
        Objects.requireNonNull(status, "status");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("runs must not be empty");
        }
    }
}
