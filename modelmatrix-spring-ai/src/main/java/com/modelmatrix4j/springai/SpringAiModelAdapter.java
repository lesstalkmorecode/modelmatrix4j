package com.modelmatrix4j.springai;

import java.util.Objects;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.scenario.Scenario;
import org.springframework.ai.chat.model.ChatModel;

/** Adapts one Spring AI {@link ChatModel} call to the provider-neutral {@link ModelAdapter} contract. */
public final class SpringAiModelAdapter implements ModelAdapter {

    private final ChatModel chatModel;

    public SpringAiModelAdapter(ChatModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
    }

    /** Delegates one physical invocation using {@link Scenario#input()} as the Spring AI prompt text. */
    @Override
    public String invoke(Scenario scenario) {
        return chatModel.call(scenario.input());
    }
}
