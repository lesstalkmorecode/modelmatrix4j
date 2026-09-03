package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MatrixExecutorTest {
    private static final Scenario SCENARIO = new Scenario("scenario", "input");

    @Test
    void preInterruptedCallerStartsNothingAndReceivesOrderedCancellationOutcomes() {
        AtomicInteger calls = new AtomicInteger();
        List<ModelUnderTest> models = List.of(
                model("first", ignored -> {
                    calls.incrementAndGet();
                    return "unexpected";
                }),
                model("second", ignored -> {
                    calls.incrementAndGet();
                    return "unexpected";
                }));

        Thread.currentThread().interrupt();
        try {
            List<RunResult> runs = run(models, 2, Duration.ofSeconds(1)).runs();

            assertEquals(0, calls.get());
            assertEquals(List.of("first", "first", "second", "second"), runs.stream()
                    .map(run -> run.model().configurationId()).toList());
            assertTrue(runs.stream().allMatch(run -> run.status() == RunStatus.CANCELLED));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void differentModelsOverlapButOutcomesRetainDeclarationOrder() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);
        List<ModelUnderTest> models = List.of(
                model("first", ignored -> {
                    bothStarted.countDown();
                    assertTrue(bothStarted.await(1, TimeUnit.SECONDS), "models did not overlap");
                    releaseFirst.await();
                    return "first";
                }),
                model("second", ignored -> {
                    bothStarted.countDown();
                    assertTrue(bothStarted.await(1, TimeUnit.SECONDS), "models did not overlap");
                    secondFinished.countDown();
                    return "second";
                }));
        CompletableFuture<CompatibilityResult> execution = runAsync(models, 1, Duration.ofSeconds(2));

        try {
            assertTrue(secondFinished.await(1, TimeUnit.SECONDS));
            releaseFirst.countDown();
            List<RunResult> runs = execution.get(1, TimeUnit.SECONDS).runs();

            assertEquals(List.of("first", "second"), runs.stream()
                    .map(run -> run.model().configurationId()).toList());
        } finally {
            releaseFirst.countDown();
        }
    }

    @Test
    void distinctTargetsMayOverlapWhenTheyShareAnAdapterInstance() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ModelAdapter shared = ignored -> {
            bothStarted.countDown();
            assertTrue(bothStarted.await(1, TimeUnit.SECONDS), "shared adapter did not overlap");
            release.await();
            return "same";
        };
        CompletableFuture<CompatibilityResult> execution = runAsync(List.of(
                model("first", shared), model("second", shared)), 1, Duration.ofSeconds(2));

        try {
            assertTrue(bothStarted.await(1, TimeUnit.SECONDS));
            release.countDown();
            assertEquals(2, execution.get(1, TimeUnit.SECONDS).runs().size());
        } finally {
            release.countDown();
        }
    }

    @Test
    void repetitionsForOneModelAreSequential() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ModelUnderTest repeated = model("repeated", ignored -> {
            int call = calls.getAndIncrement();
            maximumActive.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                if (call == 0) {
                    firstStarted.countDown();
                    releaseFirst.await();
                }
                return "call-" + call;
            } finally {
                active.decrementAndGet();
            }
        });
        ModelUnderTest companion = model("companion", ignored -> "stable");
        CompletableFuture<CompatibilityResult> execution =
                runAsync(List.of(repeated, companion), 3, Duration.ofSeconds(2));

        try {
            assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            releaseFirst.countDown();
            List<RunResult> runs = execution.get(1, TimeUnit.SECONDS).runs();
            List<RunResult> repeatedRuns = runs.stream()
                    .filter(run -> run.model().configurationId().equals("repeated"))
                    .toList();

            assertEquals(3, calls.get());
            assertEquals(1, maximumActive.get());
            assertEquals(List.of(0, 1, 2), repeatedRuns.stream().map(RunResult::repetition).toList());
        } finally {
            releaseFirst.countDown();
        }
    }

    @Test
    void physicalInvocationBoundIsRespectedAndCapacityRecoversOnAdapterExit() throws Exception {
        int bound = ExecutionSettings.defaults().maxConcurrentInvocations();
        int modelCount = bound + 3;
        CountDownLatch firstWaveStarted = new CountDownLatch(bound);
        CountDownLatch nextAdmitted = new CountDownLatch(bound + 1);
        Semaphore releaseInvocations = new Semaphore(0);
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        List<ModelUnderTest> models = new ArrayList<>();
        for (int index = 0; index < modelCount; index++) {
            models.add(model("model-" + index, ignored -> {
                calls.incrementAndGet();
                maximumActive.accumulateAndGet(active.incrementAndGet(), Math::max);
                firstWaveStarted.countDown();
                nextAdmitted.countDown();
                try {
                    releaseInvocations.acquire();
                    return "same";
                } finally {
                    active.decrementAndGet();
                }
            }));
        }
        CompletableFuture<CompatibilityResult> execution =
                runAsync(models, 1, Duration.ofSeconds(5));

        try {
            assertTrue(firstWaveStarted.await(1, TimeUnit.SECONDS));
            assertEquals(bound, calls.get());
            assertEquals(bound, active.get());
            releaseInvocations.release();
            assertTrue(nextAdmitted.await(1, TimeUnit.SECONDS), "capacity was not recovered");
            releaseInvocations.release(modelCount);

            assertEquals(modelCount, execution.get(2, TimeUnit.SECONDS).runs().size());
            assertEquals(bound, maximumActive.get());
        } finally {
            releaseInvocations.release(modelCount);
        }
    }

    @Test
    void timedOutNonCooperativeInvocationsRetainTheirPhysicalCapacity() throws Exception {
        int bound = ExecutionSettings.defaults().maxConcurrentInvocations();
        int modelCount = bound + 3;
        CountDownLatch firstWaveStarted = new CountDownLatch(bound);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(bound);
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();
        List<ModelUnderTest> models = new ArrayList<>();
        for (int index = 0; index < modelCount; index++) {
            models.add(model("noncoop-" + index, ignored -> {
                calls.incrementAndGet();
                active.incrementAndGet();
                firstWaveStarted.countDown();
                try {
                    awaitIgnoringInterrupts(release);
                    return "released";
                } finally {
                    active.decrementAndGet();
                    exited.countDown();
                }
            }));
        }
        CompletableFuture<CompatibilityResult> execution =
                runAsync(models, 1, Duration.ofMillis(200));

        try {
            assertTrue(firstWaveStarted.await(1, TimeUnit.SECONDS));
            List<RunResult> runs = execution.get(2, TimeUnit.SECONDS).runs();

            assertEquals(bound, calls.get());
            assertEquals(bound, active.get());
            assertEquals(modelCount, runs.size());
            assertEquals(modelCount, runs.stream()
                    .filter(run -> run.status() == RunStatus.TIMED_OUT).count());
            assertTrue(runs.stream().allMatch(run ->
                    run.diagnostic().equals("Model execution exceeded timeout")));
        } finally {
            release.countDown();
            assertTrue(exited.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void timeoutSuppressesOnlyLaterRepetitionsOfTheSameModel() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch slowExited = new CountDownLatch(1);
        AtomicInteger slowCalls = new AtomicInteger();
        AtomicInteger healthyCalls = new AtomicInteger();
        List<ModelUnderTest> models = List.of(
                model("slow", ignored -> {
                    slowCalls.incrementAndGet();
                    try {
                        awaitIgnoringInterrupts(release);
                        return "released";
                    } finally {
                        slowExited.countDown();
                    }
                }),
                model("healthy", ignored ->
                        "healthy-" + healthyCalls.getAndIncrement()));

        try {
            List<RunResult> runs = run(models, 2, Duration.ofMillis(500)).runs();

            assertEquals(1, slowCalls.get());
            assertEquals(2, healthyCalls.get());
            assertEquals(List.of(RunStatus.TIMED_OUT, RunStatus.CANCELLED,
                            RunStatus.COMPLETED, RunStatus.COMPLETED),
                    runs.stream().map(RunResult::status).toList());
        } finally {
            release.countDown();
            assertTrue(slowExited.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void expectedFailuresDoNotSuppressLaterRepetitions() {
        AtomicInteger failedCalls = new AtomicInteger();
        AtomicInteger unavailableCalls = new AtomicInteger();
        AtomicInteger cancelledCalls = new AtomicInteger();
        List<ModelUnderTest> models = List.of(
                model("failed", ignored -> {
                    failedCalls.incrementAndGet();
                    throw new IllegalStateException("failed");
                }),
                model("unavailable", ignored -> {
                    unavailableCalls.incrementAndGet();
                    throw new ModelUnavailableException("offline");
                }),
                model("cancelled", ignored -> {
                    cancelledCalls.incrementAndGet();
                    throw new CancellationException("cancelled");
                }));

        List<RunResult> runs = run(models, 2, Duration.ofSeconds(1)).runs();

        assertEquals(2, failedCalls.get());
        assertEquals(2, unavailableCalls.get());
        assertEquals(2, cancelledCalls.get());
        assertEquals(List.of(RunStatus.FAILED, RunStatus.FAILED,
                        RunStatus.UNAVAILABLE, RunStatus.UNAVAILABLE,
                        RunStatus.CANCELLED, RunStatus.CANCELLED),
                runs.stream().map(RunResult::status).toList());
    }

    @Test
    void callerInterruptionStopsNewWorkInterruptsActiveTasksAndRestoresInterruptStatus()
            throws Exception {
        int modelCount = 3;
        int repetitions = 3;
        CountDownLatch initialInvocations = new CountDownLatch(modelCount);
        CountDownLatch adapterInterrupted = new CountDownLatch(modelCount);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        List<ModelUnderTest> models = new ArrayList<>();
        for (int index = 0; index < modelCount; index++) {
            models.add(model("cancel-" + index, ignored -> {
                calls.incrementAndGet();
                initialInvocations.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    adapterInterrupted.countDown();
                    throw exception;
                }
                return "late";
            }));
        }
        AtomicReference<List<RunResult>> result = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        Thread caller = Thread.startVirtualThread(() -> {
            result.set(run(models, repetitions, Duration.ofSeconds(5)).runs());
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });

        try {
            assertTrue(initialInvocations.await(1, TimeUnit.SECONDS));
            caller.interrupt();
            assertTrue(caller.join(Duration.ofSeconds(1)));

            assertEquals(modelCount, calls.get());
            assertEquals(modelCount * repetitions, result.get().size());
            assertEquals(List.of("cancel-0", "cancel-0", "cancel-0", "cancel-1", "cancel-1",
                            "cancel-1", "cancel-2", "cancel-2", "cancel-2"),
                    result.get().stream().map(run -> run.model().configurationId()).toList());
            assertTrue(result.get().stream().allMatch(run -> run.status() == RunStatus.CANCELLED));
            assertTrue(interruptRestored.get());
            assertTrue(adapterInterrupted.await(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            caller.interrupt();
            caller.join(Duration.ofSeconds(1));
        }
    }

    @Test
    void callerInterruptionPreservesAnAlreadyCompletedModelOutcome() throws Exception {
        CountDownLatch firstReturned = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        List<ModelUnderTest> models = List.of(
                model("completed", ignored -> {
                    firstReturned.countDown();
                    return "done";
                }),
                model("blocked", ignored -> {
                    secondStarted.countDown();
                    releaseSecond.await();
                    return "late";
                }));
        AtomicReference<List<RunResult>> result = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        Thread caller = Thread.startVirtualThread(() -> {
            result.set(run(models, 1, Duration.ofSeconds(5)).runs());
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });

        try {
            assertTrue(firstReturned.await(1, TimeUnit.SECONDS));
            assertTrue(secondStarted.await(1, TimeUnit.SECONDS));
            caller.interrupt();
            assertTrue(caller.join(Duration.ofSeconds(1)));

            assertEquals(List.of(RunStatus.COMPLETED, RunStatus.CANCELLED),
                    result.get().stream().map(RunResult::status).toList());
            assertTrue(interruptRestored.get());
        } finally {
            releaseSecond.countDown();
            caller.interrupt();
            caller.join(Duration.ofSeconds(1));
        }
    }

    @Test
    void independentMatricesDoNotShareAdmissionState() throws Exception {
        int bound = ExecutionSettings.defaults().maxConcurrentInvocations();
        CountDownLatch saturated = new CountDownLatch(bound);
        CountDownLatch release = new CountDownLatch(1);
        List<ModelUnderTest> blockers = new ArrayList<>();
        for (int index = 0; index < bound; index++) {
            blockers.add(model("blocker-" + index, ignored -> {
                saturated.countDown();
                release.await();
                return "same";
            }));
        }
        CompletableFuture<CompatibilityResult> blocked =
                runAsync(blockers, 1, Duration.ofSeconds(3));

        try {
            assertTrue(saturated.await(1, TimeUnit.SECONDS));
            List<RunResult> independent = run(
                    List.of(
                            model("independent-a", ignored -> "ok"),
                            model("independent-b", ignored -> "ok")),
                    1, Duration.ofSeconds(1)).runs();
            assertTrue(independent.stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        } finally {
            release.countDown();
            blocked.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void fatalErrorIsPropagatedUnchangedWhileASiblingIsStillBlocked() throws Exception {
        LinkageError fatal = new LinkageError("fatal");
        CountDownLatch siblingStarted = new CountDownLatch(1);
        CountDownLatch releaseSibling = new CountDownLatch(1);
        List<ModelUnderTest> models = List.of(
                model("fatal", ignored -> {
                    assertTrue(siblingStarted.await(1, TimeUnit.SECONDS));
                    throw fatal;
                }),
                model("sibling", ignored -> {
                    siblingStarted.countDown();
                    releaseSibling.await();
                    return "late";
                }));
        CompletableFuture<Throwable> observedFailure = new CompletableFuture<>();
        Thread execution = Thread.startVirtualThread(() -> {
            try {
                run(models, 1, Duration.ofSeconds(5));
                observedFailure.complete(new AssertionError("fatal error was not propagated"));
            } catch (Throwable failure) {
                observedFailure.complete(failure);
            }
        });

        try {
            assertSame(fatal, observedFailure.get(1, TimeUnit.SECONDS));
        } finally {
            releaseSibling.countDown();
            execution.interrupt();
            assertTrue(execution.join(Duration.ofSeconds(1)));
        }
    }

    private static CompatibilityResult run(
            List<ModelUnderTest> models, int repetitions, Duration timeout) {
        ModelMatrix matrix = ModelMatrix.builder().models(models).repetitions(repetitions).timeout(timeout).build();
        return matrix.run(SCENARIO);
    }

    private static CompletableFuture<CompatibilityResult> runAsync(
            List<ModelUnderTest> models, int repetitions, Duration timeout) {
        return CompletableFuture.supplyAsync(
                () -> run(models, repetitions, timeout), Thread::startVirtualThread);
    }

    private static ModelUnderTest model(String id, ModelAdapter adapter) {
        return new ModelUnderTest(new ModelDescriptor(id), adapter);
    }

    private static void awaitIgnoringInterrupts(CountDownLatch release) {
        boolean interrupted = false;
        while (release.getCount() > 0) {
            try {
                release.await();
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
