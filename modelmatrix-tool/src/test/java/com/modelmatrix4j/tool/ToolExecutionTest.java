package com.modelmatrix4j.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolExecutionTest {

    @Test
    void capturesEvidenceFromExactlyOneCoreManagedPhysicalInvocationPerRun() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        ToolExecution.PreparedModels prepared = ToolExecution.prepare(List.of(
                model("first", firstCalls, "weather"),
                model("second", secondCalls, "weather")));

        var coreResult = ModelMatrix.builder().models(prepared.models()).repetitions(2)
                .timeout(Duration.ofSeconds(1)).build()
                .run(new Scenario("tools", "use tool"));
        List<ToolObservation> observations = prepared.observations(coreResult);

        assertEquals(2, firstCalls.get());
        assertEquals(2, secondCalls.get());
        assertEquals(4, observations.size());
        assertEquals(List.of(0, 1, 0, 1), observations.stream().map(ToolObservation::repetition).toList());
        assertEquals(List.of("first", "first", "second", "second"), observations.stream()
                .map(ToolObservation::configurationId).toList());
        assertTrue(observations.stream().allMatch(observation -> observation.calls().size() == 1));
        assertThrows(IllegalStateException.class, () -> prepared.observations(coreResult));
    }

    @Test
    void failedInvocationDoesNotProduceToolObservation() {
        ToolExecution.PreparedModels prepared = ToolExecution.prepare(List.of(
                new ToolModel(
                        new ModelDescriptor("broken"),
                        scenario -> { throw new IllegalStateException("boom"); }),
                new ToolModel(
                        new ModelDescriptor("reference"),
                        scenario -> invocation("weather"))));

        var coreResult = ModelMatrix.builder().models(prepared.models()).timeout(Duration.ofSeconds(1))
                .build().run(new Scenario("tools", "use tool"));

        assertEquals(RunStatus.FAILED, coreResult.runs().getFirst().status());
        List<ToolObservation> observations = prepared.observations(coreResult);
        assertEquals(1, observations.size());
        assertEquals("reference", observations.getFirst().configurationId());
    }

    @Test
    void timedOutLateInvocationCannotPublishEvidenceAfterConsumption() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ToolExecution.PreparedModels prepared = ToolExecution.prepare(List.of(
                new ToolModel(
                        new ModelDescriptor("slow"),
                        scenario -> {
                            entered.countDown();
                            while (true) {
                                try {
                                    release.await();
                                    break;
                                } catch (InterruptedException ignored) {
                                    // Deliberately non-cooperative to prove late publication is rejected.
                                }
                            }
                            return invocation("weather");
                        }),
                new ToolModel(
                        new ModelDescriptor("fast"),
                        scenario -> invocation("weather"))));

        var coreResult = ModelMatrix.builder().models(prepared.models()).timeout(Duration.ofMillis(50))
                .build().run(new Scenario("tools", "use tool"));
        assertTrue(entered.await(1, TimeUnit.SECONDS));
        assertEquals(RunStatus.TIMED_OUT, coreResult.runs().getFirst().status());
        List<ToolObservation> observations = prepared.observations(coreResult);
        assertEquals(1, observations.size());
        assertEquals("fast", observations.getFirst().configurationId());

        release.countDown();
        Thread.sleep(100);
        assertThrows(IllegalStateException.class, () -> prepared.observations(coreResult));
    }

    private static ToolModel model(String configurationId, AtomicInteger calls, String toolName) {
        return new ToolModel(new ModelDescriptor(configurationId), scenario -> {
            calls.incrementAndGet();
            return invocation(toolName);
        });
    }

    private static ToolInvocation invocation(String toolName) {
        return new ToolInvocation("ok", List.of(new ToolCallObservation(toolName, "{}", "done")));
    }
}
