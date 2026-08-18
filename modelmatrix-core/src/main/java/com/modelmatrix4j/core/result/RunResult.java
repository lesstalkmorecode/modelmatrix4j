package com.modelmatrix4j.core.result;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.time.Duration;
import java.util.Objects;

public record RunResult(
        String runId,
        String scenarioId,
        ModelDescriptor model,
        int repetition,
        RunStatus status,
        String output,
        Duration duration,
        String diagnostic) {

    public RunResult {
        runId = requireText(runId, "runId");
        scenarioId = requireText(scenarioId, "scenarioId");
        Objects.requireNonNull(model, "model");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be zero or greater");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        Objects.requireNonNull(diagnostic, "diagnostic");
        if (status != RunStatus.COMPLETED && !output.isEmpty()) {
            throw new IllegalArgumentException("only completed runs may contain output");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
