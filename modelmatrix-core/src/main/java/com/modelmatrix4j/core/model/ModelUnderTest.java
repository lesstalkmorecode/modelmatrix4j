package com.modelmatrix4j.core.model;

import java.util.Objects;

/**
 * One model configuration and the adapter used to invoke it.
 *
 * @param descriptor stable configuration identity
 * @param adapter provider-neutral invocation adapter
 */
public record ModelUnderTest(ModelDescriptor descriptor, ModelAdapter adapter) {
    public ModelUnderTest {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(adapter, "adapter");
    }
}
