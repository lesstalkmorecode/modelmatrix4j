package com.modelmatrix4j.rag;

import java.util.List;
import java.util.Objects;

/**
 * Generation output and retrieval evidence produced by the same physical invocation. Incidental
 * rendering omits both output and document evidence.
 */
public record RetrievalInvocation(
        String output,
        EvidenceStatus evidenceStatus,
        List<RetrievedDocument> documents
) {
    public enum EvidenceStatus {
        /** Retrieval evidence was captured and normalized reliably. */
        VALID,
        /** Retrieval evidence could not be captured or normalized reliably; this is not a compatibility mismatch. */
        INVALID
    }

    /** Treats the supplied documents as valid retrieval evidence. */
    public RetrievalInvocation(String output, List<RetrievedDocument> documents) {
        this(output, EvidenceStatus.VALID, documents);
    }

    /** Documents are defensively copied; invalid evidence cannot contain documents. */
    public RetrievalInvocation {
        output = Objects.requireNonNull(output, "output");
        Objects.requireNonNull(evidenceStatus, "evidenceStatus");
        documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
        if (evidenceStatus == EvidenceStatus.INVALID && !documents.isEmpty()) {
            throw new IllegalArgumentException("invalid retrieval evidence must not contain documents");
        }
    }

    /** Marks retrieval evidence invalid while preserving completed generation output. */
    public static RetrievalInvocation invalidEvidence(String output) {
        return new RetrievalInvocation(output, EvidenceStatus.INVALID, List.of());
    }

    @Override
    public String toString() {
        return "RetrievalInvocation[evidenceStatus=" + evidenceStatus
                + ", documentCount=" + documents.size() + "]";
    }
}
