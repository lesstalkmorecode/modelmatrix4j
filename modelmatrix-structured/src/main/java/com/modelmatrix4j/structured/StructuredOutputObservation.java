package com.modelmatrix4j.structured;

import java.util.Objects;

/**
 * One raw structured payload captured from one successful core-managed invocation.
 *
 * <p>The raw payload is capability-local evidence and is deliberately hidden from
 * {@link #toString()}.</p>
 *
 * @param runId correlated core run identifier
 * @param configurationId model configuration identifier
 * @param repetition zero-based repetition index
 * @param output raw structured payload captured before core public-result mapping
 */
public record StructuredOutputObservation(
        String runId,
        String configurationId,
        int repetition,
        String output
) {
    /** @throws IllegalArgumentException if an identifier is blank or repetition is negative */
    public StructuredOutputObservation {
        runId = requireText(runId, "runId");
        configurationId = requireText(configurationId, "configurationId");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be zero or greater");
        }
        Objects.requireNonNull(output, "output");
    }

    /** Returns a representation that omits the raw structured payload. */
    @Override
    public String toString() {
        return "StructuredOutputObservation[runId=" + runId
                + ", configurationId=" + configurationId
                + ", repetition=" + repetition
                + ", output=<hidden>]";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
