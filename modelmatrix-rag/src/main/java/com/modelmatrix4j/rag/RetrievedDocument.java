package com.modelmatrix4j.rag;

import java.util.Objects;
import java.util.Optional;

/**
 * Provider-neutral identity for one retrieved document.
 *
 * <p>{@code documentId} is the stable logical comparison identity supplied by the adapter or test
 * fixture. ModelMatrix4J does not infer cross-store identity from content, provider IDs, URIs, or
 * chunk metadata. List position defines retrieval order. Citation evidence is optional and is not
 * part of default compatibility or incidental rendering.</p>
 */
public record RetrievedDocument(
        String documentId,
        Optional<String> citation
) {
    /** @throws IllegalArgumentException if the identifier or present citation is blank */
    public RetrievedDocument {
        documentId = requireText(documentId, "documentId");
        citation = Objects.requireNonNull(citation, "citation")
                .map(value -> requireText(value, "citation"));
    }

    public RetrievedDocument(String documentId) {
        this(documentId, Optional.empty());
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
        return "RetrievedDocument[documentId=" + documentId + "]";
    }
}
