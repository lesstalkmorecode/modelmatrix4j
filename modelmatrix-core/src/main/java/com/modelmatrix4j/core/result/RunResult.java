package com.modelmatrix4j.core.result;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable terminal result of one physical model invocation.
 *
 * <p>For completed runs, {@code output} is the normalized and redacted application-visible text
 * produced at the public result boundary. Raw adapter output is normalized before compatibility
 * comparison and sensitive values are redacted before the value is exposed through this record.
 * The resulting text is still transient application data and is not automatically safe for
 * persistence; durable reporting deliberately projects a narrower contract.</p>
 *
 * @param runId stable identity for this scenario/configuration/repetition run
 * @param scenarioId scenario identifier
 * @param model model configuration that was executed
 * @param repetition zero-based repetition index
 * @param status terminal execution status
 * @param output normalized and redacted application-visible output for a completed run; otherwise
 *        the empty string
 * @param duration measured invocation lifecycle duration
 * @param diagnostic sanitized diagnostic text, possibly empty
 */
public record RunResult(
        String runId,
        String scenarioId,
        ModelDescriptor model,
        int repetition,
        RunStatus status,
        String output,
        Duration duration,
        String diagnostic) {

    /**
     * @throws IllegalArgumentException if an identifier is blank, repetition/duration is negative,
     *         or a non-completed run contains output
     */
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
