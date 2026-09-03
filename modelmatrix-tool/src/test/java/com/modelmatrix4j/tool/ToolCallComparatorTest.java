package com.modelmatrix4j.tool;

import static com.modelmatrix4j.tool.ToolCallComparison.Status.COMPATIBLE;
import static com.modelmatrix4j.tool.ToolCallComparison.Status.INVALID_ARGUMENTS;
import static com.modelmatrix4j.tool.ToolCallComparison.Status.MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class ToolCallComparatorTest {

    private final ToolCallComparator comparator = new ToolCallComparator();

    @Test
    void comparesToolArgumentsByStructuredValue() {
        ToolCallComparison comparison = comparator.compare(
                List.of(call("weather", "{\"city\":\"Berlin\",\"days\":1}")),
                List.of(call("weather", "{\"days\":1.0,\"city\":\"Berlin\"}"))
        );

        assertEquals(COMPATIBLE, comparison.status());
    }

    @Test
    void wrongToolIsMismatch() {
        assertEquals(
                MISMATCH,
                comparator.compare(
                        List.of(call("weather", "{}")),
                        List.of(call("calendar", "{}"))
                ).status()
        );
    }

    @Test
    void missingOrAdditionalToolCallIsMismatch() {
        assertEquals(
                MISMATCH,
                comparator.compare(
                        List.of(call("weather", "{}")),
                        List.of()
                ).status()
        );

        assertEquals(
                MISMATCH,
                comparator.compare(
                        List.of(call("weather", "{}")),
                        List.of(call("weather", "{}"), call("calendar", "{}"))
                ).status()
        );
    }

    @Test
    void orderedCallsAreSignificant() {
        assertEquals(
                MISMATCH,
                comparator.compare(
                        List.of(call("weather", "{}"), call("calendar", "{}")),
                        List.of(call("calendar", "{}"), call("weather", "{}"))
                ).status()
        );
    }

    @Test
    void malformedArgumentsAreInvalidNotMismatch() {
        assertEquals(
                INVALID_ARGUMENTS,
                comparator.compare(
                        List.of(call("weather", "{broken")),
                        List.of(call("weather", "{}"))
                ).status()
        );
    }

    @Test
    void diagnosticsNeverEchoToolArguments() {
        ToolCallComparison comparison = comparator.compare(
                List.of(call("weather", "{\"token\":\"secret-one\"}")),
                List.of(call("weather", "{\"token\":\"secret-two\"}"))
        );

        assertEquals(MISMATCH, comparison.status());
        assertFalse(comparison.diagnostic().contains("secret-one"));
        assertFalse(comparison.diagnostic().contains("secret-two"));
    }

    private static ToolCallObservation call(String name, String arguments) {
        return new ToolCallObservation(name, arguments, "");
    }
}
