package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.tool.ToolArgumentValidator;
import com.modelmatrix4j.tool.ToolCallComparator;
import com.modelmatrix4j.tool.ToolCallComparison;
import com.modelmatrix4j.tool.ToolCallObservation;
import com.modelmatrix4j.tool.ToolExecution;
import com.modelmatrix4j.tool.ToolInvocation;
import com.modelmatrix4j.tool.ToolModel;
import com.modelmatrix4j.tool.ToolObservation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolExternalConsumerTest {
    @Test
    void executesDocumentedToolHappyPath() {
        ToolModel baseline = model("baseline", "{\"city\":\"Berlin\",\"units\":\"c\"}");
        ToolModel candidate = model("candidate", "{\"units\":\"c\",\"city\":\"Berlin\"}");
        ToolExecution.PreparedModels prepared = ToolExecution.prepare(List.of(baseline, candidate));

        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("tool-consumer", "check weather"));
        List<ToolObservation> observations = prepared.observations(coreResult);

        ToolCallComparison comparison = new ToolCallComparator().compare(
                observations.get(0).calls(),
                observations.get(1).calls());

        assertTrue(coreResult.runs().stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        assertEquals(List.of("weather report", "weather report"), coreResult.runs().stream()
                .map(run -> run.output())
                .toList());
        assertEquals(2, observations.size());
        assertEquals(ToolCallComparison.Status.COMPATIBLE, comparison.status());
        assertTrue(new ToolArgumentValidator().isValid(observations.getFirst().calls().getFirst().arguments()));
    }

    private static ToolModel model(String configurationId, String arguments) {
        return new ToolModel(
                new ModelDescriptor(configurationId),
                scenario -> new ToolInvocation(
                        "weather report",
                        List.of(new ToolCallObservation("weather", arguments, "sunny"))));
    }
}
