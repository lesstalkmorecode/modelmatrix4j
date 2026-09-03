package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.rag.RetrievalComparator;
import com.modelmatrix4j.rag.RetrievalEvaluator;
import com.modelmatrix4j.rag.RetrievalExecution;
import com.modelmatrix4j.rag.RetrievalInvocation;
import com.modelmatrix4j.rag.RetrievalModel;
import com.modelmatrix4j.rag.RetrievalObservation;
import com.modelmatrix4j.rag.RetrievalResult;
import com.modelmatrix4j.rag.RetrievedDocument;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RagExternalConsumerTest {
    @Test
    void executesDocumentedRetrievalHappyPath() {
        RetrievalModel baseline = model("baseline", "source-a");
        RetrievalModel candidate = model("candidate", "source-b");
        RetrievalExecution.PreparedModels prepared = RetrievalExecution.prepare(List.of(baseline, candidate));

        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("rag-consumer", "retrieve context"));
        List<RetrievalObservation> observations = prepared.observations(coreResult);
        RetrievalResult result = new RetrievalEvaluator().evaluate(observations);

        assertTrue(coreResult.runs().stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        assertEquals(List.of("grounded answer", "grounded answer"), coreResult.runs().stream()
                .map(run -> run.output())
                .toList());
        assertEquals(List.of("doc-a", "doc-b"), observations.getFirst().documents().stream()
                .map(RetrievedDocument::documentId)
                .toList());
        assertEquals(RetrievalComparator.Outcome.EQUIVALENT,
                new RetrievalComparator().compare(observations.get(0), observations.get(1)));
        assertEquals(RetrievalResult.Status.COMPATIBLE, result.status());
    }

    private static RetrievalModel model(String configurationId, String citationPrefix) {
        return new RetrievalModel(
                new ModelDescriptor(configurationId),
                scenario -> new RetrievalInvocation(
                        "grounded answer",
                        List.of(
                                new RetrievedDocument("doc-a", Optional.of(citationPrefix + "-a")),
                                new RetrievedDocument("doc-b", Optional.of(citationPrefix + "-b")))));
    }
}
