package com.modelmatrix4j.tool;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.util.Objects;

/**
 * Core model identity paired with a tool-aware physical invocation adapter.
 *
 * @param descriptor stable model configuration identity
 * @param adapter adapter that returns normal output and tool-call evidence from one invocation
 */
public record ToolModel(ModelDescriptor descriptor, ToolAdapter adapter) {
    public ToolModel {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(adapter, "adapter");
    }
}
