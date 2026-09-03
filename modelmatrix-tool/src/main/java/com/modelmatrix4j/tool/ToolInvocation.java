package com.modelmatrix4j.tool;

import java.util.List;
import java.util.Objects;

/**
 * Normal output and ordered tool-call evidence produced by the same physical invocation.
 *
 * @param output normal model/application output returned to core execution
 * @param calls ordered tool-call evidence captured during that invocation
 */
public record ToolInvocation(String output, List<ToolCallObservation> calls) {
    public ToolInvocation {
        output = Objects.requireNonNull(output, "output");
        calls = List.copyOf(Objects.requireNonNull(calls, "calls"));
        if (calls.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("calls must not contain null");
        }
    }

    /** Returns a representation that omits output and tool payloads. */
    @Override
    public String toString() {
        return "ToolInvocation[output=<hidden>, callCount=" + calls.size() + "]";
    }
}
