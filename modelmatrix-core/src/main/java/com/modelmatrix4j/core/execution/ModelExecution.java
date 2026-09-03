package com.modelmatrix4j.core.execution;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Executes one model's repetition sequence sequentially. */
final class ModelExecution {
    private static final String PRIOR_TIMEOUT = "Model invocation not started after prior timeout";
    private static final String MATRIX_CANCELLATION =
            "Model invocation not started after matrix cancellation";

    private final Scenario scenario;
    private final ModelUnderTest target;
    private final int repetitions;
    private final Duration timeout;
    private final InvocationRunner invocationRunner;

    ModelExecution(Scenario scenario, ModelUnderTest target, int repetitions, Duration timeout,
            InvocationRunner invocationRunner) {
        this.scenario = scenario;
        this.target = target;
        this.repetitions = repetitions;
        this.timeout = timeout;
        this.invocationRunner = invocationRunner;
    }

    List<ExecutionOutcome> execute() {
        List<ExecutionOutcome> outcomes = new ArrayList<>(repetitions);
        boolean priorInvocationTimedOut = false;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            ExecutionOutcome.State state;
            if (priorInvocationTimedOut) {
                state = new ExecutionOutcome.Failed(RunStatus.CANCELLED, PRIOR_TIMEOUT, Duration.ZERO);
            } else if (Thread.currentThread().isInterrupted()) {
                state = new ExecutionOutcome.Failed(RunStatus.CANCELLED, MATRIX_CANCELLATION, Duration.ZERO);
            } else {
                state = invocationRunner.execute(scenario, target, timeout);
            }
            outcomes.add(outcome(repetition, state));
            priorInvocationTimedOut |= timedOut(state);
        }
        return List.copyOf(outcomes);
    }

    List<ExecutionOutcome> cancelledBeforeStart() {
        List<ExecutionOutcome> outcomes = new ArrayList<>(repetitions);
        for (int repetition = 0; repetition < repetitions; repetition++) {
            outcomes.add(outcome(repetition, new ExecutionOutcome.Failed(RunStatus.CANCELLED, MATRIX_CANCELLATION, Duration.ZERO)));
        }
        return List.copyOf(outcomes);
    }

    private ExecutionOutcome outcome(int repetition, ExecutionOutcome.State state) {
        String runId = runId(scenario.id(), target.descriptor().configurationId(), repetition);
        return new ExecutionOutcome(runId, scenario.id(), target.descriptor(), repetition, state);
    }

    private static boolean timedOut(ExecutionOutcome.State state) {
        return state instanceof ExecutionOutcome.Failed f && f.status() == RunStatus.TIMED_OUT;
    }

    private static String runId(String scenarioId, String configurationId, int repetition) {
        return scenarioId.length() + ":" + scenarioId + ":" + configurationId.length() + ":"
                + configurationId + ":" + repetition;
    }
}
