package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InvocationRunnerTest {
    private static final Scenario SCENARIO = new Scenario("scenario", "input");

    @Test
    void normalizesSuccessfulOutputOnce() {
        ExecutionOutcome.Completed success = assertInstanceOf(ExecutionOutcome.Completed.class,
                execute(ignored -> " hello\n world "));
        assertEquals("hello world", success.normalizedOutput());
    }

    @Test
    void treatsNullOutputAsFailure() {
        ExecutionOutcome.Failed failure = assertInstanceOf(ExecutionOutcome.Failed.class,
                execute(ignored -> null));
        assertEquals(RunStatus.FAILED, failure.status());
        assertEquals("Model adapter returned null output", failure.diagnostic());
    }

    @Test
    void classifiesFailuresWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();
        ExecutionOutcome.Failed failed = assertInstanceOf(ExecutionOutcome.Failed.class,
                execute(ignored -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("boom");
                }));
        assertEquals(1, calls.get());
        assertEquals(RunStatus.FAILED, failed.status());
        assertEquals("IllegalStateException: boom", failed.diagnostic());

        ExecutionOutcome.Failed unavailable = assertInstanceOf(ExecutionOutcome.Failed.class,
                execute(ignored -> {
                    throw new ModelUnavailableException("offline");
                }));
        assertEquals(RunStatus.UNAVAILABLE, unavailable.status());

        ExecutionOutcome.Failed cancelled = assertInstanceOf(ExecutionOutcome.Failed.class,
                execute(ignored -> {
                    throw new CancellationException("cancelled");
                }));
        assertEquals(RunStatus.CANCELLED, cancelled.status());

        ExecutionOutcome.Failed interrupted = assertInstanceOf(ExecutionOutcome.Failed.class,
                execute(ignored -> {
                    throw new InterruptedException("interrupted");
                }));
        assertEquals(RunStatus.CANCELLED, interrupted.status());
    }

    @Test
    void timeoutIncludesAdmissionAndDoesNotInvokeAdapter() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        InvocationRunner runner = new InvocationRunner(1);
        CountDownLatch holderStarted = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);

        // Start a long-running invocation to occupy the single permit
        Thread holder = Thread.startVirtualThread(() -> {
            runner.execute(SCENARIO, target(ignored -> {
                holderStarted.countDown();
                try {
                    releaseHolder.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                return "held";
            }), Duration.ofSeconds(5));
        });

        try {
            assertTrue(holderStarted.await(1, TimeUnit.SECONDS));
            ExecutionOutcome.State outcome = runner.execute(
                    SCENARIO, target(ignored -> {
                        calls.incrementAndGet();
                        return "unexpected";
                    }), Duration.ofMillis(50));

            assertEquals(RunStatus.TIMED_OUT,
                    assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
            assertEquals(0, calls.get());
        } finally {
            releaseHolder.countDown();
            holder.join(Duration.ofSeconds(1).toMillis());
        }
    }

    @Test
    void timeoutInterruptsCooperativeAdapter() throws Exception {
        CountDownLatch interrupted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                    SCENARIO, target(ignored -> {
                        try {
                            release.await();
                        } catch (InterruptedException exception) {
                            interrupted.countDown();
                            throw exception;
                        }
                        return "late";
                    }), Duration.ofMillis(50));
            assertEquals(RunStatus.TIMED_OUT,
                    assertInstanceOf(ExecutionOutcome.Failed.class, outcome).status());
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }
    }

    @Test
    void interruptionWhileWaitingForAdmissionCancelsWithoutLeakingAPermit() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ExecutionOutcome.State> outcome = new AtomicReference<>();
        AtomicBoolean interruptRestored = new AtomicBoolean();

        InvocationRunner runner = new InvocationRunner(1);
        CountDownLatch holderStarted = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);

        // occupy the only permit
        Thread holder = Thread.startVirtualThread(() -> {
            runner.execute(SCENARIO, target(ignored -> {
                holderStarted.countDown();
                try {
                    releaseHolder.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                return "held";
            }), Duration.ofSeconds(5));
        });

        CountDownLatch invoking = new CountDownLatch(1);
        Thread caller = Thread.ofVirtual().start(() -> {
            invoking.countDown();
            outcome.set(runner.execute(SCENARIO, target(ignored -> {
                calls.incrementAndGet();
                return "unexpected";
            }), Duration.ofSeconds(10)));
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });

        try {
            assertTrue(holderStarted.await(1, TimeUnit.SECONDS));
            assertTrue(invoking.await(1, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(Duration.ofSeconds(2).toMillis());

            assertFalse(caller.isAlive());
            assertEquals(RunStatus.CANCELLED,
                    assertInstanceOf(ExecutionOutcome.Failed.class, outcome.get()).status());
            assertEquals(0, calls.get());
            assertTrue(interruptRestored.get());
        } finally {
            releaseHolder.countDown();
            holder.join(Duration.ofSeconds(1).toMillis());
        }
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
    void propagatesFatalErrorUnchanged() throws Exception {
        LinkageError fatal = new LinkageError("fatal");
        assertSame(fatal, assertThrows(LinkageError.class,
                () -> new InvocationRunner(1).execute(SCENARIO, target(ignored -> {
                    throw fatal;
                }), Duration.ofSeconds(1))));
    }

    @Test
    void acceptsTimeoutsWhoseNanosecondsOverflow() {
        ExecutionOutcome.State outcome = new InvocationRunner(1).execute(
                SCENARIO, target(ignored -> "ok"),
                Duration.ofSeconds(Long.MAX_VALUE));
        assertInstanceOf(ExecutionOutcome.Completed.class, outcome);
    }

    private static ExecutionOutcome.State execute(ModelAdapter adapter) {
        return new InvocationRunner(1)
                .execute(SCENARIO, target(adapter), Duration.ofSeconds(1));
    }

    private static ModelUnderTest target(ModelAdapter adapter) {
        return new ModelUnderTest(new ModelDescriptor("model"), adapter);
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
