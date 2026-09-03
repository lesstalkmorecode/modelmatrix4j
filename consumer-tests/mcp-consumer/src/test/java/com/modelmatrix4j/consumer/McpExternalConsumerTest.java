package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.mcp.McpComparator;
import com.modelmatrix4j.mcp.McpEvaluator;
import com.modelmatrix4j.mcp.McpExecution;
import com.modelmatrix4j.mcp.McpInvocation;
import com.modelmatrix4j.mcp.McpModel;
import com.modelmatrix4j.mcp.McpObservation;
import com.modelmatrix4j.mcp.McpResult;
import com.modelmatrix4j.mcp.McpToolInteraction;
import java.util.List;
import org.junit.jupiter.api.Test;

class McpExternalConsumerTest {
    @Test
    void executesDocumentedMcpHappyPath() {
        McpModel baseline = model("baseline", "{\"city\":\"Berlin\",\"days\":1}");
        McpModel candidate = model("candidate", "{\"days\":1.0,\"city\":\"Berlin\"}");
        McpExecution.PreparedModels prepared = McpExecution.prepare(List.of(baseline, candidate));

        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("mcp-consumer", "use MCP weather tool"));
        List<McpObservation> observations = prepared.observations(coreResult);
        McpResult result = new McpEvaluator().evaluate(observations);

        assertTrue(coreResult.runs().stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        assertEquals(List.of("weather answer", "weather answer"), coreResult.runs().stream()
                .map(run -> run.output())
                .toList());
        assertEquals(McpResult.Status.COMPATIBLE,
                new McpComparator().compare(observations.get(0), observations.get(1)));
        assertEquals(McpResult.Status.COMPATIBLE, result.status());
    }

    private static McpModel model(String configurationId, String arguments) {
        return new McpModel(
                new ModelDescriptor(configurationId),
                scenario -> new McpInvocation(
                        "weather answer",
                        List.of(new McpToolInteraction("weather", arguments))));
    }
}
