package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.scenario.Scenario;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;

class PublicAdapterContractTest {

    @Test
    void toolAdapterRejectsMissingConstructionAndInvocationInputs() {
        ChatModel model = chatModel();
        SpringAiToolCallAdapter adapter = new SpringAiToolCallAdapter(model);

        assertThrows(NullPointerException.class,
                () -> new SpringAiToolCallAdapter(null));
        assertThrows(NullPointerException.class,
                () -> new SpringAiToolCallAdapter(model, (ToolCallback[]) null));
        assertThrows(NullPointerException.class,
                () -> new SpringAiToolCallAdapter(model, (ToolCallback) null));
        assertThrows(NullPointerException.class, () -> adapter.invoke(null));
    }

    @Test
    void retrievalAdapterRejectsMissingOrBlankPublicConstructorInputs() {
        ChatClient client = ChatClient.create(chatModel());
        Function<Document, String> identity = Document::getId;

        assertThrows(NullPointerException.class,
                () -> new SpringAiRetrievalAdapter((ChatClient) null, "documents", identity));
        assertThrows(NullPointerException.class,
                () -> new SpringAiRetrievalAdapter(client, null, identity));
        assertThrows(IllegalArgumentException.class,
                () -> new SpringAiRetrievalAdapter(client, " ", identity));
        assertThrows(NullPointerException.class,
                () -> new SpringAiRetrievalAdapter(client, "documents", null));
        assertThrows(NullPointerException.class,
                () -> new SpringAiRetrievalAdapter(
                        client, "documents", identity, (Function<Document, Optional<String>>) null));
    }

    @Test
    void mcpAdapterRejectsMissingConstructionAndInvocationInputs() {
        ChatClient client = ChatClient.create(chatModel());
        SpringAiMcpToolAdapter adapter = new SpringAiMcpToolAdapter(client);

        assertThrows(NullPointerException.class,
                () -> new SpringAiMcpToolAdapter(null));
        assertThrows(NullPointerException.class,
                () -> new SpringAiMcpToolAdapter(client, (ToolCallback[]) null));
        assertThrows(NullPointerException.class,
                () -> new SpringAiMcpToolAdapter(client, (ToolCallback) null));
        assertThrows(NullPointerException.class, () -> adapter.invoke((Scenario) null));
    }

    private static ChatModel chatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("response"))));
            }
        };
    }
}
