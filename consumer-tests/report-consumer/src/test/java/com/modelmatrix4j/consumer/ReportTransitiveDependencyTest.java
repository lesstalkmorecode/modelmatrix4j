package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.report.CompatibilityReport;
import com.modelmatrix4j.report.JsonReportWriter;
import com.modelmatrix4j.report.ReportProjector;
import com.modelmatrix4j.report.TextReportWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReportTransitiveDependencyTest {

    @Test
    void reportArtifactProvidesCoreAndJacksonTransitively() throws IOException {
        ModelUnderTest baseline =
                new ModelUnderTest(new ModelDescriptor("baseline"), ignored -> "hello");
        ModelUnderTest candidate =
                new ModelUnderTest(new ModelDescriptor("candidate"), ignored -> "hello");

        CompatibilityResult result = ModelMatrix.builder()
                .models(baseline, candidate)
                .build()
                .run(new Scenario("report-consumer", "hello"));

        CompatibilityReport report = new ReportProjector().project(result);
        String json = new JsonReportWriter().write(report);
        String text = new TextReportWriter().write(report);

        assertFalse(json.isBlank());
        assertFalse(text.isBlank());

        Path target = Path.of("target");
        Files.createDirectories(target);
        Path jsonReport = target.resolve("modelmatrix-report.json");
        Path textReport = target.resolve("modelmatrix-report.txt");
        Files.writeString(jsonReport, json, StandardCharsets.UTF_8);
        Files.writeString(textReport, text, StandardCharsets.UTF_8);

        assertTrue(Files.size(jsonReport) > 0);
        assertTrue(Files.size(textReport) > 0);
    }
}
