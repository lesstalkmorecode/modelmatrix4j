package com.modelmatrix4j.mcp;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, protocol-free fixture that produces MCP observations from scripted tool behavior. */
final class DeterministicMcpFixture {
    private final List<McpToolInteraction> tools = new ArrayList<>();

    DeterministicMcpFixture tool(String toolId, String argumentsJson) {
        tools.add(new McpToolInteraction(toolId, argumentsJson));
        return this;
    }

    McpObservation observe(String runId, String configurationId, int repetition) {
        return new McpObservation(runId, configurationId, repetition, tools);
    }
}
