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
        assertEquals(
                CompatibilityStatus.COMPATIBLE,
                policy.evaluate(List.of(
                        completed("a", "hello world"),
                        completed("b", "hello world")
                ))
        );
    }

    @Test
    void differentUnsanitizedOutputIsAMismatch() {
        assertEquals(
                CompatibilityStatus.MISMATCH,
                policy.evaluate(List.of(
                        completed("a", "token=abc"),
                        completed("b", "token=xyz")
                ))
        );
    }

    @Test
    void executionFailureHasPrecedenceOverUnavailableAndMismatch() {
        List<ExecutionOutcome.Failed> failures = List.of(
                new ExecutionOutcome.Failed(
                        RunStatus.FAILED,
                        "failed",
                        Duration.ZERO
                ),
                new ExecutionOutcome.Failed(
                        RunStatus.TIMED_OUT,
                        "timed out",
                        Duration.ZERO
                ),
                new ExecutionOutcome.Failed(
                        RunStatus.CANCELLED,
                        "cancelled",
                        Duration.ZERO
                )
        );

        for (ExecutionOutcome.Failed failure : failures) {
            assertEquals(
                    CompatibilityStatus.EXECUTION_FAILURE,
                    policy.evaluate(List.of(
                            completed("a", "first"),
                            completed("b", "different"),
                            failed(
                                    "c",
                                    RunStatus.UNAVAILABLE,
                                    "unavailable"
                            ),
                            outcome("d", failure)
                    ))
            );
        }
    }

    @Test
    void unavailableHasPrecedenceOverMismatch() {
        assertEquals(
                CompatibilityStatus.UNAVAILABLE,
                policy.evaluate(List.of(
                        completed("a", "first"),
                        completed("b", "different"),
                        failed(
                                "c",
                                RunStatus.UNAVAILABLE,
                                "unavailable"
                        )
                ))
        );
    }

    private static ExecutionOutcome completed(
            String model,
            String output
    ) {
        return new ExecutionOutcome(
                "run-" + model,
                "scenario",
                new ModelDescriptor(model),
                0,
                new ExecutionOutcome.Completed(
                        output,
                        Duration.ZERO
                )
        );
    }

    private static ExecutionOutcome failed(
            String model,
            RunStatus status,
            String diagnostic
    ) {
        return outcome(
                model,
                new ExecutionOutcome.Failed(
                        status,
                        diagnostic,
                        Duration.ZERO
                )
        );
    }

    private static ExecutionOutcome outcome(
            String model,
            ExecutionOutcome.State state
    ) {
        return new ExecutionOutcome(
                "run-" + model,
                "scenario",
                new ModelDescriptor(model),
                0,
                state
        );
    }
}