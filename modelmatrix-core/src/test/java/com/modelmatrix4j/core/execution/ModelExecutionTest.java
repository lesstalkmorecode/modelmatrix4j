package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class ModelExecutionTest {
    @Test
    void executesRepetitionsSequentiallyInOrder() {
        AtomicInteger call = new AtomicInteger();
        ModelExecution execution = execution(new ModelUnderTest(new ModelDescriptor("model"),
                ignored -> "call " + call.getAndIncrement()),
                3, Duration.ofSeconds(1));

        List<ExecutionOutcome> outcomes = execution.execute();

        assertEquals(3, call.get());
        assertEquals(List.of(0, 1, 2), outcomes.stream()
                .map(ExecutionOutcome::repetition).toList());
        assertEquals(List.of("call 0", "call 1", "call 2"), outcomes.stream()
                .map(outcome -> ((ExecutionOutcome.Completed) outcome.state()).normalizedOutput())
                .toList());
    }

    @Test
    void timeoutSuppressesLaterRepetitions() {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1);
        try {
            ModelExecution execution = execution(new ModelUnderTest(new ModelDescriptor("model"),
                    ignored -> {
                        calls.incrementAndGet();
                        release.await();
                        return "late";
                    }), 4, Duration.ofMillis(30));

            List<ExecutionOutcome> outcomes = execution.execute();

            assertEquals(1, calls.get());
            assertEquals(RunStatus.TIMED_OUT, kind(outcomes.get(0)));
            assertEquals(3, outcomes.stream().skip(1)
                    .filter(outcome -> kind(outcome) == RunStatus.CANCELLED)
                    .count());
        } finally {
            release.countDown();
        }
    }

    @Test
    void nonCooperativeTimeoutNeverReentersTheAdapterForLaterRepetitions() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        try {
            ModelExecution execution = execution(new ModelUnderTest(new ModelDescriptor("model"),
                    ignored -> {
                        calls.incrementAndGet();
                        try {
                            awaitIgnoringInterrupts(release);
                            return "late";
                        } finally {
                            exited.countDown();
                        }
                    }), 4, Duration.ofMillis(30));

            List<ExecutionOutcome> outcomes = execution.execute();

            assertEquals(1, calls.get());
            assertEquals(RunStatus.TIMED_OUT, kind(outcomes.get(0)));
            assertEquals(3, outcomes.stream().skip(1)
                    .filter(outcome -> kind(outcome) == RunStatus.CANCELLED)
                    .count());
        } finally {
            release.countDown();
            assertTrue(exited.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void interruptedThreadYieldsCancelledWithoutInvokingAdapter() {
        AtomicInteger calls = new AtomicInteger();
        ModelExecution execution = execution(new ModelUnderTest(new ModelDescriptor("model"),
                ignored -> {
                    calls.incrementAndGet();
                    return "unexpected";
                }), 3, Duration.ofSeconds(1));

        Thread.currentThread().interrupt();
        try {
            List<ExecutionOutcome> outcomes = execution.execute();

            assertEquals(0, calls.get());
            assertEquals(3, outcomes.size());
            assertTrue(outcomes.stream().allMatch(
                    outcome -> kind(outcome) == RunStatus.CANCELLED));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void runIdsAreDeterministicAndUniquePerRepetition() {
        ModelUnderTest target = new ModelUnderTest(new ModelDescriptor("model"),
                ignored -> "ok");

        List<ExecutionOutcome> first = execution(target, 2, Duration.ofSeconds(1)).execute();
        List<ExecutionOutcome> second = execution(target, 2, Duration.ofSeconds(1)).execute();

        assertEquals(List.of("8:scenario:5:model:0", "8:scenario:5:model:1"),
                first.stream().map(ExecutionOutcome::runId).toList());
        assertEquals(first.stream().map(ExecutionOutcome::runId).toList(),
                second.stream().map(ExecutionOutcome::runId).toList());
    }

    private static RunStatus kind(ExecutionOutcome outcome) {
        return assertInstanceOf(ExecutionOutcome.Failed.class, outcome.state()).status();
    }

    private static ModelExecution execution(
            ModelUnderTest target, int repetitions, Duration timeout) {
        return new ModelExecution(new Scenario("scenario", "input"), target, repetitions,
                timeout, new InvocationRunner(1));
    }

    private static void awaitIgnoringInterrupts(CountDownLatch release) {
        while (release.getCount() > 0) {
            try {
                release.await();
            } catch (InterruptedException ignored) {
                // Deliberately non-cooperative test adapter.
            }
        }
    }
}
