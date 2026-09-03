package com.modelmatrix4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicContractValidationTest {

    @Test
    void scenarioAndModelConfigurationRejectMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new Scenario(null, "input"));
        assertThrows(NullPointerException.class, () -> new Scenario("scenario", null));
        assertThrows(NullPointerException.class, () -> new ModelDescriptor(null));
        assertThrows(NullPointerException.class,
                () -> new ModelUnderTest(null, scenario -> "output"));
        assertThrows(NullPointerException.class,
                () -> new ModelUnderTest(new ModelDescriptor("model"), null));
    }

    @Test
    void compatibilityResultRequiresStatusAndAtLeastOneRun() {
        RunResult run = completedRun();

        assertThrows(NullPointerException.class,
                () -> new CompatibilityResult(null, List.of(run)));
        assertThrows(NullPointerException.class,
                () -> new CompatibilityResult(CompatibilityStatus.COMPATIBLE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompatibilityResult(CompatibilityStatus.COMPATIBLE, List.of()));
    }

    @Test
    void runResultRequiresEveryDocumentedStructuralComponent() {
        ModelDescriptor model = new ModelDescriptor("model");

        assertThrows(NullPointerException.class, () -> new RunResult(
                null, "scenario", model, 0, RunStatus.COMPLETED, "", Duration.ZERO, ""));
        assertThrows(IllegalArgumentException.class, () -> new RunResult(
                " ", "scenario", model, 0, RunStatus.COMPLETED, "", Duration.ZERO, ""));
        assertThrows(NullPointerException.class, () -> new RunResult(
                "run", null, model, 0, RunStatus.COMPLETED, "", Duration.ZERO, ""));
        assertThrows(IllegalArgumentException.class, () -> new RunResult(
                "run", " ", model, 0, RunStatus.COMPLETED, "", Duration.ZERO, ""));
        assertThrows(NullPointerException.class, () -> new RunResult(
                "run", "scenario", null, 0, RunStatus.COMPLETED, "", Duration.ZERO, ""));
        assertThrows(NullPointerException.class, () -> new RunResult(
                "run", "scenario", model, 0, null, "", Duration.ZERO, ""));
        assertThrows(NullPointerException.class, () -> new RunResult(
                "run", "scenario", model, 0, RunStatus.COMPLETED, "", null, ""));
        assertThrows(NullPointerException.class, () -> new RunResult(
                "run", "scenario", model, 0, RunStatus.COMPLETED, "", Duration.ZERO, null));
    }

    @Test
    void modelUnavailableExceptionPreservesItsDiagnosticMessage() {
        ModelUnavailableException exception = new ModelUnavailableException("provider offline");

        assertEquals("provider offline", exception.getMessage());
    }

    private static RunResult completedRun() {
        return new RunResult(
                "run", "scenario", new ModelDescriptor("model"), 0,
                RunStatus.COMPLETED, "output", Duration.ZERO, "");
    }
}
