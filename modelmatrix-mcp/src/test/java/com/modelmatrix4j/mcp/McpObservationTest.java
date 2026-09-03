package com.modelmatrix4j.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class McpObservationTest {
    @Test
    void preservesOrderedToolEvidence() {
        var observation = new McpObservation(
                "run-1",
                "baseline",
                1,
                List.of(
                        new McpToolInteraction("weather", "{\"city\":\"Berlin\"}"),
                        new McpToolInteraction("clock", "{\"zone\":\"UTC\"}")));

        assertEquals(List.of("weather", "clock"), observation.tools().stream().map(McpToolInteraction::toolId).toList());
        assertEquals(1, observation.repetition());
    }

    @Test
    void defensivelyCopiesToolEvidence() {
        var tools = new ArrayList<McpToolInteraction>();
        tools.add(new McpToolInteraction("weather", "{}"));

        var observation = new McpObservation("run-1", "baseline", 0, tools);
        tools.clear();

        assertEquals(1, observation.tools().size());
    }

    @Test
    void emptyToolListRepresentsNoMcpInteraction() {
        var observation = new McpObservation("run-1", "baseline", 0, List.of());

        assertEquals(List.of(), observation.tools());
    }

    @Test
    void rejectsInvalidIdentityAndRepetition() {
        assertThrows(IllegalArgumentException.class,
                () -> new McpToolInteraction(" ", "{}"));
        assertThrows(IllegalArgumentException.class,
                () -> new McpObservation("run-1", "baseline", -1, List.of()));
    }

    @Test
    void incidentalRenderingDoesNotExposeArguments() {
        var tool = new McpToolInteraction("weather", "{\"secret\":\"token-123\"}");
        var observation = new McpObservation("run-1", "baseline", 0, List.of(tool));

        assertFalse(tool.toString().contains("token-123"));
        assertFalse(observation.toString().contains("token-123"));
    }
}
