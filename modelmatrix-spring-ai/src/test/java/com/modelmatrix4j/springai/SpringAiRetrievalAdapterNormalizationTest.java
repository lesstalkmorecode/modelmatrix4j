package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.rag.RetrievalInvocation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;

class SpringAiRetrievalAdapterNormalizationTest {
    private static final String DOCUMENTS = "retrieved_documents";

    @Test
    void runtimeFailureInIdentityFunctionIsInvalidEvidence() throws Exception {
        Document document = new Document("provider-id", "content", Map.of());
        SpringAiRetrievalAdapter adapter = new SpringAiRetrievalAdapter(
                ignored -> response(List.of(document)),
                DOCUMENTS,
                ignored -> { throw new NullPointerException("broken identity mapping"); },
                ignored -> Optional.empty());

        RetrievalInvocation invocation = adapter.invoke(new Scenario("rag", "query"));

        assertEquals(RetrievalInvocation.EvidenceStatus.INVALID, invocation.evidenceStatus());
        assertEquals("answer", invocation.output());
    }

    private static ChatClientResponse response(List<Document> documents) {
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage("answer"))));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(Map.of(DOCUMENTS, documents))
                .build();
    }
}
