package com.modelmatrix4j.springai;

import static com.modelmatrix4j.structured.JsonObjectSchema.ValueType.NUMBER;
import static com.modelmatrix4j.structured.JsonObjectSchema.ValueType.STRING;
import static com.modelmatrix4j.structured.StructuredOutputResult.Status.COMPATIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.structured.JsonObjectSchema;
import com.modelmatrix4j.structured.StructuredOutputEvaluator;
import com.modelmatrix4j.structured.StructuredOutputExecution;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class SpringAiStructuredOutputTest {

    @Test
    void structuredSpringAiInvocationsUseCoreExecutionLifecycle() {
        Scenario scenario = new Scenario("structured", "Return customer JSON");
        var prepared = StructuredOutputExecution.prepare(List.of(
                model("first", "{\"name\":\"Ada\",\"age\":37}"),
                model("second", "{\"age\":37.0,\"name\":\"Ada\"}")
        ));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .repetitions(2)
                .timeout(Duration.ofSeconds(1))
                .maxConcurrentInvocations(2)
                .build()
                .run(scenario);
        var result = new StructuredOutputEvaluator().evaluate(
                prepared.observations(core),
                new JsonObjectSchema(Map.of("name", STRING, "age", NUMBER))
        );

        assertEquals(4, core.runs().size());
        assertEquals(4, result.observations().size());
        assertEquals(COMPATIBLE, result.status());
    }

    private static ModelUnderTest model(String configurationId, String response) {
        return new ModelUnderTest(
                new ModelDescriptor(configurationId),
                new SpringAiModelAdapter(new FakeChatModel(response))
        );
    }

    private static final class FakeChatModel implements ChatModel {
        private final String response;

        private FakeChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        }
    }
}
