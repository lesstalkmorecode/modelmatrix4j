package com.modelmatrix4j.core.execution;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.RunStatus;
import java.time.Duration;
import java.util.Objects;

/**
 * Unified internal outcome of one repetition of one model for one scenario.
 */
record ExecutionOutcome(
        String runId,
        String scenarioId,
        ModelDescriptor model,
        int repetition,
        State state) {

    ExecutionOutcome {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(scenarioId, "scenarioId");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(state, "state");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be zero or greater");
        }
    }

    sealed interface State permits Completed, Failed {
        Duration duration();
    }

    record Completed(String normalizedOutput, Duration duration) implements State {
        public Completed {
            Objects.requireNonNull(normalizedOutput, "normalizedOutput");
            Objects.requireNonNull(duration, "duration");
            if (duration.isNegative()) {
                throw new IllegalArgumentException("duration must not be negative");
            }
        }
    }

    record Failed(RunStatus status, String diagnostic, Duration duration) implements State {
        public Failed {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(diagnostic, "diagnostic");
            Objects.requireNonNull(duration, "duration");
            if (duration.isNegative()) {
                throw new IllegalArgumentException("duration must not be negative");
            }
            if (status == RunStatus.COMPLETED) {
                throw new IllegalArgumentException("Failed status must not be COMPLETED");
            }
        }
    }
}

