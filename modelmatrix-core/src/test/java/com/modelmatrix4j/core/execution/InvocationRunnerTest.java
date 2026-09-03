package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InvocationRunnerTest {
    private static final Scenario SCENARIO = new Scenario("scenario", "hello");

    @Test
    void completedInvocationIsNormalized() {
        ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> "  hello\n  world  "), Duration.ofSeconds(1));

        ExecutionOutcome.Completed completed = assertInstanceOf(ExecutionOutcome.Completed.class, outcome);
        assertEquals("hello world", completed.normalizedOutput());
    }

    @Test
    void nullOutputIsFailed() {
        ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> null), Duration.ofSeconds(1));

        assertEquals(RunStatus.FAILED,
                assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
    }

    @Test
    void unavailableExceptionIsClassified() {
        ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> { throw new ModelUnavailableException("missing"); }),
                Duration.ofSeconds(1));

        assertEquals(RunStatus.UNAVAILABLE,
                assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
    }

    @Test
    void genericExceptionIsFailed() {
        ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> { throw new IllegalStateException("boom"); }),
                Duration.ofSeconds(1));

        assertEquals(RunStatus.FAILED,
                assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
    }

    @Test
    void timeoutCancelsCooperativeInvocation() throws Exception {
        CountDownLatch interrupted = new CountDownLatch(1);
        ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(10));
                        return "late";
                    } catch (InterruptedException exception) {
                        interrupted.countDown();
                        throw exception;
                    }
                }), Duration.ofMillis(50));

        assertEquals(RunStatus.TIMED_OUT,
                assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
    }

    @Test
    void callerInterruptionCancelsInvocationAndRestoresInterruptStatus() throws Exception {
        AtomicReference<ExecutionOutcome.State> outcome = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();
        CountDownLatch entered = new CountDownLatch(1);

        Thread caller = Thread.ofVirtual().start(() -> {
            outcome.set(new InvocationRunner(1).execute(
                    SCENARIO, target(ignored -> {
                        entered.countDown();
                        Thread.sleep(Duration.ofSeconds(10));
                        return "late";
                    }), Duration.ofSeconds(20)));
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });

        assertTrue(entered.await(1, TimeUnit.SECONDS));
        caller.interrupt();
        caller.join(Duration.ofSeconds(2).toMillis());

        assertFalse(caller.isAlive());
        assertEquals(RunStatus.CANCELLED,
                assertInstanceOf(ExecutionOutcome.Failed.class, outcome.get()).status());
        assertTrue(interruptRestored.get());
    }

    @Test
    void interruptionWhileWaitingForAdmissionCancelsWithoutLeakingAPermit() throws Exception {
        AtomicInteger blockedCalls = new AtomicInteger();
        AtomicReference<ExecutionOutcome.State> outcome = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();

        InvocationRunner runner = new InvocationRunner(1);
        CountDownLatch holderStarted = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        CountDownLatch callerStarted = new CountDownLatch(1);

        Thread holder = Thread.startVirtualThread(() -> runner.execute(
                SCENARIO,
                target(ignored -> {
                    holderStarted.countDown();
                    releaseHolder.await();
                    return "held";
                }),
                Duration.ofSeconds(5)));

        try {
            assertTrue(holderStarted.await(1, TimeUnit.SECONDS));

            Thread caller = Thread.ofVirtual().start(() -> {
                callerStarted.countDown();
                outcome.set(runner.execute(
                        SCENARIO,
                        target(ignored -> {
                            blockedCalls.incrementAndGet();
                            return "unexpected";
                        }),
                        Duration.ofSeconds(10)));
                interruptRestored.set(Thread.currentThread().isInterrupted());
            });

            assertTrue(callerStarted.await(1, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(Duration.ofSeconds(2).toMillis());

            assertFalse(caller.isAlive());
            assertEquals(RunStatus.CANCELLED,
                    assertInstanceOf(ExecutionOutcome.Failed.class, outcome.get()).status());
            assertEquals(0, blockedCalls.get());
            assertTrue(interruptRestored.get());
        } finally {
            releaseHolder.countDown();
            holder.join(Duration.ofSeconds(1).toMillis());
        }

        ExecutionOutcome.State afterRelease = runner.execute(
                SCENARIO,
                target(ignored -> "after-release"),
                Duration.ofSeconds(1));
        assertEquals("after-release",
                assertInstanceOf(ExecutionOutcome.Completed.class, afterRelease).normalizedOutput());
    }

    @Test
    void permitIsReleasedOnlyWhenAdapterPhysicallyExits() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        try {
            ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                    SCENARIO, target(ignored -> {
                        try {
                            awaitIgnoringInterrupts(release);
                            return "late";
                        } finally {
                            exited.countDown();
                        }
                    }), Duration.ofMillis(50));
            assertEquals(RunStatus.TIMED_OUT,
                    assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
        } finally {
            release.countDown();
            assertTrue(exited.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void errorsEscapeRatherThanBeingNormalizedAsFailures() {
        assertThrows(AssertionError.class, () -> new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> { throw new AssertionError("boom"); }), Duration.ofSeconds(1)));
    }

    @Test
    void timeoutIncludesAdmissionWait() throws Exception {
        InvocationRunner runner = new InvocationRunner(1);
        CountDownLatch holderEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = Thread.startVirtualThread(() -> runner.execute(
                SCENARIO, target(ignored -> {
                    holderEntered.countDown();
                    awaitIgnoringInterrupts(release);
                    return "holder";
                }), Duration.ofSeconds(2)));

        try {
            assertTrue(holderEntered.await(1, TimeUnit.SECONDS));
            ExecutionOutcome.State outcome = runner.execute(
                    SCENARIO, target(ignored -> "should-not-run"), Duration.ofMillis(50));
            assertEquals(RunStatus.TIMED_OUT,
                    assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
        } finally {
            release.countDown();
            holder.join(Duration.ofSeconds(1).toMillis());
        }
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException exception) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static ModelUnderTest target(ModelAdapter adapter) {
        return new ModelUnderTest(new ModelDescriptor("model"), adapter);
    }
}
