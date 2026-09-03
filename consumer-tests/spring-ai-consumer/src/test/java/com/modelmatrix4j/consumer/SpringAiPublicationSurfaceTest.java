package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.mcp.McpAdapter;
import com.modelmatrix4j.rag.RetrievalAdapter;
import com.modelmatrix4j.springai.SpringAiMcpToolAdapter;
import com.modelmatrix4j.springai.SpringAiModelAdapter;
import com.modelmatrix4j.springai.SpringAiRetrievalAdapter;
import com.modelmatrix4j.springai.SpringAiToolCallAdapter;
import com.modelmatrix4j.tool.ToolAdapter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;

class SpringAiPublicationSurfaceTest {

    @Test
    void springAiArtifactProvidesItsPublishedConstructorSurfaceTransitively() {
        ChatModel model = chatModel();
        ChatClient client = ChatClient.create(model);

        SpringAiModelAdapter modelAdapter = new SpringAiModelAdapter(model);
        SpringAiToolCallAdapter toolAdapter = new SpringAiToolCallAdapter(model);
        SpringAiRetrievalAdapter retrievalAdapter =
                new SpringAiRetrievalAdapter(client, "documents", Document::getId);
        SpringAiMcpToolAdapter mcpAdapter = new SpringAiMcpToolAdapter(client);

        assertNotNull(modelAdapter);
        assertNotNull(toolAdapter);
        assertNotNull(retrievalAdapter);
        assertNotNull(mcpAdapter);

        assertTrue(ModelAdapter.class.isAssignableFrom(SpringAiModelAdapter.class));
        assertTrue(ToolAdapter.class.isAssignableFrom(SpringAiToolCallAdapter.class));
        assertTrue(RetrievalAdapter.class.isAssignableFrom(SpringAiRetrievalAdapter.class));
        assertTrue(McpAdapter.class.isAssignableFrom(SpringAiMcpToolAdapter.class));
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
