package com.modelmatrix4j.springai;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.tool.ToolAdapter;
import com.modelmatrix4j.tool.ToolArgumentValidator;
import com.modelmatrix4j.tool.ToolCallObservation;
import com.modelmatrix4j.tool.ToolInvocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

/**
 * Spring AI boundary for one tool-aware physical invocation; core owns timeout, concurrency, and
 * repetition lifecycle. Tool calls are observed from the same model invocation rather than by a
 * second evaluation call.
 */
public final class SpringAiToolCallAdapter implements ToolAdapter {

    private final ChatModel chatModel;
    private final ToolCallback[] toolCallbacks;
    private final Map<String, ToolCallback> callbacksByName;
    private final ToolArgumentValidator argumentValidator = new ToolArgumentValidator();

    /**
     * Callback order is preserved. Tool names must be unique.
     *
     * @throws NullPointerException if the model, callback array, or a callback is {@code null}
     * @throws IllegalArgumentException if callback tool names are duplicated
     */
    public SpringAiToolCallAdapter(ChatModel chatModel, ToolCallback... toolCallbacks) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
        Objects.requireNonNull(toolCallbacks, "toolCallbacks");
        this.toolCallbacks = toolCallbacks.clone();
        this.callbacksByName = callbacksByName(this.toolCallbacks);
    }

    /**
     * Executes one Spring AI model call and returns ordered tool calls from that response. Valid
     * arguments invoke the matching callback once; malformed arguments and calls without a matching
     * callback remain observable with an empty tool result.
     */
    @Override
    public ToolInvocation invoke(Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build();

        ChatResponse response = Objects.requireNonNull(
                chatModel.call(new Prompt(scenario.input(), options)),
                "chatModel response"
        );
        var generation = Objects.requireNonNull(response.getResult(), "chatModel result");
        List<ToolCallObservation> observations = new ArrayList<>();

        for (var toolCall : generation.getOutput().getToolCalls()) {
            String arguments = Objects.requireNonNull(toolCall.arguments(), "tool arguments");
            if (!argumentValidator.isValid(arguments)) {
                observations.add(new ToolCallObservation(toolCall.name(), arguments, ""));
                continue;
            }

            ToolCallback callback = callbacksByName.get(toolCall.name());
            String result = callback == null
                    ? ""
                    : Objects.requireNonNull(callback.call(arguments), "tool result");
            observations.add(new ToolCallObservation(toolCall.name(), arguments, result));
        }

        String output = Objects.requireNonNull(generation.getOutput().getText(), "model output");
        return new ToolInvocation(output, observations);
    }

    private static Map<String, ToolCallback> callbacksByName(ToolCallback[] toolCallbacks) {
        Map<String, ToolCallback> callbacksByName = new LinkedHashMap<>();
        for (ToolCallback callback : toolCallbacks) {
            Objects.requireNonNull(callback, "toolCallback");
            String name = callback.getToolDefinition().name();
            if (callbacksByName.put(name, callback) != null) {
                throw new IllegalArgumentException("duplicate tool name: " + name);
            }
        }
        return Map.copyOf(callbacksByName);
    }
}
