package com.modelmatrix4j.core.model;

import java.util.Objects;

/** Represents a model configuration under test. */
public record ModelUnderTest(ModelDescriptor descriptor, ModelAdapter adapter) {
    public ModelUnderTest {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(adapter, "adapter");
    }
}
