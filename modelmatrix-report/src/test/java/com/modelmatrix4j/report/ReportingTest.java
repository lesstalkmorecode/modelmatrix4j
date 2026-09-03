package com.modelmatrix4j.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportingTest {
    private static final String SECRET = "secret-output-must-not-persist";

    @Test
    void projectionPreservesOnlyPersistenceSafeFactsInRunOrder() {
        CompatibilityReport report = new ReportProjector().project(result());

        assertEquals("1", report.schemaVersion());
        assertEquals(ReportCompatibilityStatus.MISMATCH, report.status());
        assertEquals(List.of("run-b", "run-a"), report.runs().stream().map(RunReport::runId).toList());
        assertEquals(12_000_000L, report.runs().getFirst().durationNanos());
    }

    @Test
    void reportContractRejectsUnsupportedSchemaVersions() {
        assertThrows(IllegalArgumentException.class, () -> new CompatibilityReport(
                "999", ReportCompatibilityStatus.COMPATIBLE,
                List.of(runReport("run", ReportRunStatus.COMPLETED))));
    }

    @Test
    void jsonIsByteStableAndOmitsOutputAndDiagnostics() {
        CompatibilityReport report = new ReportProjector().project(result());
        JsonReportWriter writer = new JsonReportWriter();

        String first = writer.write(report);
        String second = writer.write(report);

        assertEquals(first, second);
        assertEquals("{\"schemaVersion\":\"1\",\"status\":\"MISMATCH\",\"runs\":[{\"runId\":\"run-b\",\"scenarioId\":\"scenario\",\"configurationId\":\"beta\",\"repetition\":0,\"status\":\"COMPLETED\",\"durationNanos\":12000000},{\"runId\":\"run-a\",\"scenarioId\":\"scenario\",\"configurationId\":\"alpha\",\"repetition\":0,\"status\":\"FAILED\",\"durationNanos\":34000000}]}", first);
        assertFalse(first.contains(SECRET));
        assertFalse(first.contains("provider diagnostic"));
        assertFalse(first.contains("output"));
        assertFalse(first.contains("diagnostic"));
    }

    @Test
    void textSummaryUsesTheSamePersistenceSafeProjection() {
        String summary = new TextReportWriter().write(new ReportProjector().project(result()));

        assertTrue(summary.contains("ModelMatrix report v1 — MISMATCH"));
        assertTrue(summary.contains("configuration=beta"));
        assertFalse(summary.contains(SECRET));
        assertFalse(summary.contains("provider diagnostic"));
    }

    @Test
    void textSummaryEscapesIdentifiersThatCouldForgeCiLogLines() {
        CompatibilityReport report = new CompatibilityReport("1", ReportCompatibilityStatus.MISMATCH, List.of(
                new RunReport("run\r\nforged", "checkout\nFAILED | fake-entry\u2028continued\u2029end", "model\tname", 0,
                        ReportRunStatus.COMPLETED, 1)));

        String summary = new TextReportWriter().write(report);

        assertEquals(2, summary.lines().count());
        assertTrue(summary.contains("run\\r\\nforged"));
        assertTrue(summary.contains("scenario=checkout\\nFAILED | fake-entry\\u2028continued\\u2029end"));
        assertTrue(summary.contains("configuration=model\\tname"));
    }

    @Test
    void everyTerminalRunStatusMapsToStableReportVocabulary() {
        for (RunStatus status : RunStatus.values()) {
            String output = status == RunStatus.COMPLETED ? "safe-in-process-output" : "";
            RunResult run = new RunResult("run-" + status, "scenario", new ModelDescriptor("model"), 0,
                    status, output, Duration.ZERO, "diagnostic");
            CompatibilityReport report = new ReportProjector().project(
                    new CompatibilityResult(CompatibilityStatus.EXECUTION_FAILURE, List.of(run)));
            assertEquals(ReportRunStatus.valueOf(status.name()), report.runs().getFirst().status());
        }
    }

    @Test
    void everyMatrixStatusMapsToStableReportVocabulary() {
        for (CompatibilityStatus status : CompatibilityStatus.values()) {
            RunStatus runStatus = switch (status) {
                case COMPATIBLE, MISMATCH -> RunStatus.COMPLETED;
                case UNAVAILABLE -> RunStatus.UNAVAILABLE;
                case EXECUTION_FAILURE -> RunStatus.FAILED;
            };
            CompatibilityReport report = new ReportProjector().project(
                    new CompatibilityResult(status, List.of(runResult("run-" + status, runStatus))));
            assertEquals(ReportCompatibilityStatus.valueOf(status.name()), report.status());
            assertEquals(ReportRunStatus.valueOf(runStatus.name()), report.runs().getFirst().status());
        }
    }

    @Test
    void partialUnavailableMatrixPreservesEveryRunWithoutInventingCompleteness() {
        CompatibilityResult partial = new CompatibilityResult(CompatibilityStatus.UNAVAILABLE, List.of(
                runResult("completed", RunStatus.COMPLETED),
                runResult("unavailable", RunStatus.UNAVAILABLE)));

        CompatibilityReport report = new ReportProjector().project(partial);

        assertEquals(ReportCompatibilityStatus.UNAVAILABLE, report.status());
        assertEquals(List.of(ReportRunStatus.COMPLETED, ReportRunStatus.UNAVAILABLE),
                report.runs().stream().map(RunReport::status).toList());
    }

    private static CompatibilityResult result() {
        return new CompatibilityResult(CompatibilityStatus.MISMATCH, List.of(
                new RunResult("run-b", "scenario", new ModelDescriptor("beta"), 0,
                        RunStatus.COMPLETED, SECRET, Duration.ofMillis(12), ""),
                new RunResult("run-a", "scenario", new ModelDescriptor("alpha"), 0,
                        RunStatus.FAILED, "", Duration.ofMillis(34), "provider diagnostic " + SECRET)));
    }

    private static RunResult runResult(String runId, RunStatus status) {
        return new RunResult(runId, "scenario", new ModelDescriptor("model"), 0, status,
                status == RunStatus.COMPLETED ? "output" : "", Duration.ZERO, "diagnostic");
    }

    private static RunReport runReport(String runId, ReportRunStatus status) {
        return new RunReport(runId, "scenario", "model", 0, status, 0);
    }
}
