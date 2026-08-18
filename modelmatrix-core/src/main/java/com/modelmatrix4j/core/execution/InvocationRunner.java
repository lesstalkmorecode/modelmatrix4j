package com.modelmatrix4j.core.execution;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.core.result.RunStatus;

/** Execution boundary owning lifecycle and classification of one physical adapter invocation. */
final class InvocationRunner {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final Semaphore physicalInvocationAdmission;

    InvocationRunner(int maxConcurrentInvocations) {
        if (maxConcurrentInvocations < 1) {
            throw new IllegalArgumentException("maxConcurrentInvocations must be positive");
        }
        this.physicalInvocationAdmission = new Semaphore(maxConcurrentInvocations);
    }

    ExecutionOutcome.State execute(Scenario scenario, ModelUnderTest target, Duration timeout) {
        long started = System.nanoTime();
        Invocation invocation = startInvocation(scenario, target);
        try {
            String output = invocation.task().get(timeoutNanos(timeout), TimeUnit.NANOSECONDS);
            if (output == null) {
                return new ExecutionOutcome.Failed(RunStatus.FAILED,
                        "Model adapter returned null output", elapsedSince(started));
            }
            return new ExecutionOutcome.Completed(normalize(output), elapsedSince(started));
        } catch (TimeoutException exception) {
            invocation.cancel();
            return new ExecutionOutcome.Failed(RunStatus.TIMED_OUT,
                    "Model execution exceeded timeout", elapsedSince(started));
        } catch (InterruptedException exception) {
            invocation.cancel();
            Thread.currentThread().interrupt();
            return new ExecutionOutcome.Failed(RunStatus.CANCELLED,
                    "Model invocation interrupted", elapsedSince(started));
        } catch (CancellationException exception) {
            return new ExecutionOutcome.Failed(RunStatus.CANCELLED,
                    "Model invocation cancelled", elapsedSince(started));
        } catch (ExecutionException exception) {
            return classifyFailure(exception.getCause(), elapsedSince(started));
        }
    }

    private Invocation startInvocation(Scenario scenario, ModelUnderTest target) {
        FutureTask<String> task = new FutureTask<>(() -> target.adapter().invoke(scenario));
        Thread thread = Thread.startVirtualThread(() -> runWhenAdmitted(task));
        return new Invocation(task, thread);
    }

    private void runWhenAdmitted(FutureTask<String> task) {
        boolean admitted = false;
        try {
            physicalInvocationAdmission.acquire();
            admitted = true;
            task.run();
        } catch (InterruptedException exception) {
            task.cancel(false);
            Thread.currentThread().interrupt();
        } finally {
            if (admitted) {
                physicalInvocationAdmission.release();
            }
        }
    }

    private static ExecutionOutcome.Failed classifyFailure(Throwable failure, Duration elapsed) {
        if (failure instanceof Error error) {
            throw error;
        }
        String diagnostic = diagnostic(failure);
        RunStatus status = switch (failure) {
            case ModelUnavailableException ignored -> RunStatus.UNAVAILABLE;
            case CancellationException ignored -> RunStatus.CANCELLED;
            case InterruptedException ignored -> RunStatus.CANCELLED;
            default -> RunStatus.FAILED;
        };
        return new ExecutionOutcome.Failed(status, diagnostic, elapsed);
    }

    private static String normalize(String text) {
        return WHITESPACE.matcher(text.strip()).replaceAll(" ");
    }

    private static Duration elapsedSince(long started) {
        return Duration.ofNanos(Math.max(0, System.nanoTime() - started));
    }

    private static long timeoutNanos(Duration timeout) {
        try {
            return timeout.toNanos();
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private static String diagnostic(Throwable failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record Invocation(FutureTask<String> task, Thread thread) {
        void cancel() {
            task.cancel(true);
            thread.interrupt();
        }
    }
}

