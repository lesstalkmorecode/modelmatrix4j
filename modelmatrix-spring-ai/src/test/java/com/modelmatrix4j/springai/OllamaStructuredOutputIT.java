package com.modelmatrix4j.springai;

import static com.modelmatrix4j.structured.JsonObjectSchema.ValueType.NUMBER;
import static com.modelmatrix4j.structured.JsonObjectSchema.ValueType.STRING;
import static com.modelmatrix4j.structured.StructuredOutputResult.Status.COMPATIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.structured.JsonObjectSchema;
import com.modelmatrix4j.structured.StructuredOutputEvaluator;
import com.modelmatrix4j.structured.StructuredOutputExecution;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

/** Opt-in M4 structured-output lifecycle test against two local Ollama models. */
class OllamaStructuredOutputIT {

    private static final String OLLAMA_BASE_URL = System.getProperty("ollama.baseUrl", "http://localhost:11434");
    private static final String MODEL_1 = System.getProperty("ollama.model1", "mistral");
    private static final String MODEL_2 = System.getProperty("ollama.model2", "llama2");
    private static final String OUTPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "name": {"type": "string"},
                "age": {"type": "number"}
              },
              "required": ["name", "age"],
              "additionalProperties": false
            }
            """;

    private static OllamaApi ollamaApi;
    private static List<String> availableModels = List.of();
    private static String availabilityFailure;

    @BeforeAll
    static void inspectPrerequisites() {
        try {
            ollamaApi = OllamaApi.builder().baseUrl(OLLAMA_BASE_URL).build();
            availableModels = ollamaApi.listModels().models().stream().map(OllamaApi.Model::name).toList();
        } catch (Exception exception) {
            availabilityFailure = "Ollama is not available at " + OLLAMA_BASE_URL;
        }
    }

    @Test
    void validatesStructuredBehaviorThroughCoreLifecycle() {
        requireModel(MODEL_1);
        requireModel(MODEL_2);

        Scenario scenario = new Scenario(
                "structured-customer",
                "Return an object with name Ada and age 37."
        );
        var prepared = StructuredOutputExecution.prepare(List.of(
                model(MODEL_1),
                model(MODEL_2)
        ));
        var core = ModelMatrix.builder()
                .models(prepared.models())
                .timeout(Duration.ofSeconds(30))
                .maxConcurrentInvocations(2)
                .build()
                .run(scenario);
        var structured = new StructuredOutputEvaluator().evaluate(
                prepared.observations(core),
                new JsonObjectSchema(Map.of("name", STRING, "age", NUMBER))
        );

        assertEquals(2, core.runs().size());
        assertTrue(core.runs().stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        assertEquals(2, structured.observations().size());
        assertTrue(structured.observations().stream().allMatch(observation -> observation.valid()));
        assertEquals(COMPATIBLE, structured.status());
    }

    private static ModelUnderTest model(String modelName) {
        return new ModelUnderTest(
                new ModelDescriptor(modelName),
                new SpringAiModelAdapter(createChatModel(modelName))
        );
    }

    private static ChatModel createChatModel(String modelName) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(OllamaChatOptions.builder()
                        .model(modelName)
                        .outputSchema(OUTPUT_SCHEMA)
                        .build())
                .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
                .build();
    }

    private static void requireModel(String modelName) {
        if (availabilityFailure != null) {
            fail(availabilityFailure);
        }
        if (availableModels.stream().noneMatch(name -> name.equals(modelName) || name.equals(modelName + ":latest"))) {
            fail("Ollama model not available: " + modelName);
        }
    }
}
