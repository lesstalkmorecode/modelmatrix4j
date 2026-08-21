package com.modelmatrix4j.springai;

import java.time.Duration;
import java.util.List;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Opt-in vertical slice proving Spring AI execution against two local Ollama models. */
class OllamaVerticalSliceIT {

    private static final String OLLAMA_BASE_URL = System.getProperty("ollama.baseUrl", "http://localhost:11434");

    private static final String MODEL_1 = System.getProperty("ollama.model1", "mistral");

    private static final String MODEL_2 = System.getProperty("ollama.model2", "llama2");

    private static OllamaApi ollamaApi;

    @BeforeAll
    static void verifyOllamaPrerequisites() {
        try {
            ollamaApi = OllamaApi.builder()
                    .baseUrl(OLLAMA_BASE_URL)
                    .build();

            List<String> availableModels = ollamaApi.listModels()
                    .models()
                    .stream()
                    .map(OllamaApi.Model::name)
                    .toList();

            if (!containsModel(availableModels, MODEL_1)
                    || !containsModel(availableModels, MODEL_2)) {
                throw new AssertionError(
                        """
                        Required Ollama models are not available.

                        Ollama URL:
                          %s

                        Required models:
                          %s
                          %s

                        Available models:
                          %s

                        Pull the missing models with:

                          ollama pull %s
                          ollama pull %s

                        Then run:

                          ./mvnw -B verify -Pollama-it
                        """.formatted(
                                OLLAMA_BASE_URL,
                                MODEL_1,
                                MODEL_2,
                                availableModels,
                                MODEL_1,
                                MODEL_2
                        )
                );
            }
        } catch (Exception exception) {
            throw new AssertionError(
                    """
                    Ollama is not available.

                    Expected Ollama URL:
                      %s

                    Start Ollama:

                      ollama serve

                    Pull the required models:

                      ollama pull %s
                      ollama pull %s

                    Then run:

                      ./mvnw -B verify -Pollama-it

                    You can override the configuration with:

                      -Dollama.baseUrl=http://localhost:11434
                      -Dollama.model1=%s
                      -Dollama.model2=%s
                    """.formatted(
                            OLLAMA_BASE_URL,
                            MODEL_1,
                            MODEL_2,
                            MODEL_1,
                            MODEL_2
                    ),
                    exception
            );
        }
    }

    @Test
    void sameScenarioRunsAgainstTwoOllamaModels() {
        ChatModel firstChatModel = createChatModel(MODEL_1);
        ChatModel secondChatModel = createChatModel(MODEL_2);

        ModelUnderTest first = new ModelUnderTest(
                new ModelDescriptor("ollama-" + MODEL_1),
                new SpringAiModelAdapter(firstChatModel)
        );

        ModelUnderTest second = new ModelUnderTest(
                new ModelDescriptor("ollama-" + MODEL_2),
                new SpringAiModelAdapter(secondChatModel)
        );

        Scenario scenario = new Scenario(
                "greeting",
                "Say hello in one short sentence."
        );

        ModelMatrix matrix = ModelMatrix.builder()
                .models(first, second)
                .repetitions(1)
                .timeout(Duration.ofSeconds(60))
                .build();

        CompatibilityResult result = matrix.run(scenario);

        assertEquals(
                2,
                result.runs().size(),
                "Expected exactly two model results"
        );

        assertEquals(
                "ollama-" + MODEL_1,
                result.runs().get(0).model().configurationId(),
                "First result must preserve first model declaration order"
        );

        assertEquals(
                "ollama-" + MODEL_2,
                result.runs().get(1).model().configurationId(),
                "Second result must preserve second model declaration order"
        );

        for (RunResult run : result.runs()) {
            assertEquals(
                    RunStatus.COMPLETED,
                    run.status(),
                    () -> "Expected completed run for "
                            + run.model().configurationId()
                            + ", diagnostic: "
                            + run.diagnostic()
            );

            assertNotNull(
                    run.output(),
                    "Completed model run must contain output"
            );

            assertFalse(
                    run.output().isBlank(),
                    "Completed model output must not be blank"
            );

            assertFalse(
                    run.duration().isNegative(),
                    "Run duration must not be negative"
            );
        }

        assertNotNull(
                result.status(),
                "Compatibility result must have a status"
        );

        System.out.println("Compatibility status: " + result.status());

        for (RunResult run : result.runs()) {
            System.out.println(
                    run.model().configurationId()
                            + " -> "
                            + run.output()
            );
        }
    }

    private static ChatModel createChatModel(String modelName) {
        RetryTemplate noRetry =
                new RetryTemplate(
                        RetryPolicy.withMaxRetries(0)
                );

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(
                        OllamaChatOptions.builder()
                                .model(modelName)
                                .build()
                )
                .retryTemplate(noRetry)
                .build();
    }

    private static boolean containsModel(
            List<String> availableModels,
            String requestedModel
    ) {
        return availableModels.stream()
                .anyMatch(modelName ->
                        modelName.equals(requestedModel)
                                || modelName.equals(requestedModel + ":latest")
                );
    }
}