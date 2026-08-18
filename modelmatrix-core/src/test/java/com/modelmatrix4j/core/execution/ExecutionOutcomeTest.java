package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import com.modelmatrix4j.core.result.RunStatus;
import org.junit.jupiter.api.Test;

class ExecutionOutcomeTest {
    @Test
    void completedRejectsNullsAndNegativeDurations() {
        assertThrows(NullPointerException.class,
                () -> new ExecutionOutcome.Completed(null, Duration.ZERO));
        assertThrows(NullPointerException.class,
                () -> new ExecutionOutcome.Completed("ok", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionOutcome.Completed(
                        "ok", Duration.ofNanos(-1)));
    }

    @Test
    void failedRejectsNullsNegativeDurationsAndCompletedStatus() {
        assertThrows(NullPointerException.class,
                () -> new ExecutionOutcome.Failed(null, "diagnostic", Duration.ZERO));
        assertThrows(NullPointerException.class,
                () -> new ExecutionOutcome.Failed(RunStatus.FAILED, null, Duration.ZERO));
        assertThrows(NullPointerException.class,
                () -> new ExecutionOutcome.Failed(RunStatus.FAILED, "diagnostic", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionOutcome.Failed(
                        RunStatus.FAILED, "diagnostic", Duration.ofNanos(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionOutcome.Failed(
                        RunStatus.COMPLETED, "impossible", Duration.ZERO));
    }
}
