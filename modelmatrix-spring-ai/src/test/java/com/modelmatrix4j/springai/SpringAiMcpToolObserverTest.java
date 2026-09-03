package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class SpringAiMcpToolObserverTest {

    @Test
    void recordsActualCallbackInvocationOrderAndArgumentsWithoutChangingDelegation() {
        SpringAiMcpToolObserver observer = new SpringAiMcpToolObserver();
        AtomicInteger calls = new AtomicInteger();
        ToolCallback weather = callback("weather", calls, "sunny");
        ToolCallback clock = callback("clock", calls, "12:00");
        ToolCallback[] observed = observer.observe(List.of(weather, clock));

        assertEquals("sunny", observed[0].call("{\"city\":\"Berlin\"}"));
        assertEquals("12:00", observed[1].call("{\"zone\":\"UTC\"}"));

        assertEquals(2, calls.get());
        assertEquals("weather", observer.interactions().get(0).toolId());
        assertEquals("{\"city\":\"Berlin\"}", observer.interactions().get(0).argumentsJson());
        assertEquals("clock", observer.interactions().get(1).toolId());
    }

    @Test
    void malformedArgumentsAreCapturedForLaterInvalidClassification() {
        SpringAiMcpToolObserver observer = new SpringAiMcpToolObserver();
        ToolCallback observed = observer.observe(List.of(callback("weather", new AtomicInteger(), "unused")))[0];

        observed.call("{broken");

        assertEquals("{broken", observer.interactions().getFirst().argumentsJson());
    }

    @Test
    void delegateFailureIsNotSwallowed() {
        SpringAiMcpToolObserver observer = new SpringAiMcpToolObserver();
        ToolCallback failing = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("weather").description("weather").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("MCP transport failed");
            }
        };
        ToolCallback observed = observer.observe(List.of(failing))[0];

        assertThrows(IllegalStateException.class, () -> observed.call("{}"));
    }

    private static ToolCallback callback(String name, AtomicInteger calls, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name).inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return result;
            }
        };
    }
}
