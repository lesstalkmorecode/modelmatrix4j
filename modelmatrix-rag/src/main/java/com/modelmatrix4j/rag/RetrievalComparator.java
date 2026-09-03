package com.modelmatrix4j.rag;

import java.util.List;
import java.util.Objects;

/** Compares retrieval behavior using ordered stable document identities. */
public final class RetrievalComparator {

    /** Retrieval comparison outcome. */
    public enum Outcome {
        /** Both observations contain the same document identities in the same order. */
        EQUIVALENT,
        /** Document identity or retrieval order differs. */
        MISMATCH
    }

    /**
     * Compares ordered {@link RetrievedDocument#documentId()} values; citation evidence is ignored.
     *
     * @throws NullPointerException if either observation is {@code null}
     */
    public Outcome compare(RetrievalObservation baseline, RetrievalObservation candidate) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(candidate, "candidate");
        return documentIds(baseline).equals(documentIds(candidate))
                ? Outcome.EQUIVALENT
                : Outcome.MISMATCH;
    }

    private static List<String> documentIds(RetrievalObservation observation) {
        return observation.documents().stream()
                .map(RetrievedDocument::documentId)
                .toList();
    }
}
