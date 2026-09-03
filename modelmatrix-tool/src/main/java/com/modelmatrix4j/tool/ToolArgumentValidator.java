package com.modelmatrix4j.tool;

import com.modelmatrix4j.structured.JsonValueComparator;
import java.util.Objects;

/** Validates tool-call arguments using the canonical structured JSON parser semantics. */
public final class ToolArgumentValidator {
    private final JsonValueComparator comparator = new JsonValueComparator();

    /**
     * Tests whether arguments contain exactly one valid JSON value.
     *
     * @param arguments serialized tool arguments
     * @return whether arguments satisfy the canonical JSON parser rules
     * @throws NullPointerException if {@code arguments} is {@code null}
     */
    public boolean isValid(String arguments) {
        Objects.requireNonNull(arguments, "arguments");
        return comparator.isValid(arguments);
    }
}
