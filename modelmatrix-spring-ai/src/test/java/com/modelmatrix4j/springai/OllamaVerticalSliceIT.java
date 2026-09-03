package com.modelmatrix4j.springai;

import java.time.Duration;
import java.util.List;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Opt-in vertical slice proving Spring AI execution against two local Ollama models. */
class OllamaVerticalSliceIT {

    private static final String OLLAMA_BASE_URL = System.getProperty("ollama.baseUrl", "http://localhost:11434");
    private static final String MODEL_1 = System.getProperty("ollama.model1", "mistral");
    private static final String MODEL_2 = System.getProperty("ollama.model2", "llama2");

    private static OllamaApi ollamaApi;
    private static List<String> availableModels = List.of();
    private static String availabilityFailure;

    @BeforeAll
    static void inspectOllamaPrerequisites() {
        try {
            ollamaApi = OllamaApi.builder()
                    .baseUrl(OLLAMA_BASE_URL)
                    .build();

            availableModels = ollamaApi.listModels()
                    .models()
                    .stream()
                    .map(OllamaApi.Model::name)
                    .toList();
        } catch (Exception exception) {
            availabilityFailure =
                    "Ollama is not available at " + OLLAMA_BASE_URL;
        }
    }

    @Test
    void sameScenarioRunsAgainstTwoOllamaModels() {
        ModelUnderTest first = createModel(MODEL_1);
        ModelUnderTest second = createModel(MODEL_2);

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

        if (result.status() == CompatibilityStatus.UNAVAILABLE) {
            assertTrue(
                    result.runs().stream()
                            .anyMatch(run ->
                                    run.status() == RunStatus.UNAVAILABLE
                            ),
                    "UNAVAILABLE compatibility must contain an unavailable run"
            );

            fail(unavailableMessage(result));
        }

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

        assertTrue(
                result.status() == CompatibilityStatus.COMPATIBLE
                        || result.status() == CompatibilityStatus.MISMATCH,
                () -> "Successful real-model execution must produce "
                        + "COMPATIBLE or MISMATCH, but was "
                        + result.status()
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

    private static ModelUnderTest createModel(String modelName) {
        ModelDescriptor descriptor =
                new ModelDescriptor("ollama-" + modelName);

        if (availabilityFailure != null) {
            return unavailableModel(
                    descriptor,
                    availabilityFailure
            );
        }

        if (!containsModel(availableModels, modelName)) {
            return unavailableModel(
                    descriptor,
                    "Ollama model not available: " + modelName
            );
        }

        return new ModelUnderTest(
                descriptor,
                new SpringAiModelAdapter(createChatModel(modelName))
        );
    }

    private static ModelUnderTest unavailableModel(
            ModelDescriptor descriptor,
            String diagnostic
    ) {
        return new ModelUnderTest(
                descriptor,
                ignored -> {
                    throw new ModelUnavailableException(diagnostic);
                }
        );
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

    private static String unavailableMessage(
            CompatibilityResult result
    ) {
        String diagnostics = String.join(
                System.lineSeparator(),
                result.runs().stream()
                        .filter(run ->
                                run.status() == RunStatus.UNAVAILABLE
                        )
                        .map(run ->
                                run.model().configurationId()
                                        + ": "
                                        + run.diagnostic()
                        )
                        .toList()
        );

        return """
                Ollama integration is unavailable.

                %s

                Ollama URL:
                  %s

                Required models:
                  %s
                  %s

                Start Ollama and pull the required models:

                  ollama serve
                  ollama pull %s
                  ollama pull %s

                Then run:

                  ./mvnw -B verify -Pollama-it
                """.formatted(
                diagnostics,
                OLLAMA_BASE_URL,
                MODEL_1,
                MODEL_2,
                MODEL_1,
                MODEL_2
        );
    }
}