package com.modelmatrix4j.springai;

import java.util.Objects;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.scenario.Scenario;
import org.springframework.ai.chat.model.ChatModel;

/** Adapts a Spring AI ChatModel to the ModelMatrix4J ModelAdapter contract. */
public final class SpringAiModelAdapter implements ModelAdapter {

    private final ChatModel chatModel;

    public SpringAiModelAdapter(ChatModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
    }

    @Override
    public String invoke(Scenario scenario) {
        return chatModel.call(scenario.input());
    }
}