package com.modelmatrix4j.structured;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StructuredOutputExecutionTest {

    @Test
    void capturesRawEvidenceInsideCoreRepetitionLifecycle() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        var prepared = StructuredOutputExecution.prepare(List.of(
                new ModelUnderTest(
                        new ModelDescriptor("first"),
                        scenario -> "{\"attempt\":" + firstCalls.getAndIncrement() + "}"),
                new ModelUnderTest(
                        new ModelDescriptor("second"),
                        scenario -> "{\"attempt\":" + secondCalls.getAndIncrement() + "}")));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .repetitions(2)
                .timeout(Duration.ofSeconds(1))
                .build()
                .run(new Scenario("structured", "json"));
        var evidence = prepared.observations(core);

        assertEquals(2, firstCalls.get());
        assertEquals(2, secondCalls.get());
        assertEquals(List.of(0, 1, 0, 1), evidence.stream().map(StructuredOutputObservation::repetition).toList());
        assertEquals("{\"attempt\":0}", evidence.get(0).output());
        assertEquals("{\"attempt\":1}", evidence.get(1).output());
        assertTrue(core.runs().stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        assertTrue(core.runs().stream().allMatch(run -> run.output().equals("[structured output captured]")));
        assertFalse(core.runs().toString().contains("attempt"));
    }

    @Test
    void timeoutAndCancellationStayOwnedByCoreAndProduceNoLateEvidence() throws Exception {
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicInteger slowCalls = new AtomicInteger();
        var prepared = StructuredOutputExecution.prepare(List.of(
                new ModelUnderTest(
                        new ModelDescriptor("slow"),
                        scenario -> {
                            slowCalls.incrementAndGet();
                            try {
                                Thread.sleep(Duration.ofSeconds(10));
                            } catch (InterruptedException exception) {
                                interrupted.countDown();
                                Thread.currentThread().interrupt();
                            }
                            return "{\"late\":true}";
                        }),
                new ModelUnderTest(
                        new ModelDescriptor("fast"),
                        scenario -> "{\"late\":false}")));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .repetitions(2)
                .timeout(Duration.ofMillis(50))
                .build()
                .run(new Scenario("structured", "json"));
        var evidence = prepared.observations(core);

        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertEquals(1, slowCalls.get());
        assertEquals(RunStatus.TIMED_OUT, core.runs().get(0).status());
        assertEquals(RunStatus.CANCELLED, core.runs().get(1).status());
        assertEquals(2, evidence.size());
        assertTrue(evidence.stream().allMatch(observation -> observation.configurationId().equals("fast")));
    }
}
