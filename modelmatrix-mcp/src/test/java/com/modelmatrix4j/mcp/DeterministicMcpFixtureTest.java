package com.modelmatrix4j.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicMcpFixtureTest {
    @Test
    void equivalentScriptedBehaviorIsCompatible() {
        var baseline = new DeterministicMcpFixture()
                .tool("weather", "{\"city\":\"Berlin\",\"days\":1}")
                .observe("a-0", "a", 0);
        var candidate = new DeterministicMcpFixture()
                .tool("weather", "{\"days\":1.0,\"city\":\"Berlin\"}")
                .observe("b-0", "b", 0);

        assertEquals(McpResult.Status.COMPATIBLE, new McpEvaluator().evaluate(List.of(baseline, candidate)).status());
    }

    @Test
    void missingAdditionalAndOrderedBehaviorProduceMismatch() {
        var baseline = new DeterministicMcpFixture()
                .tool("first", "{}")
                .tool("second", "{}")
                .observe("a-0", "a", 0);
        var missing = new DeterministicMcpFixture()
                .tool("first", "{}")
                .observe("b-0", "b", 0);
        var reordered = new DeterministicMcpFixture()
                .tool("second", "{}")
                .tool("first", "{}")
                .observe("b-0", "b", 0);
        var additional = new DeterministicMcpFixture()
                .tool("first", "{}")
                .tool("second", "{}")
                .tool("third", "{}")
                .observe("b-0", "b", 0);

        McpEvaluator evaluator = new McpEvaluator();
        assertEquals(McpResult.Status.MISMATCH, evaluator.evaluate(List.of(baseline, missing)).status());
        assertEquals(McpResult.Status.MISMATCH, evaluator.evaluate(List.of(baseline, reordered)).status());
        assertEquals(McpResult.Status.MISMATCH, evaluator.evaluate(List.of(baseline, additional)).status());
    }

    @Test
    void malformedArgumentsProducedByFixtureAreInvalidEvidence() {
        var baseline = new DeterministicMcpFixture()
                .tool("weather", "{broken")
                .observe("a-0", "a", 0);
        var candidate = new DeterministicMcpFixture()
                .tool("weather", "{}")
                .observe("b-0", "b", 0);

        assertEquals(McpResult.Status.INVALID, new McpEvaluator().evaluate(List.of(baseline, candidate)).status());
    }

    @Test
    void noInteractionIsValidAndComparable() {
        var baseline = new DeterministicMcpFixture().observe("a-0", "a", 0);
        var candidate = new DeterministicMcpFixture().observe("b-0", "b", 0);

        assertEquals(McpResult.Status.COMPATIBLE, new McpEvaluator().evaluate(List.of(baseline, candidate)).status());
    }
}
