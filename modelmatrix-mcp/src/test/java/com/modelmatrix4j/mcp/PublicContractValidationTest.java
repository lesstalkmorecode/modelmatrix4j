package com.modelmatrix4j.mcp;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicContractValidationTest {

    @Test
    void toolInteractionRejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new McpToolInteraction(null, "{}"));
        assertThrows(NullPointerException.class, () -> new McpToolInteraction("tool", null));
        assertThrows(IllegalArgumentException.class, () -> new McpToolInteraction("tool", " "));
    }

    @Test
    void invocationRequiresOutputAndOrderedToolEvidence() {
        assertThrows(NullPointerException.class, () -> new McpInvocation(null, List.of()));
        assertThrows(NullPointerException.class, () -> new McpInvocation("output", null));
        assertThrows(NullPointerException.class,
                () -> new McpInvocation("output", java.util.Arrays.asList((McpToolInteraction) null)));
    }

    @Test
    void modelAndObservationRejectMissingComponents() {
        assertThrows(NullPointerException.class,
                () -> new McpModel(null, scenario -> new McpInvocation("output", List.of())));
        assertThrows(NullPointerException.class,
                () -> new McpModel(new ModelDescriptor("model"), null));
        assertThrows(NullPointerException.class,
                () -> new McpObservation(null, "model", 0, List.of()));
        assertThrows(NullPointerException.class,
                () -> new McpObservation("run", null, 0, List.of()));
        assertThrows(NullPointerException.class,
                () -> new McpObservation("run", "model", 0, null));
    }

    @Test
    void resultRequiresStatusAndObservationList() {
        McpResult.ObservationSummary summary = new McpResult.ObservationSummary("run", "model", 0, 0);

        assertThrows(NullPointerException.class, () -> new McpResult(null, List.of(summary)));
        assertThrows(NullPointerException.class,
                () -> new McpResult(McpResult.Status.COMPATIBLE, null));
    }

    @Test
    void publicMcpOperationsRejectInvalidProgrammingInputs() {
        McpComparator comparator = new McpComparator();
        McpEvaluator evaluator = new McpEvaluator();
        McpObservation observation = new McpObservation("run", "model", 0, List.of());

        assertThrows(NullPointerException.class, () -> comparator.compare(null, observation));
        assertThrows(NullPointerException.class, () -> comparator.compare(observation, null));
        assertThrows(NullPointerException.class, () -> evaluator.evaluate(null));
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of()));
    }

    @Test
    void executionPreparationRejectsMissingOrEmptyModelsAndResult() {
        McpModel model = new McpModel(
                new ModelDescriptor("model"), scenario -> new McpInvocation("output", List.of()));

        assertThrows(NullPointerException.class, () -> McpExecution.prepare(null));
        assertThrows(IllegalArgumentException.class, () -> McpExecution.prepare(List.of()));
        assertThrows(NullPointerException.class,
                () -> McpExecution.prepare(java.util.Arrays.asList(model, null)));
        assertThrows(NullPointerException.class,
                () -> McpExecution.prepare(List.of(model)).observations(null));
    }
}
