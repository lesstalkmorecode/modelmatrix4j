package com.modelmatrix4j.tool;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicContractValidationTest {

    @Test
    void toolCallObservationRequiresNameArgumentsAndResult() {
        assertThrows(NullPointerException.class,
                () -> new ToolCallObservation(null, "{}", "result"));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolCallObservation(" ", "{}", "result"));
        assertThrows(NullPointerException.class,
                () -> new ToolCallObservation("tool", null, "result"));
        assertThrows(NullPointerException.class,
                () -> new ToolCallObservation("tool", "{}", null));
    }

    @Test
    void toolModelAndInvocationRequireTheirDocumentedComponents() {
        assertThrows(NullPointerException.class,
                () -> new ToolModel(null, scenario -> new ToolInvocation("", List.of())));
        assertThrows(NullPointerException.class,
                () -> new ToolModel(new ModelDescriptor("model"), null));
        assertThrows(NullPointerException.class, () -> new ToolInvocation(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ToolInvocation("output", null));
        assertThrows(NullPointerException.class,
                () -> new ToolInvocation("output", java.util.Arrays.asList((ToolCallObservation) null)));
    }

    @Test
    void toolObservationRequiresValidIdentityRepetitionAndCalls() {
        assertThrows(NullPointerException.class,
                () -> new ToolObservation(null, "model", 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolObservation(" ", "model", 0, List.of()));
        assertThrows(NullPointerException.class,
                () -> new ToolObservation("run", null, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolObservation("run", " ", 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolObservation("run", "model", -1, List.of()));
        assertThrows(NullPointerException.class,
                () -> new ToolObservation("run", "model", 0, null));
    }

    @Test
    void comparisonResultEnforcesDiagnosticContract() {
        assertThrows(NullPointerException.class, () -> new ToolCallComparison(null, ""));
        assertThrows(NullPointerException.class,
                () -> new ToolCallComparison(ToolCallComparison.Status.COMPATIBLE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolCallComparison(ToolCallComparison.Status.COMPATIBLE, "unexpected"));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolCallComparison(ToolCallComparison.Status.MISMATCH, " "));
    }

    @Test
    void publicToolOperationsRejectNullProgrammingInputs() {
        ToolArgumentValidator validator = new ToolArgumentValidator();
        ToolCallComparator comparator = new ToolCallComparator();

        assertThrows(NullPointerException.class, () -> validator.isValid(null));
        assertThrows(NullPointerException.class, () -> comparator.compare(null, List.of()));
        assertThrows(NullPointerException.class, () -> comparator.compare(List.of(), null));
    }

    @Test
    void executionPreparationRejectsMissingOrEmptyModelsAndResult() {
        ToolModel model = new ToolModel(
                new ModelDescriptor("model"), scenario -> new ToolInvocation("output", List.of()));

        assertThrows(NullPointerException.class, () -> ToolExecution.prepare(null));
        assertThrows(IllegalArgumentException.class, () -> ToolExecution.prepare(List.of()));
        assertThrows(NullPointerException.class,
                () -> ToolExecution.prepare(java.util.Arrays.asList(model, null)));
        assertThrows(NullPointerException.class,
                () -> ToolExecution.prepare(List.of(model)).observations(null));
    }
}
