package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModelMatrixTest {
    private static final Scenario SCENARIO = new Scenario("greeting", "  hello  ");

    @Test
    void runsThePublicFacadeWithNormalizedOrderedResults() {
        ModelMatrix matrix = ModelMatrix.builder()
                .models(model("first", scenario -> " hello\n world "),
                        model("second", scenario -> "hello   world"))
                .repetitions(2)
                .timeout(Duration.ofSeconds(1))
                .build();

        CompatibilityResult result = matrix.run(SCENARIO);

        assertEquals(CompatibilityStatus.COMPATIBLE, result.status());
        assertEquals(List.of("first", "first", "second", "second"), result.runs().stream()
                .map(run -> run.model().configurationId()).toList());
        assertEquals(List.of(0, 1, 0, 1), result.runs().stream().map(RunResult::repetition).toList());
        assertTrue(result.runs().stream().allMatch(run -> run.output().equals("hello world")));
        assertEquals("8:greeting:5:first:0", result.runs().getFirst().runId());
        assertNotEquals(result.runs().get(0).runId(), result.runs().get(1).runId());
    }

    @Test
    void reportsMismatchBeforeLossyPublicRedaction() {
        CompatibilityResult result = matrix(
                model("a", scenario -> "token=abc"),
                model("b", scenario -> "token=xyz")).run(SCENARIO);

        assertEquals(CompatibilityStatus.MISMATCH, result.status());
        assertEquals(List.of("token=[REDACTED]", "token=[REDACTED]"),
                result.runs().stream().map(RunResult::output).toList());
        assertFalse(result.toString().contains("abc"));
        assertFalse(result.toString().contains("xyz"));
    }

    @Test
    void acceptsAnyProviderNeutralAdapter() {
        ModelAdapter customAdapter = scenario -> "custom response";

        CompatibilityResult result = matrix(new ModelUnderTest(
                new ModelDescriptor("custom"), customAdapter)).run(SCENARIO);

        assertEquals(CompatibilityStatus.COMPATIBLE, result.status());
        assertEquals("custom response", result.runs().getFirst().output());
    }

    @Test
    void reportsFailureWithoutHiddenRetry() {
        AtomicInteger calls = new AtomicInteger();
        CompatibilityResult result = matrix(model("broken", scenario -> {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        })).run(SCENARIO);

        assertEquals(1, calls.get());
        assertEquals(CompatibilityStatus.EXECUTION_FAILURE, result.status());
        assertEquals(RunStatus.FAILED, result.runs().getFirst().status());
    }

    @Test
    void composesFailureClassificationWithBoundedRedactedDiagnostics() {
        String secret = "x".repeat(700);
        CompatibilityResult result = matrix(model("broken", scenario -> {
            throw new IllegalArgumentException("token=" + secret + "; password=hunter2");
        })).run(SCENARIO);

        String diagnostic = result.runs().getFirst().diagnostic();
        assertEquals(CompatibilityStatus.EXECUTION_FAILURE, result.status());
        assertTrue(diagnostic.length() <= 512);
        assertTrue(diagnostic.contains("token=[REDACTED]"));
        assertTrue(diagnostic.contains("password=[REDACTED]"));
        assertFalse(diagnostic.contains("hunter2"));
        assertFalse(diagnostic.contains(secret));
    }

    @Test
    void reportsUnavailableDistinctly() {
        CompatibilityResult result = matrix(
                model("offline", scenario -> {
                    throw new ModelUnavailableException("not installed");
                }),
                model("ready", scenario -> "ok")).run(SCENARIO);

        assertEquals(CompatibilityStatus.UNAVAILABLE, result.status());
        assertEquals(RunStatus.UNAVAILABLE, result.runs().getFirst().status());
    }

    @Test
    void propagatesTheSameFatalErrorInstance() {
        LinkageError fatal = new LinkageError("fatal");
        ModelMatrix matrix = matrix(model("fatal", scenario -> {
            throw fatal;
        }));

        assertSame(fatal, assertThrows(LinkageError.class, () -> matrix.run(SCENARIO)));
    }

    @Test
    void returnsAnImmutableResultSnapshot() {
        List<RunResult> mutable = new ArrayList<>();
        mutable.add(completed("one", "same"));
        CompatibilityResult result = new CompatibilityResult(CompatibilityStatus.COMPATIBLE, mutable);
        mutable.add(completed("two", "different"));

        assertEquals(1, result.runs().size());
        assertThrows(UnsupportedOperationException.class,
                () -> result.runs().add(completed("three", "x")));
    }

    @Test
    void rejectsInvalidConfigurationBeforeInvocation() {
        AtomicInteger calls = new AtomicInteger();
        ModelUnderTest model = model("model", scenario -> {
            calls.incrementAndGet();
            return "unused";
        });

        assertThrows(IllegalStateException.class, () -> ModelMatrix.builder().build());
        assertThrows(IllegalArgumentException.class,
                () -> ModelMatrix.builder().models(List.of()).build());
        assertThrows(IllegalArgumentException.class,
                () -> ModelMatrix.builder()
                        .models(model, model("model", scenario -> "unused"))
                        .build());
        assertThrows(IllegalArgumentException.class,
                () -> ModelMatrix.builder().models(model).repetitions(0).build());
        assertThrows(IllegalArgumentException.class,
                () -> ModelMatrix.builder().models(model).timeout(Duration.ZERO).build());
        assertThrows(NullPointerException.class,
                () -> ModelMatrix.builder().models(model).timeout(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> ModelMatrix.builder().models(model).maxConcurrentInvocations(0).build());
        assertThrows(IllegalArgumentException.class, () -> new Scenario(" ", "input"));
        assertThrows(IllegalArgumentException.class, () -> new ModelDescriptor(" "));
        assertThrows(NullPointerException.class,
                () -> ModelMatrix.builder().models(model).build().run(null));
        assertEquals(0, calls.get());
    }

    private static ModelMatrix matrix(ModelUnderTest... models) {
        return ModelMatrix.builder().models(models).timeout(Duration.ofSeconds(1)).build();
    }

    private static ModelUnderTest model(String id, ModelAdapter adapter) {
        return new ModelUnderTest(new ModelDescriptor(id), adapter);
    }

    private static RunResult completed(String model, String output) {
        return new RunResult("run-" + model, "scenario", new ModelDescriptor(model), 0,
                RunStatus.COMPLETED, output, Duration.ZERO, "");
    }
}
