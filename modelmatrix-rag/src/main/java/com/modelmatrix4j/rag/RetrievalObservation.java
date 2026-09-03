package com.modelmatrix4j.rag;

import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral retrieval evidence correlated to one completed core run. Document order is
 * significant; incidental rendering exposes document count but not citation evidence.
 */
public record RetrievalObservation(
        String runId,
        String configurationId,
        int repetition,
        RetrievalInvocation.EvidenceStatus evidenceStatus,
        List<RetrievedDocument> documents
) {
    /** Treats the supplied documents as valid retrieval evidence. */
    public RetrievalObservation(String runId, String configurationId, int repetition,
                                List<RetrievedDocument> documents) {
        this(runId, configurationId, repetition, RetrievalInvocation.EvidenceStatus.VALID, documents);
    }

    /**
     * Documents are defensively copied. Identifiers must be non-blank, repetition non-negative, and
     * invalid evidence cannot contain documents.
     */
    public RetrievalObservation {
        runId = requireText(runId, "runId");
        configurationId = requireText(configurationId, "configurationId");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be zero or greater");
        }
        Objects.requireNonNull(evidenceStatus, "evidenceStatus");
        documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
        if (evidenceStatus == RetrievalInvocation.EvidenceStatus.INVALID && !documents.isEmpty()) {
            throw new IllegalArgumentException("invalid retrieval evidence must not contain documents");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return "RetrievalObservation[runId=" + runId
                + ", configurationId=" + configurationId
                + ", repetition=" + repetition
                + ", evidenceStatus=" + evidenceStatus
                + ", documentCount=" + documents.size() + "]";
    }
}
