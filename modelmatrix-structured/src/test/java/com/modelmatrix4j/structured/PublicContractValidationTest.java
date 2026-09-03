package com.modelmatrix4j.structured;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PublicContractValidationTest {

    @Test
    void structuredObservationRejectsInvalidIdentityRepetitionAndOutput() {
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputObservation(null, "model", 0, "{}"));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputObservation(" ", "model", 0, "{}"));
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputObservation("run", null, 0, "{}"));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputObservation("run", " ", 0, "{}"));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputObservation("run", "model", -1, "{}"));
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputObservation("run", "model", 0, null));
    }

    @Test
    void schemaRequiresValidOrderedFieldDefinitions() {
        assertThrows(NullPointerException.class, () -> new JsonObjectSchema(null));
        assertThrows(NullPointerException.class,
                () -> new JsonObjectSchema(java.util.Collections.singletonMap(
                        null, JsonObjectSchema.ValueType.STRING)));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectSchema(Map.of(" ", JsonObjectSchema.ValueType.STRING)));
        assertThrows(NullPointerException.class,
                () -> new JsonObjectSchema(java.util.Collections.singletonMap("name", null)));

        LinkedHashMap<String, JsonObjectSchema.ValueType> fields = new LinkedHashMap<>();
        fields.put("second", JsonObjectSchema.ValueType.NUMBER);
        fields.put("first", JsonObjectSchema.ValueType.STRING);
        assertEquals(List.of("second", "first"),
                new JsonObjectSchema(fields).requiredFields().keySet().stream().toList());
    }

    @Test
    void schemaValidationResultEnforcesDiagnosticContract() {
        assertThrows(NullPointerException.class, () -> new JsonObjectSchema.Validation(true, null));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectSchema.Validation(true, "unexpected"));
        assertThrows(IllegalArgumentException.class,
                () -> new JsonObjectSchema.Validation(false, " "));
    }

    @Test
    void schemaValidationAndEvaluatorRejectInvalidProgrammingInputs() {
        JsonObjectSchema schema = new JsonObjectSchema(Map.of());
        StructuredOutputEvaluator evaluator = new StructuredOutputEvaluator();
        StructuredOutputObservation output = new StructuredOutputObservation("run", "model", 0, "{}");

        assertThrows(NullPointerException.class, () -> schema.validate(null));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(null, schema));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(List.of(output), null));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(), schema));
    }

    @Test
    void resultObservationEnforcesIdentityAndDiagnosticContract() {
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputResult.Observation(null, "model", 0, true, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult.Observation(" ", "model", 0, true, ""));
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputResult.Observation("run", null, 0, true, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult.Observation("run", " ", 0, true, ""));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult.Observation("run", "model", -1, true, ""));
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputResult.Observation("run", "model", 0, true, null));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult.Observation("run", "model", 0, true, "unexpected"));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult.Observation("run", "model", 0, false, " "));
    }

    @Test
    void structuredResultRequiresStatusNonEmptyObservationsAndMismatchPeers() {
        StructuredOutputResult.Observation valid =
                new StructuredOutputResult.Observation("run", "model", 0, true, "");

        assertThrows(NullPointerException.class,
                () -> new StructuredOutputResult(null, List.of(valid)));
        assertThrows(NullPointerException.class,
                () -> new StructuredOutputResult(StructuredOutputResult.Status.COMPATIBLE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult(StructuredOutputResult.Status.COMPATIBLE, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new StructuredOutputResult(StructuredOutputResult.Status.MISMATCH, List.of(valid)));
    }

    @Test
    void executionPreparationRejectsMissingOrEmptyModels() {
        ModelUnderTest model = new ModelUnderTest(new ModelDescriptor("model"), scenario -> "{}");

        assertThrows(NullPointerException.class, () -> StructuredOutputExecution.prepare(null));
        assertThrows(IllegalArgumentException.class, () -> StructuredOutputExecution.prepare(List.of()));
        assertThrows(NullPointerException.class,
                () -> StructuredOutputExecution.prepare(java.util.Arrays.asList(model, null)));
        assertThrows(NullPointerException.class,
                () -> StructuredOutputExecution.prepare(List.of(model)).observations(null));
    }
}
