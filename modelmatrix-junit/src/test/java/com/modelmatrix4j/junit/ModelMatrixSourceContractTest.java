package com.modelmatrix4j.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.scenario.Scenario;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelMatrixSourceContractTest {

    @Test
    void exposesDocumentedExecutionDefaults() {
        ModelMatrixSource source = new ModelMatrixSource() {
            @Override
            public Scenario scenario() {
                return new Scenario("scenario", "input");
            }

            @Override
            public List<ModelUnderTest> models() {
                return List.of();
            }
        };

        assertEquals(1, source.repetitions());
        assertEquals(Duration.ofSeconds(30), source.timeout());
        assertEquals(8, source.maxConcurrentInvocations());
    }
}
