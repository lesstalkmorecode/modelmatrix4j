package com.modelmatrix4j.core.result;

import java.util.List;
import java.util.Objects;

public record CompatibilityResult(CompatibilityStatus status, List<RunResult> runs) {
    public CompatibilityResult {
        Objects.requireNonNull(status, "status");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("runs must not be empty");
        }
    }
}
