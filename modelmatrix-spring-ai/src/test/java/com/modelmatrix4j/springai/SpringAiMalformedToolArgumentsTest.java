package com.modelmatrix4j.springai;

import static com.modelmatrix4j.tool.ToolCallComparison.Status.INVALID_ARGUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.tool.ToolCallComparator;
import com.modelmatrix4j.tool.ToolCallObservation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class SpringAiMalformedToolArgumentsTest {

    @Test
    void malformedArgumentsAreObservedAsInvalidWithoutCallingCallback() throws Exception {
        AtomicInteger callbackCalls = new AtomicInteger();
        ToolCallback tool = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("weather").description("test tool")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String input) {
                callbackCalls.incrementAndGet();
                return "must-not-run";
            }
        };
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                AssistantMessage message = AssistantMessage.builder().content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "1", "function", "weather", "{broken"))).build();
                return new ChatResponse(List.of(new Generation(message)));
            }
        };

        List<ToolCallObservation> actual = new SpringAiToolCallAdapter(model, tool)
                .invoke(new Scenario("tools", "use tool")).calls();
        var comparison = new ToolCallComparator().compare(
                List.of(new ToolCallObservation("weather", "{}", "")), actual);

        assertEquals(0, callbackCalls.get());
        assertEquals(1, actual.size());
        assertEquals("", actual.getFirst().result());
        assertEquals(INVALID_ARGUMENTS, comparison.status());
    }
}
