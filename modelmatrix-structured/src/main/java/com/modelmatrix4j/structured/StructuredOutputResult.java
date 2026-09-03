package com.modelmatrix4j.structured;

import java.util.List;
import java.util.Objects;

/**
 * Public summary of structured-output validation and cross-configuration compatibility. Raw
 * structured payloads are not included.
 */
public record StructuredOutputResult(
        Status status,
        List<Observation> observations
) {

    public enum Status {
        /** All observations are valid and configurations agree within every repetition. */
        COMPATIBLE,
        /** All observations are valid but at least one same-repetition comparison differs. */
        MISMATCH,
        /** At least one observation fails schema validation. */
        INVALID
    }

    /** Validation summary for one correlated structured-output observation. */
    public record Observation(
            String runId,
            String configurationId,
            int repetition,
            boolean valid,
            String diagnostic
    ) {
        /**
         * A valid observation has an empty diagnostic; an invalid observation has a non-blank
         * diagnostic. Identifiers must be non-blank and repetition non-negative.
         */
        public Observation {
            runId = requireText(runId, "runId");
            configurationId = requireText(configurationId, "configurationId");
            if (repetition < 0) {
                throw new IllegalArgumentException("repetition must be zero or greater");
            }
            Objects.requireNonNull(diagnostic, "diagnostic");
            if (valid && !diagnostic.isEmpty()) {
                throw new IllegalArgumentException("valid observation must not contain a diagnostic");
            }
            if (!valid && diagnostic.isBlank()) {
                throw new IllegalArgumentException("invalid observation must contain a diagnostic");
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

    /**
     * Observations are defensively copied and must be non-empty. {@link Status#INVALID} exactly
     * matches the presence of invalid observations; {@link Status#MISMATCH} requires at least two
     * valid observations.
     */
    public StructuredOutputResult {
        Objects.requireNonNull(status, "status");
        observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
        if (observations.isEmpty()) {
            throw new IllegalArgumentException("observations must not be empty");
        }
        boolean hasInvalid = observations.stream().anyMatch(observation -> !observation.valid());
        if ((status == Status.INVALID) != hasInvalid) {
            throw new IllegalArgumentException("INVALID status must exactly match invalid observations");
        }
        if (status == Status.MISMATCH && observations.size() < 2) {
            throw new IllegalArgumentException("MISMATCH requires at least two valid observations");
        }
    }
}
