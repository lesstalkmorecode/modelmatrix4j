package com.modelmatrix4j.tool;

import com.modelmatrix4j.structured.JsonValueComparator;
import java.util.List;
import java.util.Objects;

/** Compares ordered tool calls by tool identity and semantic JSON arguments. */
public final class ToolCallComparator {

    private final JsonValueComparator argumentComparator = new JsonValueComparator();

    /**
     * Tool names and call order must match. For corresponding calls with matching identities,
     * malformed arguments are {@link ToolCallComparison.Status#INVALID_ARGUMENTS}; otherwise
     * arguments are compared by semantic JSON value. Tool results are not compared.
     *
     * @param expected reference tool-call sequence
     * @param actual candidate tool-call sequence
     * @return comparison status and bounded diagnostic
     * @throws NullPointerException if either list or a call is {@code null}
     */
    public ToolCallComparison compare(
            List<ToolCallObservation> expected,
            List<ToolCallObservation> actual
    ) {
        expected = List.copyOf(Objects.requireNonNull(expected, "expected"));
        actual = List.copyOf(Objects.requireNonNull(actual, "actual"));

        if (expected.size() != actual.size()) {
            return mismatch("tool-call count differs");
        }

        for (int index = 0; index < expected.size(); index++) {
            ToolCallObservation left = expected.get(index);
            ToolCallObservation right = actual.get(index);

            if (!left.toolName().equals(right.toolName())) {
                return mismatch("tool differs at index " + index);
            }

            JsonValueComparator.Outcome argumentOutcome = argumentComparator.compare(
                    left.arguments(),
                    right.arguments()
            );

            if (argumentOutcome == JsonValueComparator.Outcome.INVALID_LEFT
                    || argumentOutcome == JsonValueComparator.Outcome.INVALID_RIGHT
                    || argumentOutcome == JsonValueComparator.Outcome.BOTH_INVALID) {
                return new ToolCallComparison(
                        ToolCallComparison.Status.INVALID_ARGUMENTS,
                        "invalid tool arguments at index " + index
                );
            }

            if (argumentOutcome == JsonValueComparator.Outcome.DIFFERENT) {
                return mismatch("tool arguments differ at index " + index);
            }
        }

        return new ToolCallComparison(ToolCallComparison.Status.COMPATIBLE, "");
    }

    private static ToolCallComparison mismatch(String diagnostic) {
        return new ToolCallComparison(ToolCallComparison.Status.MISMATCH, diagnostic);
    }
}
