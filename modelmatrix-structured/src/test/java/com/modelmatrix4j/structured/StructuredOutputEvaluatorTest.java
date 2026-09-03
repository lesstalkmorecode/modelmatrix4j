package com.modelmatrix4j.structured;

import static com.modelmatrix4j.structured.JsonObjectSchema.ValueType.NUMBER;
import static com.modelmatrix4j.structured.JsonObjectSchema.ValueType.STRING;
import static com.modelmatrix4j.structured.StructuredOutputResult.Status.COMPATIBLE;
import static com.modelmatrix4j.structured.StructuredOutputResult.Status.INVALID;
import static com.modelmatrix4j.structured.StructuredOutputResult.Status.MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredOutputEvaluatorTest {

    private final StructuredOutputEvaluator evaluator = new StructuredOutputEvaluator();
    private final JsonObjectSchema schema = new JsonObjectSchema(Map.of("name", STRING, "age", NUMBER));

    @Test
    void equivalentValidValuesAreCompatibleAndKeepRunIdentity() {
        StructuredOutputResult result = evaluator.evaluate(List.of(
                output("run-0", "first", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("run-1", "second", 0, "{\"age\":37.0,\"name\":\"Ada\"}")
        ), schema);

        assertEquals(COMPATIBLE, result.status());
        assertEquals(List.of("run-0", "run-1"), result.observations().stream()
                .map(StructuredOutputResult.Observation::runId).toList());
        assertTrue(result.observations().stream().allMatch(StructuredOutputResult.Observation::valid));
    }

    @Test
    void comparesConfigurationsWithinEachRepetition() {
        StructuredOutputResult result = evaluator.evaluate(List.of(
                output("a-0", "first", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("b-0", "second", 0, "{\"age\":37.0,\"name\":\"Ada\"}"),
                output("a-1", "first", 1, "{\"name\":\"Grace\",\"age\":45}"),
                output("b-1", "second", 1, "{\"age\":45.0,\"name\":\"Grace\"}")
        ), schema);

        assertEquals(COMPATIBLE, result.status());
        assertEquals(List.of(0, 0, 1, 1), result.observations().stream()
                .map(StructuredOutputResult.Observation::repetition).toList());
    }

    @Test
    void mismatchIsScopedToOneRepetition() {
        StructuredOutputResult result = evaluator.evaluate(List.of(
                output("a-0", "first", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("b-0", "second", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("a-1", "first", 1, "{\"name\":\"Grace\",\"age\":45}"),
                output("b-1", "second", 1, "{\"name\":\"Linus\",\"age\":45}")
        ), schema);

        assertEquals(MISMATCH, result.status());
    }

    @Test
    void rejectsPartialOrDuplicateRepetitionMatrices() {
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(
                output("a-0", "first", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("b-0", "second", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("a-1", "first", 1, "{\"name\":\"Grace\",\"age\":45}")
        ), schema));

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(
                output("a-0", "first", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("a-0-copy", "first", 0, "{\"name\":\"Ada\",\"age\":37}")
        ), schema));
    }

    @Test
    void observationRenderingHidesRawPayload() {
        StructuredOutputObservation observation = output(
                "run", "first", 0, "{\"token\":\"secret-value\"}");

        assertFalse(observation.toString().contains("secret-value"));
        assertTrue(observation.toString().contains("output=<hidden>"));
    }

    @Test
    void differentValidValuesAreMismatch() {
        StructuredOutputResult result = evaluator.evaluate(List.of(
                output("run-0", "first", 0, "{\"name\":\"Ada\",\"age\":37}"),
                output("run-1", "second", 0, "{\"name\":\"Grace\",\"age\":37}")
        ), schema);
        assertEquals(MISMATCH, result.status());
    }

    @Test
    void sameMalformedOutputIsInvalidNotMismatch() {
        StructuredOutputResult result = evaluator.evaluate(List.of(
                output("run-0", "first", 0, "{broken"),
                output("run-1", "second", 0, "{broken")
        ), schema);
        assertEquals(INVALID, result.status());
        assertTrue(result.observations().stream().noneMatch(StructuredOutputResult.Observation::valid));
    }

    @Test
    void wrongFieldTypeIsInvalidWithoutEchoingValue() {
        StructuredOutputResult result = evaluator.evaluate(List.of(
                output("run-0", "first", 0, "{\"name\":\"Ada\",\"age\":\"token=secret-value\"}")
        ), schema);
        assertEquals(INVALID, result.status());
        String diagnostic = result.observations().getFirst().diagnostic();
        assertEquals("field age must be number", diagnostic);
        assertFalse(diagnostic.contains("secret-value"));
    }

    @Test
    void resultRejectsStatusObservationContradictions() {
        assertThrows(IllegalArgumentException.class, () -> new StructuredOutputResult(
                COMPATIBLE,
                List.of(new StructuredOutputResult.Observation("run", "first", 0, false, "invalid"))
        ));
        assertThrows(IllegalArgumentException.class, () -> new StructuredOutputResult(
                INVALID,
                List.of(new StructuredOutputResult.Observation("run", "first", 0, true, ""))
        ));
    }

    private static StructuredOutputObservation output(
            String runId, String configurationId, int repetition, String json) {
        return new StructuredOutputObservation(runId, configurationId, repetition, json);
    }
}
