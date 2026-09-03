package com.modelmatrix4j.tool;

import java.util.Objects;

/**
 * Provider-neutral observation of one ordered tool call.
 *
 * @param toolName non-blank logical tool name
 * @param arguments serialized tool arguments; validity is evaluated separately
 * @param result tool execution result, possibly empty when no callback was executed
 */
public record ToolCallObservation(
        String toolName,
        String arguments,
        String result
) {
    /** @throws IllegalArgumentException if {@code toolName} is blank */
    public ToolCallObservation {
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(result, "result");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
    }

    /** Returns a representation that omits arguments and tool result payloads. */
    @Override
    public String toString() {
        return "ToolCallObservation[toolName=" + toolName + "]";
    }
}
