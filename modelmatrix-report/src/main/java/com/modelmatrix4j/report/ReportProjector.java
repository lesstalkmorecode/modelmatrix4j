package com.modelmatrix4j.report;

import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import java.util.Objects;

/**
 * Projects transient core results into the narrower durable report contract.
 *
 * <p>Projection preserves run ordering and maps core statuses explicitly to schema-local status
 * vocabularies. It deliberately does not copy model output, diagnostics, raw provider payloads, or
 * capability-local evidence.</p>
 */
public final class ReportProjector {
    public ReportProjector() {
    }

    public CompatibilityReport project(CompatibilityResult result) {
        Objects.requireNonNull(result, "result");
        return new CompatibilityReport(
                CompatibilityReport.CURRENT_SCHEMA_VERSION,
                map(result.status()),
                result.runs().stream().map(ReportProjector::projectRun).toList());
    }

    private static RunReport projectRun(RunResult run) {
        return new RunReport(
                run.runId(),
                run.scenarioId(),
                run.model().configurationId(),
                run.repetition(),
                map(run.status()),
                run.duration().toNanos());
    }

    private static ReportCompatibilityStatus map(CompatibilityStatus status) {
        return switch (status) {
            case COMPATIBLE -> ReportCompatibilityStatus.COMPATIBLE;
            case MISMATCH -> ReportCompatibilityStatus.MISMATCH;
            case UNAVAILABLE -> ReportCompatibilityStatus.UNAVAILABLE;
            case EXECUTION_FAILURE -> ReportCompatibilityStatus.EXECUTION_FAILURE;
        };
    }

    private static ReportRunStatus map(RunStatus status) {
        return switch (status) {
            case COMPLETED -> ReportRunStatus.COMPLETED;
            case FAILED -> ReportRunStatus.FAILED;
            case UNAVAILABLE -> ReportRunStatus.UNAVAILABLE;
            case TIMED_OUT -> ReportRunStatus.TIMED_OUT;
            case CANCELLED -> ReportRunStatus.CANCELLED;
        };
    }
}
