package com.modelmatrix4j.tool;

import java.util.Objects;

/**
 * Result of comparing two ordered tool-call sequences.
 *
 * @param status comparison classification
 * @param diagnostic empty when compatible; non-blank explanation otherwise
 */
public record ToolCallComparison(
        Status status,
        String diagnostic
) {
    /** Tool-call comparison status. */
    public enum Status {
        /** Tool names/order and semantic JSON arguments are equivalent. */
        COMPATIBLE,
        /** Tool-call count, identity, order, or valid arguments differ. */
        MISMATCH,
        /** At least one compared tool-call argument is invalid JSON. */
        INVALID_ARGUMENTS
    }

    /** @throws IllegalArgumentException if diagnostic presence does not match the status */
    public ToolCallComparison {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(diagnostic, "diagnostic");
        if (status == Status.COMPATIBLE && !diagnostic.isEmpty()) {
            throw new IllegalArgumentException("compatible comparison must not contain a diagnostic");
        }
        if (status != Status.COMPATIBLE && diagnostic.isBlank()) {
            throw new IllegalArgumentException("non-compatible comparison must contain a diagnostic");
        }
    }
}
