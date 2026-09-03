package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelAdapter;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnavailableException;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.result.RunResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.junit.ModelMatrixSource;
import com.modelmatrix4j.junit.ModelMatrixTest;
import com.modelmatrix4j.mcp.McpAdapter;
import com.modelmatrix4j.mcp.McpComparator;
import com.modelmatrix4j.mcp.McpEvaluator;
import com.modelmatrix4j.mcp.McpExecution;
import com.modelmatrix4j.mcp.McpInvocation;
import com.modelmatrix4j.mcp.McpModel;
import com.modelmatrix4j.mcp.McpObservation;
import com.modelmatrix4j.mcp.McpResult;
import com.modelmatrix4j.mcp.McpToolInteraction;
import com.modelmatrix4j.rag.RetrievalAdapter;
import com.modelmatrix4j.rag.RetrievalComparator;
import com.modelmatrix4j.rag.RetrievalEvaluator;
import com.modelmatrix4j.rag.RetrievalExecution;
import com.modelmatrix4j.rag.RetrievalInvocation;
import com.modelmatrix4j.rag.RetrievalModel;
import com.modelmatrix4j.rag.RetrievalObservation;
import com.modelmatrix4j.rag.RetrievalResult;
import com.modelmatrix4j.rag.RetrievedDocument;
import com.modelmatrix4j.report.CompatibilityReport;
import com.modelmatrix4j.report.JsonReportWriter;
import com.modelmatrix4j.report.ReportCompatibilityStatus;
import com.modelmatrix4j.report.ReportProjector;
import com.modelmatrix4j.report.ReportRunStatus;
import com.modelmatrix4j.report.RunReport;
import com.modelmatrix4j.report.TextReportWriter;
import com.modelmatrix4j.springai.SpringAiMcpToolAdapter;
import com.modelmatrix4j.springai.SpringAiModelAdapter;
import com.modelmatrix4j.springai.SpringAiRetrievalAdapter;
import com.modelmatrix4j.springai.SpringAiToolCallAdapter;
import com.modelmatrix4j.structured.JsonObjectSchema;
import com.modelmatrix4j.structured.JsonValueComparator;
import com.modelmatrix4j.structured.StructuredOutputEvaluator;
import com.modelmatrix4j.structured.StructuredOutputExecution;
import com.modelmatrix4j.structured.StructuredOutputObservation;
import com.modelmatrix4j.structured.StructuredOutputResult;
import com.modelmatrix4j.tool.ToolAdapter;
import com.modelmatrix4j.tool.ToolArgumentValidator;
import com.modelmatrix4j.tool.ToolCallComparator;
import com.modelmatrix4j.tool.ToolCallComparison;
import com.modelmatrix4j.tool.ToolCallObservation;
import com.modelmatrix4j.tool.ToolExecution;
import com.modelmatrix4j.tool.ToolInvocation;
import com.modelmatrix4j.tool.ToolModel;
import com.modelmatrix4j.tool.ToolObservation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SupportedPublicApiCompilationTest {

    private static final Pattern SUPPORTED_FQCN = Pattern.compile("^- `(com\\.modelmatrix4j\\.[^`]+)`$");

    /**
     * docs/PUBLIC_API.md is the source of truth for this explicit external surface guard.
     * Any supported top-level type added or removed there must be updated here in the same change.
     */
    @Test
    void everyDocumentedSupportedTopLevelTypeIsAvailable() throws IOException {
        Class<?>[] supportedTypes = {
            // modelmatrix-core
            ModelMatrix.class,
            Scenario.class,
            ModelAdapter.class,
            ModelDescriptor.class,
            ModelUnderTest.class,
            ModelUnavailableException.class,
            CompatibilityResult.class,
            CompatibilityStatus.class,
            RunResult.class,
            RunStatus.class,

            // modelmatrix-junit
            ModelMatrixTest.class,
            ModelMatrixSource.class,

            // modelmatrix-structured
            JsonObjectSchema.class,
            JsonValueComparator.class,
            StructuredOutputEvaluator.class,
            StructuredOutputExecution.class,
            StructuredOutputObservation.class,
            StructuredOutputResult.class,

            // modelmatrix-tool
            ToolAdapter.class,
            ToolArgumentValidator.class,
            ToolCallComparator.class,
            ToolCallComparison.class,
            ToolCallObservation.class,
            ToolExecution.class,
            ToolInvocation.class,
            ToolModel.class,
            ToolObservation.class,

            // modelmatrix-rag
            RetrievalAdapter.class,
            RetrievalComparator.class,
            RetrievalEvaluator.class,
            RetrievalExecution.class,
            RetrievalInvocation.class,
            RetrievalModel.class,
            RetrievalObservation.class,
            RetrievalResult.class,
            RetrievedDocument.class,

            // modelmatrix-mcp
            McpAdapter.class,
            McpComparator.class,
            McpEvaluator.class,
            McpExecution.class,
            McpInvocation.class,
            McpModel.class,
            McpObservation.class,
            McpResult.class,
            McpToolInteraction.class,

            // modelmatrix-spring-ai
            SpringAiModelAdapter.class,
            SpringAiToolCallAdapter.class,
            SpringAiRetrievalAdapter.class,
            SpringAiMcpToolAdapter.class,

            // modelmatrix-report
            CompatibilityReport.class,
            JsonReportWriter.class,
            ReportCompatibilityStatus.class,
            ReportProjector.class,
            ReportRunStatus.class,
            RunReport.class,
            TextReportWriter.class
        };

        Set<String> guardedTypes = Arrays.stream(supportedTypes)
                .map(Class::getName)
                .collect(Collectors.toUnmodifiableSet());

        String publicApi = Files.readString(Path.of("..", "..", "docs", "PUBLIC_API.md"));
        String supportedSection = publicApi.substring(
                publicApi.indexOf("## Supported public types"),
                publicApi.indexOf("## What is not supported public API"));
        Set<String> documentedTypes = supportedSection.lines()
                .map(SUPPORTED_FQCN::matcher)
                .filter(matcher -> matcher.matches())
                .map(matcher -> matcher.group(1))
                .collect(Collectors.toUnmodifiableSet());

        assertEquals(documentedTypes, guardedTypes,
                "SupportedPublicApiCompilationTest must match docs/PUBLIC_API.md");
    }
}
