package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunStatus;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityEvaluatorTest {
    private final CompatibilityEvaluator policy = new CompatibilityEvaluator();

    @Test
    void equivalentNormalizedOutputIsCompatible() {
        assertEquals(CompatibilityStatus.COMPATIBLE,
                policy.evaluate(List.of(
                        new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                new ExecutionOutcome.Completed("hello world", Duration.ZERO)),
                        new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                new ExecutionOutcome.Completed("hello world", Duration.ZERO)))));
    }

    @Test
    void differentUnsanitizedOutputIsAMismatch() {
        assertEquals(CompatibilityStatus.MISMATCH,
                policy.evaluate(List.of(
                        new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                new ExecutionOutcome.Completed("token=abc", Duration.ZERO)),
                        new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                new ExecutionOutcome.Completed("token=xyz", Duration.ZERO)))));
    }

    @Test
    void executionFailureHasPrecedence() {
        List<ExecutionOutcome.Failed> failures = List.of(
                new ExecutionOutcome.Failed(RunStatus.FAILED, "failed", Duration.ZERO),
                new ExecutionOutcome.Failed(RunStatus.TIMED_OUT, "timed out", Duration.ZERO),
                new ExecutionOutcome.Failed(RunStatus.CANCELLED, "cancelled", Duration.ZERO));

        for (ExecutionOutcome.Failed failure : failures) {
            assertEquals(CompatibilityStatus.EXECUTION_FAILURE,
                    policy.evaluate(List.of(
                            new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                    new ExecutionOutcome.Failed(RunStatus.UNAVAILABLE, "unavailable", Duration.ZERO)),
                            new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                    new ExecutionOutcome.Completed("same", Duration.ZERO)),
                            new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0, failure))));
        }
    }

    @Test
    void unavailableIsDistinctFromMismatch() {
        assertEquals(CompatibilityStatus.UNAVAILABLE,
                policy.evaluate(List.of(
                        new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                new ExecutionOutcome.Completed("same", Duration.ZERO)),
                        new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                                new ExecutionOutcome.Failed(RunStatus.UNAVAILABLE, "unavailable", Duration.ZERO)))));
    }
}
