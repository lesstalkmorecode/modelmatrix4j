package com.modelmatrix4j.core.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RunResultTest {
    @Test
    void retainsProvidedImmutableValue() {
        RunResult result = new RunResult("run", "scenario", new ModelDescriptor("model"), 0,
                RunStatus.COMPLETED, "already safe", Duration.ZERO, "");
        assertEquals("already safe", result.output());
    }

    @Test
    void enforcesStructuralInvariantsOnly() {
        ModelDescriptor model = new ModelDescriptor("model");
        assertThrows(IllegalArgumentException.class, () -> new RunResult("run", "scenario", model,
                -1, RunStatus.COMPLETED, "", Duration.ZERO, ""));
        assertThrows(IllegalArgumentException.class, () -> new RunResult("run", "scenario", model,
                0, RunStatus.FAILED, "output", Duration.ZERO, ""));
        assertThrows(IllegalArgumentException.class, () -> new RunResult("run", "scenario", model,
                0, RunStatus.COMPLETED, "", Duration.ofNanos(-1), ""));
        assertThrows(NullPointerException.class, () -> new RunResult("run", "scenario", model,
                0, RunStatus.COMPLETED, null, Duration.ZERO, ""));
    }
}
