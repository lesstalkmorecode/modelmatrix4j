package com.modelmatrix4j.core.execution;

import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security boundary between internal outcomes and the safe public {@link RunResult}: enforces the
 * redaction policy and bounds diagnostics while mapping.
 */
final class RunResultMapper {
    static final int MAX_DIAGNOSTIC_LENGTH = 512;
    private static final String SENSITIVE_KEY = "authorization|api[-_ ]?key|token|password|secret";
    private static final Pattern JSON_SENSITIVE_VALUE = Pattern.compile(
            "(?i)(\\\"(?:" + SENSITIVE_KEY + ")\\\"\\s*:\\s*)\\\"[^\\\"]*\\\"");
    private static final Pattern ASSIGNED_SENSITIVE_VALUE = Pattern.compile(
            "(?i)(" + SENSITIVE_KEY + ")\\s*[:=]\\s*(?:Bearer\\s+)?"
                    + "(?:\\\"[^\\\"]*\\\"|'[^']*'|[^,;&?\\r\\n\\s]+)");

    List<RunResult> map(List<ExecutionOutcome> outcomes) {
        List<RunResult> results = new ArrayList<>(outcomes.size());
        for (ExecutionOutcome outcome : outcomes) {
            results.add(map(outcome));
        }
        return List.copyOf(results);
    }

    private RunResult map(ExecutionOutcome outcome) {
        return switch (outcome.state()) {
            case ExecutionOutcome.Completed success -> result(outcome, RunStatus.COMPLETED,
                    output(success), "");
            case ExecutionOutcome.Failed failure -> result(outcome, failure.status(),
                    "", safeDiagnostic(failure.diagnostic()));
        };
    }

    // The M2 public surface exposes normalized textual output only.
    private static String output(ExecutionOutcome.Completed success) {
        return redact(success.normalizedOutput());
    }

    private static RunResult result(
            ExecutionOutcome outcome, RunStatus status, String output, String diagnostic) {
        return new RunResult(outcome.runId(), outcome.scenarioId(), outcome.model(),
                outcome.repetition(), status, output, outcome.state().duration(), diagnostic);
    }

    private static String safeDiagnostic(String value) {
        String safe = redact(value);
        return safe.length() <= MAX_DIAGNOSTIC_LENGTH
                ? safe
                : safe.substring(0, MAX_DIAGNOSTIC_LENGTH);
    }

    private static String redact(String value) {
        String jsonSafe = JSON_SENSITIVE_VALUE.matcher(value).replaceAll("$1\\\"[REDACTED]\\\"");
        return ASSIGNED_SENSITIVE_VALUE.matcher(jsonSafe).replaceAll("$1=[REDACTED]");
    }
}
