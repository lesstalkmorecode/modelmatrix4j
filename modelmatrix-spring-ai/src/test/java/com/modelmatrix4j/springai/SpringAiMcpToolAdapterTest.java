package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.mcp.McpExecution;
import com.modelmatrix4j.mcp.McpInvocation;
import com.modelmatrix4j.mcp.McpModel;
import com.modelmatrix4j.mcp.McpObservation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class SpringAiMcpToolAdapterTest {

    @Test
    void repetitionsUseFreshObserverStateAndDoNotLeakToolEvidence() {
        AtomicInteger modelCalls = new AtomicInteger();
        ChatModel model = toolCallingModel(call -> {
            if (call == 0) {
                return responseWithTool("1", "weather", "{\"run\":0}");
            }
            if (call == 1) {
                return finalResponse("done-0");
            }
            if (call == 2) {
                return responseWithTool("2", "weather", "{\"run\":1}");
            }
            return finalResponse("done-1");
        }, modelCalls);
        SpringAiMcpToolAdapter adapter = new SpringAiMcpToolAdapter(
                ChatClient.create(model),
                callback("weather"));
        var prepared = McpExecution.prepare(List.of(
                new McpModel(new ModelDescriptor("spring-mcp"), adapter),
                new McpModel(new ModelDescriptor("companion"), scenario ->
                        new McpInvocation("stable", List.of()))));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .repetitions(2)
                .build()
                .run(new Scenario("mcp", "use weather"));
        List<McpObservation> observations = prepared.observations(core).stream()
                .filter(observation -> observation.configurationId().equals("spring-mcp"))
                .toList();

        assertEquals(2, observations.size());
        assertEquals(1, observations.get(0).tools().size());
        assertEquals(1, observations.get(1).tools().size());
        assertEquals("{\"run\":0}", observations.get(0).tools().getFirst().argumentsJson());
        assertEquals("{\"run\":1}", observations.get(1).tools().getFirst().argumentsJson());
        assertEquals(List.of("done-0", "done-1"), core.runs().stream()
                .filter(run -> run.model().configurationId().equals("spring-mcp"))
                .map(run -> run.output())
                .toList());
    }

    @Test
    void oneInvocationProducesOutputAndToolEvidenceFromTheSameChatClientExecution() throws Exception {
        AtomicInteger modelCalls = new AtomicInteger();
        ChatModel model = toolCallingModel(call -> call == 0
                ? responseWithTool("1", "weather", "{\"city\":\"Berlin\"}")
                : finalResponse("sunny"), modelCalls);
        SpringAiMcpToolAdapter adapter = new SpringAiMcpToolAdapter(
                ChatClient.create(model),
                callback("weather"));

        McpInvocation invocation = adapter.invoke(new Scenario("mcp", "weather"));

        assertEquals("sunny", invocation.output());
        assertEquals(1, invocation.tools().size());
        assertEquals("weather", invocation.tools().getFirst().toolId());
        assertEquals("{\"city\":\"Berlin\"}", invocation.tools().getFirst().argumentsJson());
        assertEquals(2, modelCalls.get());
    }

    private static ChatModel toolCallingModel(IntFunction<ChatResponse> response, AtomicInteger calls) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return response.apply(calls.getAndIncrement());
            }

            @Override
            public ChatOptions getOptions() {
                return DefaultToolCallingChatOptions.builder().build();
            }
        };
    }

    private static ToolCallback callback(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name(name)
                        .description("test MCP tool")
                        .inputSchema("{\"type\":\"object\"}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return "tool-result";
            }
        };
    }

    private static ChatResponse responseWithTool(String id, String name, String arguments) {
        AssistantMessage message = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private static ChatResponse finalResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
