package com.modelmatrix4j.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.rag.RetrievalEvaluator;
import com.modelmatrix4j.rag.RetrievalExecution;
import com.modelmatrix4j.rag.RetrievalInvocation;
import com.modelmatrix4j.rag.RetrievalModel;
import com.modelmatrix4j.rag.RetrievalResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;

class SpringAiRetrievalAdapterTest {
    private static final String DOCUMENTS = "retrieved_documents";

    @Test
    void mapsAdvisorContextDocumentsWithoutOwningRetrieval() throws Exception {
        Document first = new Document("provider-a", "first", Map.of("logicalId", "doc-1", "source", "a.pdf"));
        Document second = new Document("provider-b", "second", Map.of("logicalId", "doc-2", "source", "b.pdf"));
        SpringAiRetrievalAdapter adapter = adapter(
                response("answer", Map.of(DOCUMENTS, List.of(first, second))),
                document -> document.getMetadata().get("logicalId").toString());

        RetrievalInvocation invocation = adapter.invoke(new Scenario("rag", "query"));

        assertEquals(RetrievalInvocation.EvidenceStatus.VALID, invocation.evidenceStatus());
        assertEquals("answer", invocation.output());
        assertEquals(List.of("doc-1", "doc-2"),
                invocation.documents().stream().map(document -> document.documentId()).toList());
    }

    @Test
    void emptyAdvisorDocumentListIsValidNoResult() throws Exception {
        RetrievalInvocation invocation = adapter(
                response("none", Map.of(DOCUMENTS, List.of())), Document::getId)
                .invoke(new Scenario("rag", "query"));

        assertEquals(RetrievalInvocation.EvidenceStatus.VALID, invocation.evidenceStatus());
        assertEquals(List.of(), invocation.documents());
    }

    @Test
    void missingOrMalformedAdvisorEvidenceIsInvalidNotExecutionFailure() throws Exception {
        RetrievalInvocation missing = adapter(response("answer", Map.of()), Document::getId)
                .invoke(new Scenario("rag", "query"));
        RetrievalInvocation malformed = adapter(response("answer", Map.of(DOCUMENTS, "not-documents")), Document::getId)
                .invoke(new Scenario("rag", "query"));

        assertEquals(RetrievalInvocation.EvidenceStatus.INVALID, missing.evidenceStatus());
        assertEquals(RetrievalInvocation.EvidenceStatus.INVALID, malformed.evidenceStatus());
        assertEquals("answer", missing.output());
    }

    @Test
    void identityNormalizationFailureIsInvalidEvidence() throws Exception {
        Document document = new Document("provider-id", "content", Map.of());
        RetrievalInvocation invocation = adapter(
                response("answer", Map.of(DOCUMENTS, List.of(document))),
                ignored -> " ")
                .invoke(new Scenario("rag", "query"));

        assertEquals(RetrievalInvocation.EvidenceStatus.INVALID, invocation.evidenceStatus());
        assertEquals(List.of(), invocation.documents());
    }

    @Test
    void springEvidenceFlowsThroughCoreExecutionIntoRetrievalEvaluation() {
        AtomicInteger calls = new AtomicInteger();
        Document document = new Document("provider-id", "content", Map.of("logicalId", "doc-1"));
        SpringAiRetrievalAdapter adapter = new SpringAiRetrievalAdapter(
                scenario -> {
                    calls.incrementAndGet();
                    return response("answer", Map.of(DOCUMENTS, List.of(document)));
                },
                DOCUMENTS,
                value -> value.getMetadata().get("logicalId").toString(),
                ignored -> Optional.empty());
        var prepared = RetrievalExecution.prepare(List.of(
                new RetrievalModel(new ModelDescriptor("a"), adapter),
                new RetrievalModel(new ModelDescriptor("b"), adapter)));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("rag", "query"));
        var observations = prepared.observations(core);
        var retrieval = new RetrievalEvaluator().evaluate(observations);

        assertEquals(2, calls.get());
        assertEquals(2, observations.size());
        assertEquals(List.of("answer", "answer"), core.runs().stream().map(run -> run.output()).toList());
        assertEquals(RetrievalResult.Status.COMPATIBLE, retrieval.status());
    }

    private static SpringAiRetrievalAdapter adapter(
            ChatClientResponse response,
            java.util.function.Function<Document, String> identity
    ) {
        return new SpringAiRetrievalAdapter(
                ignored -> response,
                DOCUMENTS,
                identity,
                document -> Optional.ofNullable((String) document.getMetadata().get("source")));
    }

    private static ChatClientResponse response(String output, Map<String, Object> context) {
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(output))));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(context)
                .build();
    }
}
