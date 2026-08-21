package com.modelmatrix4j.springai;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpringAiModelAdapterTest {

    @Test
    void constructorRejectsNullChatModel() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SpringAiModelAdapter(null)
        );

        assertEquals("chatModel", exception.getMessage());
    }

    @Test
    void invokePassesScenarioInputToChatModel() throws Exception {
        String expectedInput = "Hello model";
        String expectedOutput = "Hello user";

        FakeChatModel fakeChatModel = new FakeChatModel(expectedOutput);
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(fakeChatModel);

        Scenario scenario = new Scenario("test", expectedInput);
        String result = adapter.invoke(scenario);

        assertEquals(expectedOutput, result);
        assertEquals(expectedInput, fakeChatModel.lastInvokedInput);
        assertEquals(1, fakeChatModel.invokeCount);
    }

    @Test
    void invokeReturnsRawTextUnchanged() throws Exception {
        String rawText = "  Hello   \n  world  ";
        FakeChatModel fakeChatModel = new FakeChatModel(rawText);
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(fakeChatModel);

        Scenario scenario = new Scenario("id", "input");
        String result = adapter.invoke(scenario);

        assertEquals(rawText, result);
    }

    @Test
    void invokesModelExactlyOnce() throws Exception {
        FakeChatModel fakeChatModel = new FakeChatModel("response");
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(fakeChatModel);

        Scenario scenario = new Scenario("id", "input");
        adapter.invoke(scenario);

        assertEquals(1, fakeChatModel.invokeCount);
    }

    @Test
    void propagatesNormalExceptions() {
        FakeChatModel fakeChatModel = new FakeChatModel(
                new RuntimeException("Model error")
        );
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(fakeChatModel);

        Scenario scenario = new Scenario("id", "input");
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.invoke(scenario)
        );

        assertEquals("Model error", exception.getMessage());
    }

    @Test
    void matrixClassifiesChatModelFailureAsExecutionFailure() {
        ChatModel chatModel = new FakeChatModel(
                new RuntimeException("boom")
        );

        ModelUnderTest model = new ModelUnderTest(
                new ModelDescriptor("spring-ai-test"),
                new SpringAiModelAdapter(chatModel)
        );

        CompatibilityResult result = ModelMatrix.builder()
                .models(model)
                .build()
                .run(new Scenario("test", "hello"));

        assertEquals(CompatibilityStatus.EXECUTION_FAILURE, result.status());
        assertEquals(RunStatus.FAILED, result.runs().getFirst().status());
    }

    @Test
    void matrixNormalizesAdapterOutput() {
        ChatModel chatModel = new FakeChatModel("  Hello   \n world  ");

        SpringAiModelAdapter adapter = new SpringAiModelAdapter(chatModel);

        ModelUnderTest model = new ModelUnderTest(
                new ModelDescriptor("spring-ai-test"),
                adapter
        );

        CompatibilityResult result = ModelMatrix.builder()
                .models(model)
                .build()
                .run(new Scenario("test", "hello"));

        assertEquals("Hello world", result.runs().getFirst().output());
    }

    @Test
    void handlesEmptyResponse() throws Exception {
        FakeChatModel fakeChatModel = new FakeChatModel("");
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(fakeChatModel);

        Scenario scenario = new Scenario("id", "input");
        String result = adapter.invoke(scenario);

        assertEquals("", result);
    }

    @Test
    void handlesMultilineResponse() throws Exception {
        String multiline = "Line 1\nLine 2\nLine 3";
        FakeChatModel fakeChatModel = new FakeChatModel(multiline);
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(fakeChatModel);

        Scenario scenario = new Scenario("id", "input");
        String result = adapter.invoke(scenario);

        assertEquals(multiline, result);
    }

    /** Deterministic stub ChatModel for testing. */
    private static final class FakeChatModel implements ChatModel {

        private final String response;
        private final RuntimeException failure;

        private String lastInvokedInput;
        private int invokeCount;

        FakeChatModel(String response) {
            this.response = response;
            this.failure = null;
        }

        FakeChatModel(RuntimeException failure) {
            this.response = null;
            this.failure = failure;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            invokeCount++;
            lastInvokedInput = prompt.getContents();

            if (failure != null) {
                throw failure;
            }

            var message = new AssistantMessage(response);
            return new ChatResponse(List.of(new Generation(message)));
        }
    }
}

