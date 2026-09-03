package com.modelmatrix4j.rag;

import java.util.List;
import java.util.Objects;

/** Safe public summary of retrieval compatibility; document and citation payloads are not included. */
public record RetrievalResult(
        Status status,
        List<Observation> observations
) {
    public enum Status {
        /** Ordered document identities agree within every repetition. */
        COMPATIBLE,
        /** Valid retrieval evidence differs within at least one repetition. */
        MISMATCH,
        /** At least one observation contains invalid retrieval evidence. */
        INVALID
    }

    /** Safe per-run retrieval summary without document payloads. */
    public record Observation(
            String runId,
            String configurationId,
            int repetition,
            RetrievalInvocation.EvidenceStatus evidenceStatus,
            int documentCount
    ) {
        /**
         * Identifiers must be non-blank, repetition and count non-negative, and invalid evidence has
         * zero documents.
         */
        public Observation {
            runId = requireText(runId, "runId");
            configurationId = requireText(configurationId, "configurationId");
            if (repetition < 0) {
                throw new IllegalArgumentException("repetition must be zero or greater");
            }
            Objects.requireNonNull(evidenceStatus, "evidenceStatus");
            if (documentCount < 0) {
                throw new IllegalArgumentException("documentCount must be zero or greater");
            }
            if (evidenceStatus == RetrievalInvocation.EvidenceStatus.INVALID && documentCount != 0) {
                throw new IllegalArgumentException("invalid retrieval evidence must have zero documentCount");
            }
        }
    }

    /**
     * Observations are defensively copied and must be non-empty. {@link Status#INVALID} exactly
     * tracks invalid evidence; {@link Status#MISMATCH} requires at least two observations.
     */
    public RetrievalResult {
        Objects.requireNonNull(status, "status");
        observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
        if (observations.isEmpty()) {
            throw new IllegalArgumentException("observations must not be empty");
        }
        boolean hasInvalid = observations.stream().anyMatch(observation ->
                observation.evidenceStatus() == RetrievalInvocation.EvidenceStatus.INVALID);
        if (status == Status.INVALID && !hasInvalid) {
            throw new IllegalArgumentException("INVALID requires at least one invalid observation");
        }
        if (status != Status.INVALID && hasInvalid) {
            throw new IllegalArgumentException(status + " cannot contain invalid observations");
        }
        if (status == Status.MISMATCH && observations.size() < 2) {
            throw new IllegalArgumentException("MISMATCH requires at least two observations");
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
