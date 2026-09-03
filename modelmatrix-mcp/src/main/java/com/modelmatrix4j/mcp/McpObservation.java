package com.modelmatrix4j.mcp;

import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral ordered MCP tool evidence for one completed core run. Incidental rendering
 * exposes tool count but not argument payloads.
 */
public record McpObservation(
        String runId,
        String configurationId,
        int repetition,
        List<McpToolInteraction> tools
) {
    /** Tool evidence is defensively copied; identifiers must be non-blank and repetition non-negative. */
    public McpObservation {
        runId = requireText(runId, "runId");
        configurationId = requireText(configurationId, "configurationId");
        if (repetition < 0) {
            throw new IllegalArgumentException("repetition must be >= 0");
        }
        tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
        if (tools.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("tools must not contain null");
        }
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
        return "McpObservation[runId=" + runId
                + ", configurationId=" + configurationId
                + ", repetition=" + repetition
                + ", toolCount=" + tools.size() + "]";
    }
}
