package com.modelmatrix4j.mcp;

import java.util.Objects;

/**
 * One ordered, provider-neutral MCP tool interaction. {@code argumentsJson} participates in
 * semantic comparison but is hidden from incidental rendering. Both components must be non-blank.
 */
public record McpToolInteraction(String toolId, String argumentsJson) {
    public McpToolInteraction {
        toolId = requireText(toolId, "toolId");
        argumentsJson = requireText(argumentsJson, "argumentsJson");
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
        return "McpToolInteraction[toolId=" + toolId + ", arguments=<hidden>]";
    }
}
