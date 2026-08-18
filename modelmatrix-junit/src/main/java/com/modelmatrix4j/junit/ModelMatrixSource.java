package com.modelmatrix4j.junit;

import java.time.Duration;
import java.util.List;

import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.scenario.Scenario;

public interface ModelMatrixSource {
    Scenario scenario();

    List<ModelUnderTest> models();

    default int repetitions() {
        return 1;
    }

    default Duration timeout() {
        return Duration.ofSeconds(30);
    }
}
