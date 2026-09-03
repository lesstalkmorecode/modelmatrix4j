package com.modelmatrix4j.mcp;

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

class McpExecutionTest {

    @Test
    void correlatesPerConfigurationInvocationOrderWithCoreRepetitionsAndInvokesExactlyOnce() {
        AtomicInteger baselineCalls = new AtomicInteger();
        AtomicInteger candidateCalls = new AtomicInteger();
        var prepared = McpExecution.prepare(List.of(
                model("baseline", baselineCalls, "tool-a"),
                model("candidate", candidateCalls, "tool-a")));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .repetitions(2)
                .timeout(Duration.ofSeconds(1))
                .build()
                .run(new Scenario("mcp", "query"));
        var observations = prepared.observations(core);

        assertEquals(2, baselineCalls.get());
        assertEquals(2, candidateCalls.get());
        assertEquals(4, observations.size());
        assertEquals(List.of(0, 1, 0, 1), observations.stream().map(McpObservation::repetition).toList());
        assertEquals(List.of("tool-a", "tool-a", "tool-a", "tool-a"), observations.stream()
                .map(observation -> observation.tools().getFirst().toolId())
                .toList());
    }

    @Test
    void preservesNormalCoreOutputWithoutUsingItAsEvidenceTransport() {
        var prepared = McpExecution.prepare(List.of(
                new McpModel(new ModelDescriptor("baseline"), scenario -> new McpInvocation(
                        "normal answer", List.of(new McpToolInteraction("tool-a", "{}")))),
                new McpModel(new ModelDescriptor("candidate"), scenario -> new McpInvocation(
                        "normal answer", List.of(new McpToolInteraction("tool-a", "{}"))))));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("mcp", "query"));

        assertEquals(List.of("normal answer", "normal answer"),
                core.runs().stream().map(run -> run.output()).toList());
        assertEquals(McpResult.Status.COMPATIBLE, prepared.evaluate(core).status());
    }

    @Test
    void timedOutInvocationCannotPublishLateEvidence() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger slowCalls = new AtomicInteger();

        McpModel slow = new McpModel(new ModelDescriptor("slow"), scenario -> {
            slowCalls.incrementAndGet();
            started.countDown();
            while (release.getCount() > 0) {
                try {
                    release.await(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    // Intentionally continue: this fixture models a non-cooperative late completion.
                }
            }
            try {
                return new McpInvocation("late answer", List.of(new McpToolInteraction("late-tool", "{}")));
            } finally {
                completed.countDown();
            }
        });
        McpModel fast = new McpModel(new ModelDescriptor("fast"), scenario -> new McpInvocation(
                "ok", List.of(new McpToolInteraction("fast-tool", "{}"))));
        var prepared = McpExecution.prepare(List.of(slow, fast));

        var core = ModelMatrix.builder()
                .models(prepared.models())
                .timeout(Duration.ofMillis(50))
                .build()
                .run(new Scenario("mcp", "query"));

        assertTrue(started.await(1, TimeUnit.SECONDS));
        var observations = prepared.observations(core);
        release.countDown();
        assertTrue(completed.await(1, TimeUnit.SECONDS));

        assertEquals(1, slowCalls.get());
        assertEquals(1, observations.size());
        assertEquals("fast", observations.getFirst().configurationId());
        assertEquals("fast-tool", observations.getFirst().tools().getFirst().toolId());
        assertEquals(1, core.runs().stream().filter(run -> run.status() == RunStatus.TIMED_OUT).count());
    }

    @Test
    void evidenceCanOnlyBeConsumedOnce() {
        var prepared = McpExecution.prepare(List.of(
                model("baseline", new AtomicInteger(), "tool-a"),
                model("candidate", new AtomicInteger(), "tool-a")));
        var core = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("mcp", "query"));

        prepared.observations(core);

        assertThrows(IllegalStateException.class, () -> prepared.observations(core));
    }

    private static McpModel model(String configurationId, AtomicInteger calls, String toolId) {
        return new McpModel(new ModelDescriptor(configurationId), scenario -> {
            calls.incrementAndGet();
            return new McpInvocation("answer", List.of(new McpToolInteraction(toolId, "{\"value\":1}")));
        });
    }
}
