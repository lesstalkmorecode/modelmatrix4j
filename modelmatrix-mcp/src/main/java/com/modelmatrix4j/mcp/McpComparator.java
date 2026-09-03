package com.modelmatrix4j.mcp;

import com.modelmatrix4j.structured.JsonValueComparator;
import java.util.Objects;

/** Compares ordered MCP tool identity and semantic JSON arguments without protocol metadata. */
public final class McpComparator {
    private final JsonValueComparator jsonComparator = new JsonValueComparator();

    /**
     * Tool count, order, identity, and semantic JSON arguments participate in comparison. Invalid
     * arguments take precedence over mismatch; tool execution result payloads are not part of this
     * MCP evidence model.
     *
     * @throws NullPointerException if either observation is {@code null}
     */
    public McpResult.Status compare(McpObservation baseline, McpObservation candidate) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(candidate, "candidate");

        if (baseline.tools().size() != candidate.tools().size()) {
            return hasInvalidToolArguments(baseline) || hasInvalidToolArguments(candidate)
                    ? McpResult.Status.INVALID
                    : McpResult.Status.MISMATCH;
        }

        boolean mismatch = false;
        for (int index = 0; index < baseline.tools().size(); index++) {
            McpToolInteraction left = baseline.tools().get(index);
            McpToolInteraction right = candidate.tools().get(index);
            if (!left.toolId().equals(right.toolId())) {
                mismatch = true;
            }

            JsonValueComparator.Outcome arguments = jsonComparator.compare(left.argumentsJson(), right.argumentsJson());
            if (arguments == JsonValueComparator.Outcome.DIFFERENT) {
                mismatch = true;
            } else if (arguments != JsonValueComparator.Outcome.EQUIVALENT) {
                return McpResult.Status.INVALID;
            }
        }

        return mismatch ? McpResult.Status.MISMATCH : McpResult.Status.COMPATIBLE;
    }

    private boolean hasInvalidToolArguments(McpObservation observation) {
        return observation.tools().stream()
                .anyMatch(tool -> !jsonComparator.isValid(tool.argumentsJson()));
    }
}
