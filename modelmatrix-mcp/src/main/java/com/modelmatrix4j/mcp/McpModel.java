package com.modelmatrix4j.mcp;

import com.modelmatrix4j.core.model.ModelDescriptor;
import java.util.Objects;

/** Core model identity paired with an MCP-aware physical invocation adapter. */
public record McpModel(ModelDescriptor descriptor, McpAdapter adapter) {
    public McpModel {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(adapter, "adapter");
    }
}
