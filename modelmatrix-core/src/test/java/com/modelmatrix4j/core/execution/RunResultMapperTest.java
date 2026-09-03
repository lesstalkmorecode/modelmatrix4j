package com.modelmatrix4j.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunResultMapperTest {
    private final RunResultMapper mapper = new RunResultMapper();

    @Test
    void redactsSuccessfulOutputAtPublicBoundary() {
        RunResult result = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Completed("token=secret safe=yes", Duration.ofMillis(7))));
        assertEquals("token=[REDACTED] safe=yes", result.output());
        assertFalse(result.toString().contains("secret"));
    }

    @Test
    void redactsAndBoundsDiagnostics() {
        RunResult result = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Failed(RunStatus.FAILED, "****** " + "x".repeat(700), Duration.ofMillis(7))));
        assertTrue(result.diagnostic().length() <= RunResultMapper.MAX_DIAGNOSTIC_LENGTH);
        assertTrue(result.diagnostic().contains("******"));
        assertFalse(result.toString().contains("hunter2"));
    }

    @Test
    void redactsJsonBearerAndQuerySecretsAcrossPublicResults() {
        RunResult output = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Completed("{\"token\":\"json-secret\"} Authorization: ****** ?api_key=query-secret&value=kept", Duration.ofMillis(7))));
        RunResult diagnostic = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Failed(RunStatus.FAILED, "{\"password\":\"json-password\"}, Authorization: ******; token=assigned-secret&safe=yes", Duration.ofMillis(7))));

        assertEquals("{\"token\":\"[REDACTED]\"} Authorization=[REDACTED] ?api_key=[REDACTED]&value=kept", output.output());
        assertEquals("{\"password\":\"[REDACTED]\"}, Authorization=[REDACTED]; token=[REDACTED]&safe=yes", diagnostic.diagnostic());
        assertSecretsAbsent(output, "json-secret", "header-secret", "query-secret");
        assertSecretsAbsent(diagnostic, "json-password", "header-password", "assigned-secret");
    }

    @Test
    void redactsCompleteQuotedMultiwordAssignedValues() {
        RunResult output = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Completed("token=\"alpha beta\" safe", Duration.ofMillis(7))));
        RunResult diagnostic = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Failed(RunStatus.FAILED, "password='gamma delta' safe", Duration.ofMillis(7))));

        assertEquals("token=[REDACTED] safe", output.output());
        assertEquals("password=[REDACTED] safe", diagnostic.diagnostic());
        assertSecretsAbsent(output, "alpha", "beta");
        assertSecretsAbsent(diagnostic, "gamma", "delta");
    }

    @Test
    void boundsDiagnosticAtTheExactPolicyLimit() {
        RunResult result = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Failed(RunStatus.FAILED, "x".repeat(RunResultMapper.MAX_DIAGNOSTIC_LENGTH + 100), Duration.ofMillis(7))));

        assertEquals(RunResultMapper.MAX_DIAGNOSTIC_LENGTH, result.diagnostic().length());
        assertEquals("x".repeat(RunResultMapper.MAX_DIAGNOSTIC_LENGTH), result.diagnostic());
    }

    @Test
    void mapsEveryOutcomeToItsRunStatusAndEmitsOutputOnlyForSuccess() {
        List<RunResult> results = mapper.map(List.of(
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Completed("output", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.FAILED, "failed", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.UNAVAILABLE, "unavailable", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.TIMED_OUT, "timed out", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.CANCELLED, "cancelled", Duration.ofMillis(7)))));

        assertEquals(List.of(RunStatus.COMPLETED, RunStatus.FAILED, RunStatus.UNAVAILABLE,
                        RunStatus.TIMED_OUT, RunStatus.CANCELLED),
                results.stream().map(RunResult::status).toList());
        assertEquals(List.of("output", "", "", "", ""),
                results.stream().map(RunResult::output).toList());
        assertEquals(List.of("", "failed", "unavailable", "timed out", "cancelled"),
                results.stream().map(RunResult::diagnostic).toList());
        for (RunResult result : results) {
            assertEquals("run", result.runId());
            assertEquals("scenario", result.scenarioId());
            assertEquals("model", result.model().configurationId());
            assertEquals(0, result.repetition());
            assertEquals(Duration.ofMillis(7), result.duration());
        }
    }

    @Test
    void successfulOutputMapsToCompletedWithOutput() {
        RunResult result = map(new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                new ExecutionOutcome.Completed("value", Duration.ofMillis(7))));

        assertEquals(RunStatus.COMPLETED, result.status());
        assertEquals("value", result.output());
    }

    @Test
    void noSecretSurvivesInOutputDiagnosticOrToString() {
        String[] secrets = {"output-secret", "hunter2", "offline-secret",
                "timeout-secret", "cancel-secret"};
        List<RunResult> results = mapper.map(List.of(
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Completed("token=output-secret ok", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.FAILED, "****** boom", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.UNAVAILABLE, "api_key=offline-secret", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.TIMED_OUT, "secret=timeout-secret", Duration.ofMillis(7))),
                new ExecutionOutcome("run", "scenario", new ModelDescriptor("model"), 0,
                        new ExecutionOutcome.Failed(RunStatus.CANCELLED, "authorization=cancel-secret", Duration.ofMillis(7)))));

        for (RunResult result : results) {
            assertSecretsAbsent(result, secrets);
        }
    }

    private RunResult map(ExecutionOutcome executionOutcome) {
        return mapper.map(List.of(executionOutcome)).getFirst();
    }

    private static void assertSecretsAbsent(RunResult result, String... secrets) {
        for (String secret : secrets) {
            assertFalse(result.output().contains(secret));
            assertFalse(result.diagnostic().contains(secret));
            assertFalse(result.toString().contains(secret));
        }
    }
}
