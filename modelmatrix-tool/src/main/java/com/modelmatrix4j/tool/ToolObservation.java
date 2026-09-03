package com.modelmatrix4j.tool;

import java.util.List;
import java.util.Objects;

/**
 * Tool-call evidence correlated to one completed core run. Calls preserve invocation order and are
 * defensively copied.
 */
public record ToolObservation(
        String runId,
        String configurationId,
        int repetition,
        List<ToolCallObservation> calls
) {
    /** Identifiers must be non-blank and repetition non-negative. */
    public ToolObservation {
        runId = requireText(runId, "runId");
        configurationId = requireText(configurationId, "configurationId");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be >= 0");
        }
        calls = List.copyOf(Objects.requireNonNull(calls, "calls"));
        if (calls.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("calls must not contain null");
        }
    }

    /** Incidental rendering exposes only the number of captured calls. */
    @Override
    public String toString() {
        return "ToolObservation[runId=" + runId + ", configurationId=" + configurationId
                + ", repetition=" + repetition + ", callCount=" + calls.size() + "]";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
