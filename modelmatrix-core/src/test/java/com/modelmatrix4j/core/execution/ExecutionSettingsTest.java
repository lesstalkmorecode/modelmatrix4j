package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExecutionSettingsTest {
    @Test
    void defaultsAreOneRepetitionThirtySecondsAndEightInvocations() {
        ExecutionSettings defaults = ExecutionSettings.defaults();

        assertEquals(1, defaults.repetitions());
        assertEquals(Duration.ofSeconds(30), defaults.timeout());
        assertEquals(8, defaults.maxConcurrentInvocations());
    }

    @Test
    void rejectsInvalidTuning() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionSettings(0, Duration.ofSeconds(1), 1));
        assertThrows(NullPointerException.class,
                () -> new ExecutionSettings(1, null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionSettings(1, Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionSettings(1, Duration.ofSeconds(-1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ExecutionSettings(1, Duration.ofSeconds(1), 0));
    }
}
