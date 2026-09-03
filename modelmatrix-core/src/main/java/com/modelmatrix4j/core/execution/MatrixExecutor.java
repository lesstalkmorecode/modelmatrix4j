package com.modelmatrix4j.core.execution;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.scenario.Scenario;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Application service that only orchestrates parallel model executions and returns outcomes. */
final class MatrixExecutor {
    List<ExecutionOutcome> execute(Scenario scenario, List<ModelUnderTest> models, ExecutionSettings settings) {
        InvocationRunner invocationRunner = new InvocationRunner(settings.maxConcurrentInvocations());
        List<ModelExecution> executions = new ArrayList<>(models.size());
        for (ModelUnderTest model : models) {
            executions.add(new ModelExecution(scenario, model, settings.repetitions(), settings.timeout(), invocationRunner));
        }

        if (Thread.currentThread().isInterrupted()) {
            return cancelledBeforeStart(executions);
        }

        ExecutorService modelTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            List<Future<List<ExecutionOutcome>>> submissions = new ArrayList<>(executions.size());
            for (ModelExecution execution : executions) {
                submissions.add(modelTaskExecutor.submit(execution::execute));
            }
            try {
                return collectInDeclarationOrder(submissions);
            } catch (InterruptedException exception) {
                modelTaskExecutor.shutdownNow();
                try {
                    return drainUninterruptibly(submissions);
                } finally {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            modelTaskExecutor.shutdownNow();
        }
    }

    private static List<ExecutionOutcome> collectInDeclarationOrder(
            List<Future<List<ExecutionOutcome>>> submissions) throws InterruptedException {
        List<ExecutionOutcome> outcomes = new ArrayList<>();
        for (Future<List<ExecutionOutcome>> submission : submissions) {
            try {
                outcomes.addAll(submission.get());
            } catch (ExecutionException exception) {
                rethrowModelFailure(exception.getCause());
            }
        }
        return List.copyOf(outcomes);
    }

    private static List<ExecutionOutcome> drainUninterruptibly(
            List<Future<List<ExecutionOutcome>>> submissions) {
        List<ExecutionOutcome> outcomes = new ArrayList<>();
        for (Future<List<ExecutionOutcome>> submission : submissions) {
            outcomes.addAll(awaitCompletion(submission));
        }
        return List.copyOf(outcomes);
    }

    private static List<ExecutionOutcome> awaitCompletion(
            Future<List<ExecutionOutcome>> submission) {
        while (true) {
            try {
                return submission.get();
            } catch (InterruptedException exception) {
                // Interruption is restored once all deterministic outcomes have been drained.
            } catch (ExecutionException exception) {
                rethrowModelFailure(exception.getCause());
            }
        }
    }

    private static void rethrowModelFailure(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException("Unexpected checked failure from model task", failure);
    }

    private static List<ExecutionOutcome> cancelledBeforeStart(List<ModelExecution> executions) {
        List<ExecutionOutcome> outcomes = new ArrayList<>();
        for (ModelExecution execution : executions) {
            outcomes.addAll(execution.cancelledBeforeStart());
        }
        return List.copyOf(outcomes);
    }
}

