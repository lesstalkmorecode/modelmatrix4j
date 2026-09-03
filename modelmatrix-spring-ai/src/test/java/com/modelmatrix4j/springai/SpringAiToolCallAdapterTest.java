package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.tool.ToolCallObservation;
import com.modelmatrix4j.tool.ToolInvocation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class SpringAiToolCallAdapterTest {

    @Test
    void observesAndExecutesToolCallsInModelOrder() throws Exception {
        FakeTool first = new FakeTool("weather", "sunny");
        FakeTool second = new FakeTool("calendar", "free");
        FakeToolCallingChatModel chatModel = new FakeToolCallingChatModel(List.of(
                new AssistantMessage.ToolCall("1", "function", "weather", "{\"city\":\"Berlin\"}"),
                new AssistantMessage.ToolCall("2", "function", "calendar", "{\"day\":\"Monday\"}")
        ));

        ToolInvocation invocation = new SpringAiToolCallAdapter(chatModel, first, second)
                .invoke(new Scenario("tools", "use tools"));

        assertEquals(List.of("weather", "calendar"), invocation.calls().stream()
                .map(ToolCallObservation::toolName).toList());
        assertEquals(List.of("sunny", "free"), invocation.calls().stream()
                .map(ToolCallObservation::result).toList());
        assertEquals(1, first.calls.get());
        assertEquals(1, second.calls.get());
        assertEquals(1, chatModel.calls);
        assertEquals("use tools", chatModel.lastPrompt.getContents());
        assertEquals(2, ((ToolCallingChatOptions) chatModel.lastPrompt.getOptions()).getToolCallbacks().size());
    }

    @Test
    void unknownToolIsObservedWithoutExecutingKnownTool() throws Exception {
        FakeTool known = new FakeTool("weather", "sunny");
        FakeToolCallingChatModel chatModel = new FakeToolCallingChatModel(List.of(
                new AssistantMessage.ToolCall("1", "function", "unknown", "{}")));

        ToolInvocation invocation = new SpringAiToolCallAdapter(chatModel, known)
                .invoke(new Scenario("tools", "use tools"));

        assertEquals(1, invocation.calls().size());
        assertEquals("unknown", invocation.calls().getFirst().toolName());
        assertEquals("", invocation.calls().getFirst().result());
        assertEquals(0, known.calls.get());
    }

    @Test
    void toolRuntimeFailurePropagatesWithoutRetry() {
        FakeTool broken = new FakeTool("weather", new IllegalStateException("boom"));
        FakeToolCallingChatModel chatModel = new FakeToolCallingChatModel(List.of(
                new AssistantMessage.ToolCall("1", "function", "weather", "{}")));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new SpringAiToolCallAdapter(chatModel, broken)
                        .invoke(new Scenario("tools", "use tools")));

        assertEquals("boom", failure.getMessage());
        assertEquals(1, broken.calls.get());
        assertEquals(1, chatModel.calls);
    }

    @Test
    void rejectsDuplicateToolNamesBeforeCallingModel() {
        FakeToolCallingChatModel chatModel = new FakeToolCallingChatModel(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new SpringAiToolCallAdapter(chatModel,
                        new FakeTool("weather", "one"), new FakeTool("weather", "two")));
        assertEquals(0, chatModel.calls);
    }

    @Test
    void observationToStringDoesNotLeakArgumentsOrResult() {
        ToolCallObservation observation = new ToolCallObservation(
                "weather", "{\"token\":\"secret\"}", "password=hunter2");
        assertFalse(observation.toString().contains("secret"));
        assertFalse(observation.toString().contains("hunter2"));
        assertTrue(observation.toString().contains("weather"));
    }

    private static final class FakeToolCallingChatModel implements ChatModel {
        private final List<AssistantMessage.ToolCall> toolCalls;
        private Prompt lastPrompt;
        private int calls;

        private FakeToolCallingChatModel(List<AssistantMessage.ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls++;
            lastPrompt = prompt;
            AssistantMessage message = AssistantMessage.builder().content("").toolCalls(toolCalls).build();
            return new ChatResponse(List.of(new Generation(message)));
        }
    }

    private static final class FakeTool implements ToolCallback {
        private final ToolDefinition definition;
        private final java.util.function.Function<String, String> body;
        private final AtomicInteger calls = new AtomicInteger();

        private FakeTool(String name, String result) {
            this(name, input -> result);
        }

        private FakeTool(String name, RuntimeException failure) {
            this(name, input -> { throw failure; });
        }

        private FakeTool(String name, java.util.function.Function<String, String> body) {
            this.definition = ToolDefinition.builder().name(name).description("test tool")
                    .inputSchema("{\"type\":\"object\"}").build();
            this.body = body;
        }

        @Override
        public ToolDefinition getToolDefinition() { return definition; }

        @Override
        public String call(String toolInput) {
            calls.incrementAndGet();
            return body.apply(toolInput);
        }
    }
}
