package com.modelmatrix4j.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PublicContractValidationTest {

    @Test
    void runReportRejectsInvalidDurableRunFacts() {
        assertThrows(NullPointerException.class,
                () -> new RunReport(null, "scenario", "model", 0, ReportRunStatus.COMPLETED, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RunReport(" ", "scenario", "model", 0, ReportRunStatus.COMPLETED, 0));
        assertThrows(NullPointerException.class,
                () -> new RunReport("run", null, "model", 0, ReportRunStatus.COMPLETED, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RunReport("run", " ", "model", 0, ReportRunStatus.COMPLETED, 0));
        assertThrows(NullPointerException.class,
                () -> new RunReport("run", "scenario", null, 0, ReportRunStatus.COMPLETED, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RunReport("run", "scenario", " ", 0, ReportRunStatus.COMPLETED, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RunReport("run", "scenario", "model", -1, ReportRunStatus.COMPLETED, 0));
        assertThrows(NullPointerException.class,
                () -> new RunReport("run", "scenario", "model", 0, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RunReport("run", "scenario", "model", 0, ReportRunStatus.COMPLETED, -1));
    }

    @Test
    void compatibilityReportRequiresCurrentSchemaStatusAndRuns() {
        RunReport run = runReport();

        assertEquals("1", CompatibilityReport.CURRENT_SCHEMA_VERSION);
        assertThrows(NullPointerException.class,
                () -> new CompatibilityReport(null, ReportCompatibilityStatus.COMPATIBLE, List.of(run)));
        assertThrows(NullPointerException.class,
                () -> new CompatibilityReport(CompatibilityReport.CURRENT_SCHEMA_VERSION, null, List.of(run)));
        assertThrows(NullPointerException.class,
                () -> new CompatibilityReport(
                        CompatibilityReport.CURRENT_SCHEMA_VERSION, ReportCompatibilityStatus.COMPATIBLE, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompatibilityReport(
                        CompatibilityReport.CURRENT_SCHEMA_VERSION, ReportCompatibilityStatus.COMPATIBLE, List.of()));
    }

    @Test
    void reportEntryPointsRejectNullInputs() {
        assertThrows(NullPointerException.class, () -> new ReportProjector().project(null));
        assertThrows(NullPointerException.class, () -> new JsonReportWriter().write(null));
        assertThrows(NullPointerException.class, () -> new TextReportWriter().write(null));
    }

    private static RunReport runReport() {
        return new RunReport("run", "scenario", "model", 0, ReportRunStatus.COMPLETED, 0);
    }
}
