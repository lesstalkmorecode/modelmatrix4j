package com.modelmatrix4j.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.junit.ModelMatrixSource;
import com.modelmatrix4j.junit.ModelMatrixTest;
import java.util.List;

class JUnitTransitiveDependencyTest implements ModelMatrixSource {

    @Override
    public Scenario scenario() {
        return new Scenario("junit-consumer", "hello");
    }

    @Override
    public List<ModelUnderTest> models() {
        return List.of(
                new ModelUnderTest(new ModelDescriptor("baseline"), ignored -> "hello"),
                new ModelUnderTest(new ModelDescriptor("candidate"), ignored -> "hello"));
    }

    @ModelMatrixTest
    void junitArtifactProvidesCorePublicTypesTransitively(CompatibilityResult result) {
        assertEquals(CompatibilityStatus.COMPATIBLE, result.status());
        assertEquals(2, result.runs().size());
    }
}
