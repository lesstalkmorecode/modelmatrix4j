package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.RunStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.structured.JsonObjectSchema;
import com.modelmatrix4j.structured.StructuredOutputEvaluator;
import com.modelmatrix4j.structured.StructuredOutputExecution;
import com.modelmatrix4j.structured.StructuredOutputObservation;
import com.modelmatrix4j.structured.StructuredOutputResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredExternalConsumerTest {
    @Test
    void executesDocumentedStructuredOutputHappyPath() {
        List<ModelUnderTest> models = List.of(
                new ModelUnderTest(
                        new ModelDescriptor("baseline"),
                        ignored -> "{\"name\":\"Ada\",\"score\":1}"),
                new ModelUnderTest(
                        new ModelDescriptor("candidate"),
                        ignored -> "{\"score\":1.0,\"name\":\"Ada\"}"));

        StructuredOutputExecution.PreparedModels prepared = StructuredOutputExecution.prepare(models);
        CompatibilityResult coreResult = ModelMatrix.builder()
                .models(prepared.models())
                .build()
                .run(new Scenario("structured-consumer", "return structured data"));

        List<StructuredOutputObservation> observations = prepared.observations(coreResult);
        JsonObjectSchema schema = new JsonObjectSchema(Map.of(
                "name", JsonObjectSchema.ValueType.STRING,
                "score", JsonObjectSchema.ValueType.NUMBER));
        StructuredOutputResult result = new StructuredOutputEvaluator().evaluate(observations, schema);

        assertTrue(coreResult.runs().stream().allMatch(run -> run.status() == RunStatus.COMPLETED));
        assertTrue(coreResult.runs().stream()
                .allMatch(run -> "[structured output captured]".equals(run.output())));
        assertEquals(2, observations.size());
        assertEquals(List.of("baseline", "candidate"), observations.stream()
                .map(StructuredOutputObservation::configurationId)
                .toList());
        assertEquals(StructuredOutputResult.Status.COMPATIBLE, result.status());
        assertTrue(result.observations().stream().allMatch(StructuredOutputResult.Observation::valid));
    }
}
