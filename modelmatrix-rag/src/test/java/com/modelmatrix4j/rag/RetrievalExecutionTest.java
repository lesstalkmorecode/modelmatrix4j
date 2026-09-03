package com.modelmatrix4j.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetrievalExecutionTest {

    @Test
    void correlatesPerConfigurationInvocationOrderWithCoreRepetitions() {
        AtomicInteger baselineCalls = new AtomicInteger();
        AtomicInteger candidateCalls = new AtomicInteger();

        RetrievalExecution.PreparedModels prepared = RetrievalExecution.prepare(List.of(
                model("baseline", baselineCalls, "doc-a"),
                model("candidate", candidateCalls, "doc-a")));

        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .repetitions(2)
                .timeout(Duration.ofSeconds(1))
                .build()
                .run(new Scenario("rag", "query"));

        List<RetrievalObservation> observations = prepared.observations(coreResult);

        assertEquals(2, baselineCalls.get());
        assertEquals(2, candidateCalls.get());
        assertEquals(4, observations.size());
        assertEquals(List.of(0, 1, 0, 1), observations.stream().map(RetrievalObservation::repetition).toList());
        assertEquals(List.of("doc-a", "doc-a", "doc-a", "doc-a"), observations.stream()
                .map(observation -> observation.documents().getFirst().documentId()).toList());
    }

    @Test
    void preservesNormalCoreOutputWithoutUsingItAsEvidenceTransport() {
        RetrievalExecution.PreparedModels prepared = RetrievalExecution.prepare(List.of(
                new RetrievalModel(new ModelDescriptor("baseline"), scenario ->
                        new RetrievalInvocation("normal answer", List.of(new RetrievedDocument("doc-a")))),
                new RetrievalModel(new ModelDescriptor("candidate"), scenario ->
                        new RetrievalInvocation("normal answer", List.of(new RetrievedDocument("doc-a"))))));

        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("rag", "query"));

        assertEquals(List.of("normal answer", "normal answer"),
                coreResult.runs().stream().map(run -> run.output()).toList());
        assertEquals(2, prepared.observations(coreResult).size());
    }

    @Test
    void timedOutInvocationCannotPublishLateEvidence() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();

        RetrievalModel slow = new RetrievalModel(new ModelDescriptor("slow"), scenario -> {
            calls.incrementAndGet();
            started.countDown();
            while (!release.await(10, TimeUnit.MILLISECONDS)) {
                // Ignore interruption deliberately to simulate a late adapter completion.
            }
            return new RetrievalInvocation("late", List.of(new RetrievedDocument("late-doc")));
        });
        RetrievalModel fast = new RetrievalModel(new ModelDescriptor("fast"), scenario ->
                new RetrievalInvocation("ok", List.of(new RetrievedDocument("doc-a"))));
        RetrievalExecution.PreparedModels prepared = RetrievalExecution.prepare(List.of(slow, fast));

        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .timeout(Duration.ofMillis(50))
                .build()
                .run(new Scenario("rag", "query"));

        started.await(1, TimeUnit.SECONDS);
        List<RetrievalObservation> observations = prepared.observations(coreResult);
        release.countDown();
        Thread.sleep(50);

        assertEquals(1, calls.get());
        assertEquals(1, observations.size());
        assertEquals("fast", observations.getFirst().configurationId());
        assertEquals("doc-a", observations.getFirst().documents().getFirst().documentId());
    }

    @Test
    void evidenceCanOnlyBeConsumedOnce() {
        RetrievalExecution.PreparedModels prepared = RetrievalExecution.prepare(List.of(
                model("baseline", new AtomicInteger(), "doc-a"),
                model("candidate", new AtomicInteger(), "doc-a")));
        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("rag", "query"));

        prepared.observations(coreResult);

        assertThrows(IllegalStateException.class, () -> prepared.observations(coreResult));
    }

    private static RetrievalModel model(String configurationId, AtomicInteger calls, String documentId) {
        return new RetrievalModel(new ModelDescriptor(configurationId), scenario -> {
            calls.incrementAndGet();
            return new RetrievalInvocation("answer", List.of(new RetrievedDocument(documentId)));
        });
    }
}
