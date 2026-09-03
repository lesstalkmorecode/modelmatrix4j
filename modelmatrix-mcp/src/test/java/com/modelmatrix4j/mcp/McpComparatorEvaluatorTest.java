package com.modelmatrix4j.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class McpComparatorEvaluatorTest {
    private final McpComparator comparator = new McpComparator();

    @Test
    void canonicalJsonArgumentsAreCompatible() {
        var left = observation("a", 0,
                List.of(new McpToolInteraction("weather", "{\"city\":\"Berlin\",\"days\":2}")));
        var right = observation("b", 0,
                List.of(new McpToolInteraction("weather", "{ \"days\" : 2.0, \"city\" : \"Berlin\" }")));

        assertEquals(McpResult.Status.COMPATIBLE, comparator.compare(left, right));
    }

    @Test
    void malformedToolArgumentsAreInvalid() {
        var left = observation("a", 0, List.of(new McpToolInteraction("weather", "{not-json}")));
        var right = observation("b", 0, List.of(new McpToolInteraction("weather", "{}")));

        assertEquals(McpResult.Status.INVALID, comparator.compare(left, right));
    }

    @Test
    void invalidEvidenceTakesPrecedenceOverStructuralMismatch() {
        var malformedWithExtraTool = observation("a", 0,
                List.of(
                        new McpToolInteraction("weather", "{not-json}"),
                        new McpToolInteraction("search", "{}")));
        var fewerValidTools = observation("b", 0,
                List.of(new McpToolInteraction("weather", "{}")));

        assertEquals(McpResult.Status.INVALID, comparator.compare(malformedWithExtraTool, fewerValidTools));
    }

    @Test
    void missingAdditionalAndOrderDifferencesAreMismatch() {
        var baseline = observation("a", 0,
                List.of(new McpToolInteraction("first", "{}"), new McpToolInteraction("second", "{}")));
        var reordered = observation("b", 0,
                List.of(new McpToolInteraction("second", "{}"), new McpToolInteraction("first", "{}")));
        var additional = observation("b", 0,
                List.of(
                        new McpToolInteraction("first", "{}"),
                        new McpToolInteraction("second", "{}"),
                        new McpToolInteraction("third", "{}")));

        assertEquals(McpResult.Status.MISMATCH, comparator.compare(baseline, reordered));
        assertEquals(McpResult.Status.MISMATCH, comparator.compare(baseline, additional));
    }

    @Test
    void evaluatorComparesSameRepetitionAcrossConfigurations() {
        var result = new McpEvaluator().evaluate(List.of(
                observation("a", 0, List.of(new McpToolInteraction("tool", "{\"value\":1}"))),
                observation("b", 0, List.of(new McpToolInteraction("tool", "{\"value\":1}"))),
                observation("a", 1, List.of(new McpToolInteraction("tool", "{\"value\":2}"))),
                observation("b", 1, List.of(new McpToolInteraction("tool", "{\"value\":2}")))));

        assertEquals(McpResult.Status.COMPATIBLE, result.status());
    }

    @Test
    void invalidEvidenceTakesPrecedenceOverMismatch() {
        var result = new McpEvaluator().evaluate(List.of(
                observation("a", 0, List.of(new McpToolInteraction("one", "{}"))),
                observation("b", 0, List.of(new McpToolInteraction("two", "{}"))),
                observation("a", 1, List.of(new McpToolInteraction("tool", "{bad}"))),
                observation("b", 1, List.of(new McpToolInteraction("tool", "{}")))));

        assertEquals(McpResult.Status.INVALID, result.status());
    }

    @Test
    void rejectsSingleConfigurationDuplicateAndPartialMatrices() {
        McpEvaluator evaluator = new McpEvaluator();
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(List.of(observation("a", 0, List.of()))));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(List.of(
                        observation("a", 0, List.of()),
                        observation("a", 0, List.of()))));
        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(List.of(
                        observation("a", 0, List.of()),
                        observation("b", 0, List.of()),
                        observation("a", 1, List.of()))));
    }

    private static McpObservation observation(
            String configurationId,
            int repetition,
            List<McpToolInteraction> tools) {
        return new McpObservation(configurationId + "-" + repetition, configurationId, repetition, tools);
    }
}
